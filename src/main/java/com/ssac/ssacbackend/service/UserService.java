package com.ssac.ssacbackend.service;

import com.ssac.ssacbackend.common.exception.BadRequestException;
import com.ssac.ssacbackend.common.exception.ConflictException;
import com.ssac.ssacbackend.common.exception.ErrorCode;
import com.ssac.ssacbackend.common.exception.NotFoundException;
import com.ssac.ssacbackend.domain.content.Content;
import com.ssac.ssacbackend.domain.content.ContentCategory;
import com.ssac.ssacbackend.domain.content.ContentViewHistory;
import com.ssac.ssacbackend.domain.onboarding.LevelInfo;
import com.ssac.ssacbackend.domain.onboarding.UserInterest;
import com.ssac.ssacbackend.domain.user.Gender;
import com.ssac.ssacbackend.domain.user.User;
import com.ssac.ssacbackend.domain.user.UserLevel;
import com.ssac.ssacbackend.domain.user.UserType;
import com.ssac.ssacbackend.dto.request.UpdateProfileRequest;
import com.ssac.ssacbackend.dto.response.MyPageResponse;
import com.ssac.ssacbackend.dto.response.UpdateProfileResponse;
import com.ssac.ssacbackend.dto.response.ViewedContentsResponse;
import com.ssac.ssacbackend.repository.ContentProgressRepository;
import com.ssac.ssacbackend.repository.ContentRepository;
import com.ssac.ssacbackend.repository.ContentViewHistoryRepository;
import com.ssac.ssacbackend.repository.QuizAttemptRepository;
import com.ssac.ssacbackend.repository.UserInterestRepository;
import com.ssac.ssacbackend.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 마이페이지 비즈니스 로직.
 *
 * <p>프로필 조회, 관심 도메인 수정, 사용자 유형 변경을 담당한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserInterestRepository userInterestRepository;
    private final ContentProgressRepository contentProgressRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final HomeCacheEvictService homeCacheEvictService;
    private final ContentViewHistoryRepository contentViewHistoryRepository;
    private final ContentRepository contentRepository;
    private final TokenService tokenService;

    /**
     * 마이페이지 프로필을 조회한다.
     *
     * @param email JWT에서 추출한 사용자 이메일
     * @return 프로필 응답 DTO
     */
    @Transactional(readOnly = true)
    public MyPageResponse getMyPage(String email) {
        User user = findUserByEmail(email);

        List<String> interests = userInterestRepository.findDomainIdsByUserId(user.getId());

        long totalContentsCompleted = contentProgressRepository
            .countByUserEmailAndProgressRateGreaterThanEqual(email, 100);

        List<Object[]> statsData = quizAttemptRepository.aggregateOverallStats(email);
        long totalQuizCompleted = 0;
        int correctRate = 0;
        if (!statsData.isEmpty() && statsData.get(0) != null && statsData.get(0)[0] != null) {
            Object[] row = statsData.get(0);
            long totalCorrect = row[2] != null ? ((Number) row[2]).longValue() : 0;
            long totalQuestions = row[3] != null ? ((Number) row[3]).longValue() : 0;
            totalQuizCompleted = row[1] != null ? ((Number) row[1]).longValue() : 0;
            correctRate = totalQuestions > 0
                ? (int) Math.round((double) totalCorrect / totalQuestions * 100) : 0;
        }

        int continuousLearningDays = calculateContinuousLearningDays(email);

        UserLevel level = user.getLevel();
        LevelInfo levelInfo = level != null ? LevelInfo.from(level) : null;

        MyPageResponse.StatsDto stats = new MyPageResponse.StatsDto(
            totalContentsCompleted, totalQuizCompleted, correctRate, continuousLearningDays);

        String phoneFormatted = formatPhone(user.getPhone());

        return new MyPageResponse(
            String.valueOf(user.getId()),
            user.getEmail(),
            user.getNickname(),
            user.getName(),
            user.getBirthDate(),
            phoneFormatted,
            user.getGender() != null ? user.getGender().name() : null,
            user.getUserType() != null ? user.getUserType().name() : null,
            userTypeLabel(user.getUserType()),
            level != null ? level.name() : null,
            levelInfo != null ? levelInfo.getLabel() : null,
            levelInfo != null ? levelInfo.getEmoji() : null,
            user.isOnboardingCompleted(),
            interests,
            stats,
            user.getProvider() != null ? user.getProvider().toUpperCase() : null,
            user.getCreatedAt()
        );
    }

    /**
     * 관심 도메인을 수정한다.
     *
     * @param email     사용자 이메일
     * @param domainIds 새 관심 도메인 ID 목록 (1~3개)
     */
    @Transactional
    public void updateInterests(String email, List<String> domainIds) {
        if (domainIds == null || domainIds.size() < 1 || domainIds.size() > 3) {
            throw new BadRequestException(ErrorCode.ONBOARDING_INTEREST_INVALID_COUNT);
        }
        User user = findUserByEmail(email);
        userInterestRepository.deleteByUserId(user.getId());

        List<UserInterest> interests = domainIds.stream()
            .map(domainId -> UserInterest.builder()
                .userId(user.getId())
                .domainId(domainId)
                .build())
            .toList();
        userInterestRepository.saveAll(interests);
        log.info("관심 도메인 수정 완료: email={}, domains={}", email, domainIds);
        homeCacheEvictService.evict(user.getId());
    }

    /**
     * 사용자 유형을 변경한다. 유형 변경 시 온보딩 결과가 초기화된다.
     *
     * @param email   사용자 이메일
     * @param newType 새 사용자 유형
     */
    @Transactional
    public void updateUserType(String email, UserType newType) {
        User user = findUserByEmail(email);
        user.setUserType(newType);

        if (user.isOnboardingCompleted()) {
            user.resetOnboarding();
            userInterestRepository.deleteByUserId(user.getId());
            log.info("사용자 유형 변경에 따른 온보딩 초기화: email={}, newType={}", email, newType);
        }
        homeCacheEvictService.evict(user.getId());
    }

    /**
     * 개인정보를 선택적으로 수정한다. null 필드는 변경하지 않는다.
     *
     * @param email   사용자 이메일
     * @param request 수정 요청 DTO
     * @return 수정된 개인정보 응답 DTO
     */
    @Transactional
    public UpdateProfileResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = findUserByEmail(email);

        if (request.name() != null) {
            if (request.name().isBlank()) {
                throw new BadRequestException(ErrorCode.NAME_REQUIRED);
            }
            if (request.name().length() > 20) {
                throw new BadRequestException(ErrorCode.NAME_EXCEEDS_MAX_LENGTH);
            }
        }

        LocalDate birthDate = null;
        if (request.birthDate() != null) {
            try {
                birthDate = LocalDate.parse(request.birthDate());
            } catch (DateTimeParseException e) {
                throw new BadRequestException(ErrorCode.BIRTH_DATE_FORMAT_INVALID);
            }
            if (Period.between(birthDate, LocalDate.now()).getYears() < 14) {
                throw new BadRequestException(ErrorCode.BIRTH_DATE_UNDER_14);
            }
        }

        if (request.phone() != null) {
            if (!request.phone().matches("^010-\\d{4}-\\d{4}$")) {
                throw new BadRequestException(ErrorCode.PHONE_FORMAT_INVALID);
            }
            String phoneRawCheck = request.phone().replace("-", "");
            if (!phoneRawCheck.equals(user.getPhone()) && userRepository.existsByPhone(phoneRawCheck)) {
                throw new ConflictException(ErrorCode.PHONE_DUPLICATED);
            }
        }

        if (request.email() != null) {
            if (!request.email().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                throw new BadRequestException(ErrorCode.EMAIL_FORMAT_INVALID);
            }
            if (!request.email().equals(user.getEmail()) && userRepository.existsByEmail(request.email())) {
                throw new ConflictException(ErrorCode.EMAIL_DUPLICATED);
            }
        }

        Gender gender = null;
        if (request.gender() != null) {
            try {
                gender = Gender.valueOf(request.gender().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException(ErrorCode.GENDER_INVALID);
            }
        }

        String phoneRaw = request.phone() != null ? request.phone().replace("-", "") : null;

        user.updateProfile(request.name(), birthDate, phoneRaw, gender, request.email());
        log.info("개인정보 수정 완료: email={}", email);

        return new UpdateProfileResponse(
            user.getName(),
            user.getBirthDate() != null ? user.getBirthDate().toString() : null,
            formatPhone(user.getPhone()),
            user.getGender() != null ? user.getGender().name() : null,
            user.getEmail()
        );
    }

    /**
     * 회원 탈퇴 처리. soft delete + 개인정보 익명화 + Refresh Token 전체 무효화.
     *
     * @param email 사용자 이메일
     */
    @Transactional
    public void withdraw(String email) {
        User user = findUserByEmail(email);
        tokenService.logoutAll(email);
        user.withdraw();
        log.info("회원 탈퇴 완료: userId={}", user.getId());
    }

    /**
     * 내가 본 콘텐츠 목록을 반환한다.
     *
     * @param email 사용자 이메일
     * @return 조회 이력 응답 DTO
     */
    @Transactional(readOnly = true)
    public ViewedContentsResponse getViewedContents(String email) {
        User user = findUserByEmail(email);
        List<ContentViewHistory> histories =
            contentViewHistoryRepository.findByUserIdOrderByViewedAtDesc(user.getId());

        List<ViewedContentsResponse.ViewedContentDto> contents = histories.stream()
            .map(h -> {
                Content content = contentRepository.findById(h.getContentId()).orElse(null);
                if (content == null) {
                    return null;
                }
                String category = content.getFirstCategory();
                String emoji = ContentCategory
                    .findById(category)
                    .map(ContentCategory::getEmoji).orElse("");
                return new ViewedContentsResponse.ViewedContentDto(
                    String.valueOf(content.getId()),
                    content.getTitle(),
                    category,
                    emoji,
                    content.getDifficulty() != null ? content.getDifficulty().name() : null,
                    h.getViewedAt(),
                    h.isCompleted()
                );
            })
            .filter(d -> d != null)
            .toList();

        return new ViewedContentsResponse(contents.size(), contents);
    }

    private static String formatPhone(String phone) {
        if (phone == null || phone.length() != 11) {
            return phone;
        }
        return phone.substring(0, 3) + "-" + phone.substring(3, 7) + "-" + phone.substring(7);
    }

    private int calculateContinuousLearningDays(String email) {
        Set<LocalDate> activityDates = new HashSet<>();

        contentProgressRepository.findActivityTimestampsByUserEmail(email)
            .stream()
            .map(LocalDateTime::toLocalDate)
            .forEach(activityDates::add);

        quizAttemptRepository.findActivityTimestampsByUserEmail(email)
            .stream()
            .map(LocalDateTime::toLocalDate)
            .forEach(activityDates::add);

        int count = 0;
        LocalDate date = LocalDate.now();
        while (activityDates.contains(date)) {
            count++;
            date = date.minusDays(1);
        }
        return count;
    }

    private static String userTypeLabel(UserType type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case HIGH_SCHOOL -> "수능이 끝난 고3";
            case EARLY_CAREER -> "사회초년생";
        };
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
    }
}
