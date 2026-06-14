package com.ssac.ssacbackend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ssac.ssacbackend.common.exception.ErrorCode;
import com.ssac.ssacbackend.common.exception.ServiceUnavailableException;
import com.ssac.ssacbackend.domain.auth.PendingRegistration;
import com.ssac.ssacbackend.domain.social.OAuthProvider;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 소셜 로그인 신규 회원의 임시 등록 상태를 관리한다.
 *
 * <p>tempToken은 UUID로 생성되며 {@link #TTL_SECONDS}(10분) 동안 유효하다.
 * Redis에 저장되어 Railway 재배포 시 인스턴스 재시작에 안전하다.
 * 회원 가입 완료({@link #invalidate}) 또는 TTL 만료 시 자동으로 제거된다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PendingRegistrationService {

    static final long TTL_SECONDS = 600L;
    static final String KEY_PREFIX = "auth:pending:";

    private static final ObjectMapper OBJECT_MAPPER =
        new ObjectMapper().registerModule(new JavaTimeModule());

    private final StringRedisTemplate redisTemplate;

    /**
     * 신규 소셜 회원에 대한 임시 등록 항목을 생성하고 tempToken을 반환한다.
     */
    public String create(OAuthProvider provider, String providerUserId, String email) {
        String tempToken = UUID.randomUUID().toString();
        PendingRegistration pending = new PendingRegistration(tempToken, provider, providerUserId, email);
        store(tempToken, pending, TTL_SECONDS);
        log.debug("PendingRegistration 생성: provider={}, tempToken={}", provider, tempToken);
        return tempToken;
    }

    /**
     * tempToken으로 유효한 임시 등록 항목을 조회한다. Redis TTL이 만료된 항목은 존재하지 않으므로 empty를 반환한다.
     */
    public Optional<PendingRegistration> findValid(String tempToken) {
        String value;
        try {
            value = redisTemplate.opsForValue().get(KEY_PREFIX + tempToken);
        } catch (Exception e) {
            log.error("Redis 장애: PendingRegistration 조회 실패: tempToken={}", tempToken, e);
            throw new ServiceUnavailableException(ErrorCode.REDIS_UNAVAILABLE);
        }
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(deserialize(value));
        } catch (JsonProcessingException e) {
            log.warn("PendingRegistration 역직렬화 실패: tempToken={}", tempToken, e);
            return Optional.empty();
        }
    }

    /**
     * 약관 동의 정보를 Redis에 반영한다.
     *
     * <p>{@code RegistrationService.saveTerms()} 에서 {@link PendingRegistration#completeTerms}
     * 호출 후 반드시 이 메서드를 호출해야 변경 사항이 Redis에 저장된다.
     *
     * @param tempToken 임시 토큰
     * @param pending   약관 동의가 완료된 임시 등록 객체
     */
    public void update(String tempToken, PendingRegistration pending) {
        Long remainingTtl;
        try {
            remainingTtl = redisTemplate.getExpire(KEY_PREFIX + tempToken, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Redis 장애: PendingRegistration TTL 조회 실패: tempToken={}", tempToken, e);
            throw new ServiceUnavailableException(ErrorCode.REDIS_UNAVAILABLE);
        }
        if (remainingTtl == null || remainingTtl <= 0) {
            log.warn("PendingRegistration 업데이트 실패 — 만료됨: tempToken={}", tempToken);
            return;
        }
        store(tempToken, pending, remainingTtl);
        log.debug("PendingRegistration 업데이트(약관 동의): tempToken={}", tempToken);
    }

    /**
     * 회원 가입 완료 후 tempToken을 즉시 무효화한다.
     */
    public void invalidate(String tempToken) {
        try {
            redisTemplate.delete(KEY_PREFIX + tempToken);
            log.debug("PendingRegistration 무효화: tempToken={}", tempToken);
        } catch (Exception e) {
            // 삭제 실패해도 TTL 만료로 자동 소멸되므로 경고 로그만 기록한다
            log.warn("Redis 장애: PendingRegistration 무효화 실패 (TTL 만료로 자동 소멸): tempToken={}", tempToken, e);
        }
    }

    private void store(String tempToken, PendingRegistration pending, long ttlSeconds) {
        String value;
        try {
            value = serialize(pending);
        } catch (JsonProcessingException e) {
            log.error("PendingRegistration 직렬화 실패: tempToken={}", tempToken, e);
            throw new IllegalStateException("PendingRegistration 저장 실패", e);
        }
        try {
            redisTemplate.opsForValue().set(KEY_PREFIX + tempToken, value, Duration.ofSeconds(ttlSeconds));
        } catch (Exception e) {
            log.error("Redis 장애: PendingRegistration 저장 실패: tempToken={}", tempToken, e);
            throw new ServiceUnavailableException(ErrorCode.REDIS_UNAVAILABLE);
        }
    }

    private String serialize(PendingRegistration pr) throws JsonProcessingException {
        return OBJECT_MAPPER.writeValueAsString(Map.of(
            "tempToken", pr.getTempToken(),
            "provider", pr.getProvider().name(),
            "providerUserId", pr.getProviderUserId(),
            "email", pr.getEmail(),
            "termsCompleted", String.valueOf(pr.isTermsCompleted()),
            "serviceTermAgreedAt", nullableDateTime(pr.getServiceTermAgreedAt()),
            "privacyTermAgreedAt", nullableDateTime(pr.getPrivacyTermAgreedAt()),
            "ageVerificationAgreedAt", nullableDateTime(pr.getAgeVerificationAgreedAt()),
            "marketingTermAgreedAt", nullableDateTime(pr.getMarketingTermAgreedAt())
        ));
    }

    private PendingRegistration deserialize(String json) throws JsonProcessingException {
        Map<String, String> map = OBJECT_MAPPER.readValue(json, new TypeReference<>() {});
        PendingRegistration pr = new PendingRegistration(
            map.get("tempToken"),
            OAuthProvider.valueOf(map.get("provider")),
            map.get("providerUserId"),
            map.get("email")
        );
        if (Boolean.parseBoolean(map.get("termsCompleted"))) {
            pr.completeTerms(
                parseDateTime(map.get("serviceTermAgreedAt")),
                parseDateTime(map.get("privacyTermAgreedAt")),
                parseDateTime(map.get("ageVerificationAgreedAt")),
                parseDateTime(map.get("marketingTermAgreedAt"))
            );
        }
        return pr;
    }

    private String nullableDateTime(LocalDateTime dt) {
        return dt != null ? dt.toString() : "";
    }

    private LocalDateTime parseDateTime(String value) {
        return (value == null || value.isBlank()) ? null : LocalDateTime.parse(value);
    }
}
