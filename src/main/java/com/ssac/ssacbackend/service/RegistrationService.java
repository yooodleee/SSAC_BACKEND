package com.ssac.ssacbackend.service;

import com.ssac.ssacbackend.common.exception.BadRequestException;
import com.ssac.ssacbackend.common.exception.ConflictException;
import com.ssac.ssacbackend.common.exception.ErrorCode;
import com.ssac.ssacbackend.common.exception.UnauthorizedException;
import com.ssac.ssacbackend.config.JwtProperties;
import com.ssac.ssacbackend.domain.auth.PendingRegistration;
import com.ssac.ssacbackend.domain.social.OAuthProvider;
import com.ssac.ssacbackend.domain.social.SocialAccount;
import com.ssac.ssacbackend.domain.user.Gender;
import com.ssac.ssacbackend.domain.user.User;
import com.ssac.ssacbackend.dto.RegisterV2Result;
import com.ssac.ssacbackend.dto.TokenPair;

import com.ssac.ssacbackend.dto.request.EmailRegisterRequest;
import com.ssac.ssacbackend.dto.request.RegisterRequest;
import com.ssac.ssacbackend.dto.request.RegisterV2Request;
import com.ssac.ssacbackend.dto.request.TermsRequest;
import com.ssac.ssacbackend.dto.response.EmailCheckResponse;
import com.ssac.ssacbackend.dto.response.NicknameCheckResponse;
import com.ssac.ssacbackend.dto.response.RegisterResponse;
import com.ssac.ssacbackend.dto.response.RegisterV2Response;
import com.ssac.ssacbackend.repository.SocialAccountRepository;
import com.ssac.ssacbackend.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 소셜 로그인 기반 신규 회원 가입 비즈니스 로직.
 *
 * <p>약관 동의 저장 → 닉네임 설정 → 회원 가입 완료의 순서로 진행된다.
 * 카카오는 {@link User#provider}/{@link User#providerId} 필드를 사용하고,
 * 네이버는 {@link SocialAccount}를 통해 연결한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationService {

    /** 닉네임 허용 패턴: 한글·영문·숫자만, 2~10자 */
    private static final Pattern NICKNAME_PATTERN = Pattern.compile("^[가-힣a-zA-Z0-9]{2,10}$");

    /** 이메일 형식 패턴 */
    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final PendingRegistrationService pendingRegistrationService;
    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final TokenService tokenService;
    private final GuestMigrationService guestMigrationService;
    private final PasswordEncoder passwordEncoder;
    private final JwtProperties jwtProperties;

    /**
     * 약관 동의 정보를 임시 등록 항목에 저장한다.
     *
     * @throws UnauthorizedException tempToken이 만료되었거나 존재하지 않을 때 (TERMS-002)
     * @throws BadRequestException   필수 약관 미동의 시 (TERMS-001)
     */
    public void saveTerms(TermsRequest request) {
        PendingRegistration pending = pendingRegistrationService.findValid(request.tempToken())
            .orElseThrow(() -> new UnauthorizedException(ErrorCode.REGISTRATION_SESSION_EXPIRED));

        TermsRequest.Agreements agreements = request.agreements();
        if (!Boolean.TRUE.equals(agreements.serviceTerm())
            || !Boolean.TRUE.equals(agreements.privacyTerm())
            || !Boolean.TRUE.equals(agreements.ageVerification())) {
            throw new BadRequestException(ErrorCode.TERMS_REQUIRED);
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime marketingAgreedAt = Boolean.TRUE.equals(agreements.marketingTerm()) ? now : null;

        pending.completeTerms(now, now, now, marketingAgreedAt);
        pendingRegistrationService.update(request.tempToken(), pending);
        log.debug("약관 동의 저장 완료: tempToken={}", request.tempToken());
    }

    /**
     * 닉네임을 설정하고 회원 가입을 완료한다.
     *
     * <p>처리 순서:
     * <ol>
     *   <li>tempToken 유효성 검증</li>
     *   <li>약관 동의 완료 여부 확인</li>
     *   <li>닉네임 유효성 검증(형식·중복)</li>
     *   <li>소셜 계정 → 내부 User Entity 매핑</li>
     *   <li>약관 동의 정보 저장</li>
     *   <li>guestId 데이터 병합</li>
     *   <li>Access/Refresh Token 발급</li>
     *   <li>tempToken 무효화</li>
     * </ol>
     *
     * @throws UnauthorizedException tempToken 만료 (TERMS-002)
     * @throws BadRequestException   닉네임 형식 오류 (NICKNAME_INVALID), 약관 미동의 (TERMS-001),
     *                               userType 누락 (USER-TYPE-001)
     * @throws ConflictException     닉네임 중복 (NICKNAME_DUPLICATED)
     */
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        PendingRegistration pending = pendingRegistrationService.findValid(request.tempToken())
            .orElseThrow(() -> new UnauthorizedException(ErrorCode.REGISTRATION_SESSION_EXPIRED));

        if (!pending.isTermsCompleted()) {
            throw new BadRequestException(ErrorCode.TERMS_REQUIRED);
        }

        if (request.userType() == null) {
            throw new BadRequestException(ErrorCode.USER_TYPE_MISSING);
        }

        validateNickname(request.nickname());

        // 1~3: User + SocialAccount 생성
        User user = createUser(pending, request.nickname());

        // 4: 약관 동의 정보 저장
        user.agreeTerms(
            pending.getServiceTermAgreedAt(),
            pending.getPrivacyTermAgreedAt(),
            pending.getAgeVerificationAgreedAt(),
            pending.getMarketingTermAgreedAt()
        );

        // 5: 사용자 유형 저장
        user.setUserType(request.userType());

        // 6: guestId 데이터 병합
        int mergedQuizCount = 0;
        String guestId = request.guestId();
        if (guestId != null && !guestId.isBlank()) {
            GuestMigrationService.MigrationResult migrationResult =
                guestMigrationService.migrateGuestData(guestId, user);
            if (!migrationResult.success()) {
                log.warn("회원 가입 중 Guest 마이그레이션 실패, 가입 계속 진행: guestId={}", guestId);
            }
            mergedQuizCount = migrationResult.quizCount();
        }

        // 7: 토큰 발급
        TokenPair tokenPair = tokenService.issueTokens(user);
        log.info("회원 가입 완료: userId={}, provider={}, userType={}", user.getId(), pending.getProvider(), user.getUserType());

        // 8: tempToken 무효화
        pendingRegistrationService.invalidate(request.tempToken());

        return new RegisterResponse(
            tokenPair.accessToken(),
            tokenPair.refreshToken(),
            new RegisterResponse.UserInfo(
                user.getId(),
                user.getNickname(),
                user.getUserType(),
                user.getLevel(),
                user.getUserType() != null && user.getLevel() != null
            ),
            new RegisterResponse.MergedInfo(mergedQuizCount)
        );
    }

    /**
     * 신규 회원 가입 완료 (POST /api/v1/auth/register).
     *
     * <p>처리 순서:
     * <ol>
     *   <li>tempToken 유효성 검증</li>
     *   <li>입력 값 유효성 검증 (이름/생일/만14세/전화/성별/이메일/약관)</li>
     *   <li>이메일·전화 중복 확인</li>
     *   <li>소셜 계정 → 내부 User 엔티티 생성</li>
     *   <li>약관 동의 정보 저장</li>
     *   <li>회원 가입 정보(이름/생일/전화/성별) 저장</li>
     *   <li>guestId 데이터 병합</li>
     *   <li>Access/Refresh Token 발급</li>
     *   <li>tempToken 무효화</li>
     * </ol>
     *
     * @return 발급된 AccessToken 및 사용자 정보
     * @throws UnauthorizedException tempToken 만료 또는 미존재 (TERMS-002)
     * @throws BadRequestException   유효성 검증 실패
     * @throws ConflictException     이메일 중복 (EMAIL-002)
     */
    @Transactional
    public RegisterV2Result registerV2(RegisterV2Request request) {
        // 1. tempToken 검증
        PendingRegistration pending = pendingRegistrationService.findValid(request.tempToken())
            .orElseThrow(() -> new UnauthorizedException(ErrorCode.REGISTRATION_SESSION_EXPIRED));

        // 2. 입력 값 유효성 검증
        validateBirthDate(request.birthDate());
        Gender gender = parseGender(request.gender());

        // 3. 약관 검증
        RegisterV2Request.Agreements agreements = request.agreements();
        if (!Boolean.TRUE.equals(agreements.serviceTerm())
            || !Boolean.TRUE.equals(agreements.privacyTerm())
            || !Boolean.TRUE.equals(agreements.ageVerification())) {
            throw new BadRequestException(ErrorCode.TERMS_REQUIRED);
        }

        // 4. 이메일 중복 확인
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException(ErrorCode.EMAIL_DUPLICATED);
        }

        // 5. 전화번호 중복 확인
        String phoneRaw = request.phone().replace("-", "");
        if (userRepository.existsByPhone(phoneRaw)) {
            throw new ConflictException(ErrorCode.PHONE_DUPLICATED);
        }

        // 6. User + SocialAccount 생성
        String nickname = generateUniqueNickname(request.name());
        String dummyPassword = passwordEncoder.encode(UUID.randomUUID().toString());

        User user;
        if (pending.getProvider() == OAuthProvider.KAKAO) {
            user = User.builder()
                .email(request.email())
                .password(dummyPassword)
                .nickname(nickname)
                .provider("kakao")
                .providerId(pending.getProviderUserId())
                .build();
            user = userRepository.save(user);
        } else {
            user = User.builder()
                .email(request.email())
                .password(dummyPassword)
                .nickname(nickname)
                .build();
            user = userRepository.save(user);

            SocialAccount socialAccount = SocialAccount.builder()
                .provider(OAuthProvider.NAVER)
                .providerUserId(pending.getProviderUserId())
                .user(user)
                .build();
            socialAccountRepository.save(socialAccount);
        }

        // 7. 회원 가입 정보 저장
        LocalDate birthDate = LocalDate.parse(request.birthDate());
        user.completeSignup(request.name(), birthDate, phoneRaw, gender);

        // 8. 약관 동의 정보 저장
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime marketingAgreedAt = Boolean.TRUE.equals(agreements.marketingTerm()) ? now : null;
        user.agreeTerms(now, now, now, marketingAgreedAt);

        // 9. guestId 데이터 병합
        String guestId = request.guestId();
        if (guestId != null && !guestId.isBlank()) {
            GuestMigrationService.MigrationResult result =
                guestMigrationService.migrateGuestData(guestId, user);
            if (!result.success()) {
                log.warn("회원 가입 중 Guest 마이그레이션 실패, 가입 계속 진행: guestId={}", guestId);
            }
        }

        // 10. 토큰 발급
        TokenPair tokenPair = tokenService.issueTokens(user);
        log.info("신규 회원 가입 완료: userId={}, provider={}", user.getId(), pending.getProvider());

        // 11. tempToken 무효화
        pendingRegistrationService.invalidate(request.tempToken());

        RegisterV2Response response = new RegisterV2Response(
            tokenPair.accessToken(),
            "Bearer",
            jwtProperties.getExpirationMs(),
            new RegisterV2Response.UserInfo(
                String.valueOf(user.getId()),
                user.getDisplayNickname(),
                user.getName(),
                user.getEmail(),
                user.getUserType(),
                user.getLevel(),
                user.isOnboardingCompleted()
            )
        );
        return new RegisterV2Result(tokenPair.refreshToken(), response);
    }

    /**
     * 이메일+비밀번호 직접 회원가입 (POST /api/v1/auth/register/email).
     *
     * <p>소셜 인증 없이 이메일·비밀번호·개인 정보를 직접 입력하여 가입한다.
     * provider / providerId 는 null 로 저장된다.
     *
     * @throws BadRequestException   유효성 검증 실패 (비밀번호/생일/성별/약관)
     * @throws ConflictException     이메일 중복 (EMAIL-002)
     */
    @Transactional
    public RegisterV2Result registerWithEmail(EmailRegisterRequest request) {
        // 1. 입력 값 검증
        validateBirthDate(request.birthDate());
        validatePassword(request.password());
        Gender gender = parseGender(request.gender());

        // 2. 약관 검증
        EmailRegisterRequest.Agreements agreements = request.agreements();
        if (!Boolean.TRUE.equals(agreements.serviceTerm())
            || !Boolean.TRUE.equals(agreements.privacyTerm())
            || !Boolean.TRUE.equals(agreements.ageVerification())) {
            throw new BadRequestException(ErrorCode.TERMS_REQUIRED);
        }

        // 3. 이메일 중복 확인
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException(ErrorCode.EMAIL_DUPLICATED);
        }

        // 4. 닉네임 자동 생성 + User 저장
        String nickname = generateUniqueNickname(request.name());
        User user = User.builder()
            .email(request.email())
            .password(passwordEncoder.encode(request.password()))
            .nickname(nickname)
            .build();
        user = userRepository.save(user);

        // 5. 회원 가입 정보 저장
        String phoneRaw = request.phone().replace("-", "");
        if (userRepository.existsByPhone(phoneRaw)) {
            throw new ConflictException(ErrorCode.PHONE_DUPLICATED);
        }
        LocalDate birthDate = LocalDate.parse(request.birthDate());
        user.completeSignup(request.name(), birthDate, phoneRaw, gender);

        // 6. 약관 동의 저장
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime marketingAgreedAt = Boolean.TRUE.equals(agreements.marketingTerm()) ? now : null;
        user.agreeTerms(now, now, now, marketingAgreedAt);

        // 7. 게스트 데이터 병합
        String guestId = request.guestId();
        if (guestId != null && !guestId.isBlank()) {
            GuestMigrationService.MigrationResult result =
                guestMigrationService.migrateGuestData(guestId, user);
            if (!result.success()) {
                log.warn("이메일 회원가입 중 Guest 마이그레이션 실패, 가입 계속 진행: guestId={}", guestId);
            }
        }

        // 8. 토큰 발급
        TokenPair tokenPair = tokenService.issueTokens(user);
        log.info("이메일 회원가입 완료: userId={}", user.getId());

        RegisterV2Response response = new RegisterV2Response(
            tokenPair.accessToken(),
            "Bearer",
            jwtProperties.getExpirationMs(),
            new RegisterV2Response.UserInfo(
                String.valueOf(user.getId()),
                user.getDisplayNickname(),
                user.getName(),
                user.getEmail(),
                user.getUserType(),
                user.getLevel(),
                user.isOnboardingCompleted()
            )
        );
        return new RegisterV2Result(tokenPair.refreshToken(), response);
    }

    /**
     * 이메일 중복 여부를 확인한다. 형식이 유효하지 않으면 예외를 던진다.
     *
     * @throws BadRequestException 이메일 형식 오류 (EMAIL-001)
     */
    public EmailCheckResponse checkEmail(String email) {
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new BadRequestException(ErrorCode.EMAIL_FORMAT_INVALID);
        }
        boolean isDuplicated = userRepository.existsByEmail(email);
        return isDuplicated ? EmailCheckResponse.unavailable() : EmailCheckResponse.available();
    }

    /**
     * 닉네임 중복 여부를 확인한다. 형식이 유효하지 않으면 예외를 던진다.
     *
     * @throws BadRequestException 닉네임 형식 오류 (NICKNAME_INVALID)
     */
    public NicknameCheckResponse checkNickname(String nickname) {
        validateNicknameFormat(nickname);
        boolean isDuplicated = userRepository.existsByNickname(nickname);
        return isDuplicated ? NicknameCheckResponse.unavailable() : NicknameCheckResponse.available();
    }

    /**
     * 회원 가입 시 닉네임 형식과 중복을 모두 검증한다.
     *
     * @throws BadRequestException 닉네임 형식 오류 (NICKNAME_INVALID)
     * @throws ConflictException   닉네임 중복 (NICKNAME_DUPLICATED)
     */
    private void validateNickname(String nickname) {
        validateNicknameFormat(nickname);
        if (userRepository.existsByNickname(nickname)) {
            throw new ConflictException(ErrorCode.NICKNAME_DUPLICATED);
        }
    }

    private void validateNicknameFormat(String nickname) {
        if (!NICKNAME_PATTERN.matcher(nickname).matches()) {
            throw new BadRequestException(ErrorCode.NICKNAME_INVALID);
        }
    }

    /**
     * 생년월일 형식 및 만 14세 이상 여부를 검증한다.
     *
     * @throws BadRequestException 형식 오류 (BIRTH-001) 또는 만 14세 미만 (BIRTH-002)
     */
    private void validateBirthDate(String birthDateStr) {
        LocalDate birthDate;
        try {
            birthDate = LocalDate.parse(birthDateStr);
        } catch (DateTimeParseException e) {
            throw new BadRequestException(ErrorCode.BIRTH_DATE_FORMAT_INVALID);
        }
        Period age = Period.between(birthDate, LocalDate.now());
        if (age.getYears() < 14) {
            throw new BadRequestException(ErrorCode.BIRTH_DATE_UNDER_14);
        }
    }

    /**
     * 성별 문자열을 Gender enum으로 변환한다. null 또는 빈 문자열이면 null을 반환한다.
     *
     * @throws BadRequestException 유효하지 않은 성별 값 (GENDER-001)
     */
    private Gender parseGender(String genderStr) {
        if (genderStr == null || genderStr.isBlank()) {
            return null;
        }
        try {
            return Gender.valueOf(genderStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(ErrorCode.GENDER_INVALID);
        }
    }

    /**
     * 비밀번호 형식을 검증한다. 8~20자이며 영문과 숫자를 모두 포함해야 한다.
     *
     * @throws BadRequestException 형식 오류 (PASSWORD-001)
     */
    private void validatePassword(String password) {
        if (password == null || !password.matches("^(?=.*[a-zA-Z])(?=.*\\d).{8,20}$")) {
            throw new BadRequestException(ErrorCode.PASSWORD_FORMAT_INVALID);
        }
    }

    /**
     * 이름 기반으로 고유한 닉네임을 생성한다.
     * 형식: name(최대6자) + UUID 8자 (총 최대 14자, 20자 제한 이내)
     */
    private String generateUniqueNickname(String name) {
        String base = name.length() > 6 ? name.substring(0, 6) : name;
        for (int i = 0; i < 10; i++) {
            String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            String candidate = base + suffix;
            if (!userRepository.existsByNickname(candidate)) {
                return candidate;
            }
        }
        // 극히 드문 경우: 완전 랜덤 UUID 사용
        return UUID.randomUUID().toString().replace("-", "").substring(0, 14);
    }

    private User createUser(PendingRegistration pending, String nickname) {
        String dummyPassword = passwordEncoder.encode(java.util.UUID.randomUUID().toString());

        if (pending.getProvider() == OAuthProvider.KAKAO) {
            User user = User.builder()
                .email(pending.getEmail())
                .password(dummyPassword)
                .nickname(nickname)
                .provider("kakao")
                .providerId(pending.getProviderUserId())
                .build();
            return userRepository.save(user);
        } else {
            // NAVER: SocialAccount로 연결
            User user = User.builder()
                .email(pending.getEmail())
                .password(dummyPassword)
                .nickname(nickname)
                .build();
            userRepository.save(user);

            SocialAccount socialAccount = SocialAccount.builder()
                .provider(OAuthProvider.NAVER)
                .providerUserId(pending.getProviderUserId())
                .user(user)
                .build();
            socialAccountRepository.save(socialAccount);

            return user;
        }
    }
}
