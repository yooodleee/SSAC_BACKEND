package com.ssac.ssacbackend.service;

import com.ssac.ssacbackend.common.exception.BadRequestException;
import com.ssac.ssacbackend.common.exception.ConflictException;
import com.ssac.ssacbackend.common.exception.ErrorCode;
import com.ssac.ssacbackend.common.exception.UnauthorizedException;
import com.ssac.ssacbackend.domain.auth.PendingRegistration;
import com.ssac.ssacbackend.domain.social.OAuthProvider;
import com.ssac.ssacbackend.domain.social.SocialAccount;
import com.ssac.ssacbackend.domain.user.User;
import com.ssac.ssacbackend.dto.TokenPair;
import com.ssac.ssacbackend.dto.request.RegisterRequest;
import com.ssac.ssacbackend.dto.request.TermsRequest;
import com.ssac.ssacbackend.dto.response.NicknameCheckResponse;
import com.ssac.ssacbackend.dto.response.RegisterResponse;
import com.ssac.ssacbackend.repository.SocialAccountRepository;
import com.ssac.ssacbackend.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
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

    private final PendingRegistrationService pendingRegistrationService;
    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final TokenService tokenService;
    private final GuestMigrationService guestMigrationService;
    private final PasswordEncoder passwordEncoder;

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
     * @throws BadRequestException   닉네임 형식 오류 (NICKNAME_INVALID) 또는 약관 미동의 (TERMS-001)
     * @throws ConflictException     닉네임 중복 (NICKNAME_DUPLICATED)
     */
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        PendingRegistration pending = pendingRegistrationService.findValid(request.tempToken())
            .orElseThrow(() -> new UnauthorizedException(ErrorCode.REGISTRATION_SESSION_EXPIRED));

        if (!pending.isTermsCompleted()) {
            throw new BadRequestException(ErrorCode.TERMS_REQUIRED);
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

        // 5: guestId 데이터 병합
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

        // 6: 토큰 발급
        TokenPair tokenPair = tokenService.issueTokens(user);
        log.info("회원 가입 완료: userId={}, provider={}", user.getId(), pending.getProvider());

        // 7: tempToken 무효화
        pendingRegistrationService.invalidate(request.tempToken());

        return new RegisterResponse(
            tokenPair.accessToken(),
            tokenPair.refreshToken(),
            new RegisterResponse.UserInfo(
                user.getId(),
                user.getNickname(),
                pending.getProvider().name(),
                "beginner"
            ),
            new RegisterResponse.MergedInfo(mergedQuizCount)
        );
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
