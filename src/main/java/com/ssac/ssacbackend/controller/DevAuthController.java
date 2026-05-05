package com.ssac.ssacbackend.controller;

import com.ssac.ssacbackend.domain.social.OAuthProvider;
import com.ssac.ssacbackend.service.DevUserService;
import com.ssac.ssacbackend.service.PendingRegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 로컬/개발 환경 전용 인증 모의(Mock) 엔드포인트.
 *
 * <p>실제 카카오·네이버 OAuth 없이 회원 가입 플로우를 테스트할 수 있도록
 * tempToken을 직접 발급한다. {@code prod} 프로파일에서는 Bean 자체가 생성되지 않는다.
 *
 * <h3>사용법</h3>
 * <pre>
 * # 신규 회원 시나리오 (tempToken 발급 → FE /auth/kakao/callback 리다이렉트)
 * GET /api/auth/dev/mock-new-user?provider=KAKAO
 *
 * # 직접 tempToken JSON 응답 (Swagger / Postman 테스트용)
 * GET /api/auth/dev/mock-new-user?provider=NAVER&redirect=false
 *
 * # 테스트 계정 삭제 (이메일 기준, 관련 데이터 전체 삭제)
 * DELETE /api/auth/dev/users?email=test@example.com
 * </pre>
 */
@Slf4j
@RestController
@RequestMapping("/api/auth/dev")
@RequiredArgsConstructor
@Profile("!prod")
@Tag(name = "[DEV] 인증 모의", description = "로컬/개발 환경 전용 — prod 프로파일에서는 비활성화")
public class DevAuthController {

    private final PendingRegistrationService pendingRegistrationService;
    private final DevUserService devUserService;

    @Value("${oauth2.default-redirect-uri:http://localhost:3000}")
    private String defaultRedirectUri;

    /**
     * 소셜 로그인 없이 신규 회원 가입 플로우를 시작한다.
     *
     * <p>{@code redirect=true}(기본값)이면 실제 OAuth와 동일한 FE 콜백 URL로 리다이렉트하고,
     * {@code redirect=false}이면 JSON으로 tempToken을 반환한다.
     *
     * @param provider KAKAO 또는 NAVER
     * @param redirect false 지정 시 JSON 응답 반환 (기본값: true)
     */
    @GetMapping("/mock-new-user")
    @Operation(
        summary = "[DEV] 신규 회원 가입 플로우 모의 시작",
        description = "카카오·네이버 OAuth 없이 tempToken을 발급하고 신규 회원 가입 플로우를 시작한다. "
            + "redirect=true(기본)이면 FE 콜백 URL로 리다이렉트, "
            + "redirect=false이면 JSON으로 tempToken을 반환한다."
    )
    public Object mockNewUser(
        @RequestParam(defaultValue = "KAKAO") OAuthProvider provider,
        @RequestParam(defaultValue = "true") boolean redirect,
        HttpServletResponse response
    ) throws IOException {
        String fakeProviderUserId = "dev-mock-" + System.currentTimeMillis();
        String fakeEmail = "dev-mock@" + provider.name().toLowerCase() + ".local";

        String tempToken = pendingRegistrationService.create(provider, fakeProviderUserId, fakeEmail);
        log.info("[DEV] 신규 회원 모의 tempToken 발급: provider={}, tempToken={}", provider, tempToken);

        if (redirect) {
            String callbackPath = provider == OAuthProvider.KAKAO
                ? "/auth/kakao/callback" : "/auth/naver/callback";
            response.sendRedirect(defaultRedirectUri
                + callbackPath + "?isNewUser=true&tempToken=" + tempToken + "&provider=" + provider);
            return null;
        }

        return new MockNewUserResponse(true, tempToken, provider.name());
    }

    /**
     * 이메일로 테스트 사용자와 모든 연관 데이터를 삭제한다.
     *
     * <p>삭제 순서: SocialAccount → QuizAttempt(→AttemptAnswer cascade) →
     * Notification → ContentProgress → RefreshToken → MigrationFailure → User
     *
     * @param email 삭제할 사용자의 이메일
     */
    @DeleteMapping("/users")
    @Operation(
        summary = "[DEV] 테스트 사용자 삭제",
        description = "이메일 기준으로 사용자 및 모든 연관 데이터를 삭제한다. 테스트 초기화 목적으로 사용한다."
    )
    public ResponseEntity<Map<String, String>> deleteUser(@RequestParam String email) {
        devUserService.deleteByEmail(email);
        return ResponseEntity.ok(Map.of("message", "삭제 완료", "email", email));
    }

    record MockNewUserResponse(boolean isNewUser, String tempToken, String provider) {}
}
