package com.ssac.ssacbackend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ssac.ssacbackend.common.exception.ErrorCode;
import com.ssac.ssacbackend.common.exception.NotFoundException;
import com.ssac.ssacbackend.domain.onboarding.LevelInfo;
import com.ssac.ssacbackend.domain.user.User;
import com.ssac.ssacbackend.domain.user.UserLevel;
import com.ssac.ssacbackend.dto.response.HomeResponse;
import com.ssac.ssacbackend.dto.response.HomeResponse.HomeUserDto;
import com.ssac.ssacbackend.dto.response.HomeResponse.LastVisitDto;
import com.ssac.ssacbackend.dto.response.HomeResponse.RecommendedContentDto;
import com.ssac.ssacbackend.dto.response.HomeResponse.TodayQuizDto;
import com.ssac.ssacbackend.dto.response.HomeResponse.WelcomeBackDto;
import com.ssac.ssacbackend.dto.response.OnboardingRequiredResponse;
import com.ssac.ssacbackend.repository.UserRepository;
import com.ssac.ssacbackend.service.HomeContentAssembler.ContentSections;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 홈 화면 비즈니스 로직.
 *
 * <p>사용자 유형과 레벨에 맞는 추천 콘텐츠, 오늘의 카드, 이어보기, 오늘의 퀴즈를 제공한다.
 * 홈 데이터는 Redis에 사용자별로 캐싱되며, 당일 자정에 자동 만료된다.
 * 콘텐츠 완료·퀴즈 완료·레벨 변경·관심 도메인 변경 시 캐시가 즉시 무효화된다.
 *
 * <p>섹션별 조립 책임은 {@link HomeContentAssembler}와 {@link HomeQuizAssembler}에 위임한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HomeService {

    private static final String ONBOARDING_REDIRECT = "/onboarding/test";
    private static final String HOME_CACHE_PREFIX = HomeCacheEvictService.HOME_CACHE_PREFIX;
    private static final String REC_HISTORY_PREFIX = "home:rec_history:";
    private static final long REC_HISTORY_TTL_DAYS = 7L;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private static final int LONG_ABSENCE_DAYS = 7;

    private final UserRepository userRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final HomeContentAssembler homeContentAssembler;
    private final HomeQuizAssembler homeQuizAssembler;

    /**
     * 홈 화면 데이터를 반환한다.
     *
     * <p>온보딩 미완료 시 {@link OnboardingRequiredResponse}를 반환한다.
     * 캐시 히트 시 DB 조회 없이 즉시 응답한다.
     */
    @Transactional
    public Object getHome(String email) {
        User user = findUserByEmail(email);

        if (!user.isOnboardingCompleted()) {
            return new OnboardingRequiredResponse(true, ONBOARDING_REDIRECT);
        }

        String cacheKey = HOME_CACHE_PREFIX + user.getId();
        try {
            String cachedJson = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cachedJson != null) {
                log.debug("홈 캐시 히트: userId={}", user.getId());
                return OBJECT_MAPPER.readValue(cachedJson, HomeResponse.class);
            }
        } catch (Exception e) {
            log.warn("홈 캐시 읽기 실패 (Redis 불가용), DB에서 조회합니다: userId={}", user.getId());
        }

        HomeResponse response = buildHomeResponse(user, email);
        user.updateLastVisitedAt();
        updateRecHistory(user.getId(), response.recommendedContents());

        try {
            Duration ttl = computeTtlUntilMidnight();
            String json = OBJECT_MAPPER.writeValueAsString(response);
            stringRedisTemplate.opsForValue().set(cacheKey, json, ttl);
            log.debug("홈 캐시 저장: userId={}, ttl={}s", user.getId(), ttl.getSeconds());
        } catch (Exception e) {
            log.warn("홈 캐시 저장 실패 (Redis 불가용): userId={}", user.getId());
        }

        return response;
    }

    // ── 홈 데이터 조립 ──────────────────────────────────────────────────────────

    private HomeResponse buildHomeResponse(User user, String email) {
        Set<Long> recentlyRecommendedIds = getRecentlyRecommendedIds(user.getId());
        UserLevel level = user.getLevel() != null ? user.getLevel() : UserLevel.SEED;

        ContentSections contentSections =
            homeContentAssembler.build(user.getId(), email, level, recentlyRecommendedIds);
        TodayQuizDto todayQuiz = homeQuizAssembler.build(user.getId(), level, email);

        LevelInfo levelInfo = LevelInfo.from(level);
        String levelImageKey = resolveLevelImageKey(user.getLevel());
        HomeUserDto userDto = new HomeUserDto(
            user.getNickname(),
            user.getUserType() != null ? user.getUserType().name() : null,
            level.name(),
            levelInfo.getLabel(),
            levelInfo.getEmoji(),
            levelImageKey
        );

        LastVisitDto lastVisit = buildLastVisit(user.getLastVisitedAt());
        WelcomeBackDto welcomeBack = buildWelcomeBack(user.getLastVisitedAt());

        return new HomeResponse(
            false, userDto, contentSections.todayCard(), contentSections.recommended(),
            contentSections.continueLearning(), todayQuiz, contentSections.categories(),
            lastVisit, welcomeBack
        );
    }

    // ── 마지막 접속 / 복귀 메시지 ────────────────────────────────────────────────

    private LastVisitDto buildLastVisit(LocalDateTime lastVisitedAt) {
        if (lastVisitedAt == null) {
            return new LastVisitDto(null, 0);
        }
        int days = (int) ChronoUnit.DAYS.between(lastVisitedAt.toLocalDate(), LocalDate.now());
        return new LastVisitDto(lastVisitedAt, days);
    }

    private WelcomeBackDto buildWelcomeBack(LocalDateTime lastVisitedAt) {
        if (lastVisitedAt == null) {
            return null;
        }
        int days = (int) ChronoUnit.DAYS.between(lastVisitedAt.toLocalDate(), LocalDate.now());
        if (days < LONG_ABSENCE_DAYS) {
            return null;
        }
        return new WelcomeBackDto(true, days);
    }

    // ── 추천 이력 (Redis) ───────────────────────────────────────────────────────

    private Set<Long> getRecentlyRecommendedIds(Long userId) {
        try {
            String key = REC_HISTORY_PREFIX + userId;
            String raw = stringRedisTemplate.opsForValue().get(key);
            if (raw != null) {
                return OBJECT_MAPPER.readValue(raw, new TypeReference<Set<Long>>() {});
            }
        } catch (Exception e) {
            log.warn("추천 이력 읽기 실패 (Redis 불가용): userId={}", userId);
        }
        return new HashSet<>();
    }

    private void updateRecHistory(Long userId, List<RecommendedContentDto> contents) {
        if (contents == null || contents.isEmpty()) {
            return;
        }
        try {
            String key = REC_HISTORY_PREFIX + userId;
            Set<Long> ids = contents.stream()
                .filter(c -> !c.isPreview())
                .map(c -> Long.parseLong(c.id()))
                .collect(Collectors.toSet());

            Set<Long> existing = getRecentlyRecommendedIds(userId);
            existing.addAll(ids);
            String json = OBJECT_MAPPER.writeValueAsString(existing);
            stringRedisTemplate.opsForValue().set(key, json, Duration.ofDays(REC_HISTORY_TTL_DAYS));
        } catch (Exception e) {
            log.warn("추천 이력 저장 실패 (Redis 불가용): userId={}", userId);
        }
    }

    // ── 유틸리티 ────────────────────────────────────────────────────────────────

    private Duration computeTtlUntilMidnight() {
        LocalDateTime midnight = LocalDate.now().plusDays(1).atTime(LocalTime.MIDNIGHT);
        long seconds = ChronoUnit.SECONDS.between(LocalDateTime.now(), midnight);
        return Duration.ofSeconds(Math.max(seconds, 1L));
    }

    private String resolveLevelImageKey(UserLevel level) {
        if (level == null) {
            return "default";
        }
        return switch (level) {
            case SEED -> "seed";
            case SPROUT -> "sprout";
            case TREE -> "tree";
        };
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
    }
}
