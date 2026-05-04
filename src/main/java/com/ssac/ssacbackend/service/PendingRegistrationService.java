package com.ssac.ssacbackend.service;

import com.ssac.ssacbackend.domain.auth.PendingRegistration;
import com.ssac.ssacbackend.domain.social.OAuthProvider;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 소셜 로그인 신규 회원의 임시 등록 상태를 관리한다.
 *
 * <p>tempToken은 UUID로 생성되며 {@link #TTL_SECONDS}(10분) 동안 유효하다.
 * 회원 가입 완료({@link #invalidate}) 또는 TTL 만료 시 자동으로 제거된다.
 */
@Slf4j
@Service
public class PendingRegistrationService {

    static final long TTL_SECONDS = 600L;

    private final ConcurrentHashMap<String, PendingRegistration> store = new ConcurrentHashMap<>();

    /**
     * 신규 소셜 회원에 대한 임시 등록 항목을 생성하고 tempToken을 반환한다.
     */
    public String create(OAuthProvider provider, String providerUserId, String email) {
        purgeExpired();
        String tempToken = UUID.randomUUID().toString();
        store.put(tempToken, new PendingRegistration(tempToken, provider, providerUserId, email));
        log.debug("PendingRegistration 생성: provider={}, tempToken={}", provider, tempToken);
        return tempToken;
    }

    /**
     * tempToken으로 유효한 임시 등록 항목을 조회한다. 만료된 항목은 제거 후 empty를 반환한다.
     */
    public Optional<PendingRegistration> findValid(String tempToken) {
        PendingRegistration pending = store.get(tempToken);
        if (pending == null) {
            return Optional.empty();
        }
        if (isExpired(pending)) {
            store.remove(tempToken);
            log.debug("PendingRegistration 만료 제거: tempToken={}", tempToken);
            return Optional.empty();
        }
        return Optional.of(pending);
    }

    /**
     * 회원 가입 완료 후 tempToken을 즉시 무효화한다.
     */
    public void invalidate(String tempToken) {
        store.remove(tempToken);
        log.debug("PendingRegistration 무효화: tempToken={}", tempToken);
    }

    private boolean isExpired(PendingRegistration pending) {
        return Instant.now().isAfter(pending.getCreatedAt().plusSeconds(TTL_SECONDS));
    }

    private void purgeExpired() {
        store.entrySet().removeIf(entry -> isExpired(entry.getValue()));
    }
}
