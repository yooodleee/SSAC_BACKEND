package com.ssac.ssacbackend.service;

import com.ssac.ssacbackend.common.exception.ErrorCode;
import com.ssac.ssacbackend.common.exception.UnauthorizedException;
import com.ssac.ssacbackend.config.JwtProperties;
import com.ssac.ssacbackend.domain.auth.AdminCode;
import com.ssac.ssacbackend.domain.user.User;
import com.ssac.ssacbackend.dto.TokenPair;
import com.ssac.ssacbackend.dto.response.AdminLoginResponse;
import com.ssac.ssacbackend.repository.AdminCodeRepository;
import com.ssac.ssacbackend.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 코드 로그인 서비스.
 *
 * <p>코드는 SHA-256 해시로 조회한다. 1회 사용 후 재사용 불가.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminLoginService {

    private final AdminCodeRepository adminCodeRepository;
    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final JwtProperties jwtProperties;

    /**
     * 로그인 결과: 응답 DTO + Refresh Token (쿠키 설정용).
     */
    public record LoginResult(AdminLoginResponse response, String refreshToken) {}

    @Transactional
    public LoginResult login(String rawCode) {
        String codeHash = sha256(rawCode);

        AdminCode adminCode = adminCodeRepository.findByCodeHash(codeHash)
            .orElseThrow(() -> new UnauthorizedException(ErrorCode.ADMIN_CODE_INVALID));

        if (adminCode.isUsed()) {
            throw new UnauthorizedException(ErrorCode.ADMIN_CODE_INVALID);
        }

        if (adminCode.isExpired()) {
            throw new UnauthorizedException(ErrorCode.ADMIN_CODE_EXPIRED);
        }

        User admin = userRepository.findById(adminCode.getAdminUserId())
            .orElseThrow(() -> new UnauthorizedException(ErrorCode.ADMIN_CODE_INVALID));

        adminCode.markAsUsed();

        TokenPair tokens = tokenService.issueTokens(admin);

        log.info("관리자 로그인 성공: userId={}", admin.getId());

        AdminLoginResponse response = new AdminLoginResponse(
            tokens.accessToken(),
            "Bearer",
            jwtProperties.getExpirationMs(),
            new AdminLoginResponse.UserInfo(
                String.valueOf(admin.getId()),
                admin.getDisplayNickname(),
                admin.getRole().name(),
                "/admin"
            )
        );

        return new LoginResult(response, tokens.refreshToken());
    }

    /**
     * 관리자 코드를 SHA-256 해시로 변환한다.
     */
    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 찾을 수 없습니다.", e);
        }
    }
}
