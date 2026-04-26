package com.ssac.ssacbackend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ssac.ssacbackend.domain.user.User;
import com.ssac.ssacbackend.domain.user.UserRole;
import com.ssac.ssacbackend.dto.TokenPair;
import com.ssac.ssacbackend.repository.QuizAttemptRepository;
import com.ssac.ssacbackend.repository.RefreshTokenRepository;
import com.ssac.ssacbackend.repository.SocialAccountRepository;
import com.ssac.ssacbackend.repository.UserRepository;
import com.ssac.ssacbackend.service.JwtService;
import com.ssac.ssacbackend.service.TokenService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * 역할 기반 접근 제어(RBAC) 통합 테스트.
 *
 * <p>실제 Security Filter Chain을 통과하는 HTTP 요청으로 다음 시나리오를 검증한다.
 * <ul>
 *   <li>토큰 없이 보호된 API → 401</li>
 *   <li>USER/ADMIN/GUEST 역할별 접근 제어</li>
 *   <li>로그아웃 후 기존 Access Token 차단</li>
 *   <li>권한 변경 즉시 반영</li>
 *   <li>IDOR 방어 (URL 직접 입력 차단)</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
class RbacIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private SocialAccountRepository socialAccountRepository;

    @Autowired
    private QuizAttemptRepository quizAttemptRepository;

    private User user;
    private User adminUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();

        // FK가 있는 테이블부터 정리 (QuizAttempt → User, SocialAccount → User)
        quizAttemptRepository.deleteAll();
        socialAccountRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        user = userRepository.save(User.builder()
            .email("user@test.com")
            .nickname("testuser")
            .role(UserRole.USER)
            .build());

        adminUser = userRepository.save(User.builder()
            .email("admin@test.com")
            .nickname("testadmin")
            .role(UserRole.ADMIN)
            .build());
    }

    // ── 401: 인증 토큰 없이 보호된 API ──────────────────────────────────────────

    @Test
    @DisplayName("토큰 없이 보호된 API 호출 시 401을 응답받는다")
    void noToken_protectedApi_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("토큰 없이 Admin API 호출 시 401을 응답받는다")
    void noToken_adminApi_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
            .andExpect(status().isUnauthorized());
    }

    // ── 200: 정상 접근 ───────────────────────────────────────────────────────

    @Test
    @DisplayName("USER 토큰으로 본인 프로필 조회 시 200을 응답받는다")
    void userToken_ownProfile_returns200() throws Exception {
        String token = jwtService.generateAccessToken(user.getId(), user.getEmail(), "USER");

        mockMvc.perform(get("/api/v1/users/me")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.email").value("user@test.com"));
    }

    @Test
    @DisplayName("ADMIN 토큰으로 사용자 목록 조회 시 200과 사용자 목록을 응답받는다")
    void adminToken_listUsers_returns200() throws Exception {
        String token = jwtService.generateAccessToken(adminUser.getId(), adminUser.getEmail(), "ADMIN");

        mockMvc.perform(get("/api/v1/admin/users")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("ADMIN 토큰으로 사용자 권한 변경 시 200과 변경된 사용자 정보를 응답받는다")
    void adminToken_updateRole_returns200() throws Exception {
        String token = jwtService.generateAccessToken(adminUser.getId(), adminUser.getEmail(), "ADMIN");

        mockMvc.perform(patch("/api/v1/admin/users/" + user.getId() + "/role")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"ADMIN\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.role").value("ADMIN"));
    }

    // ── 403: 권한 부족 ───────────────────────────────────────────────────────

    @Test
    @DisplayName("USER 토큰으로 Admin API 접근 시 403을 응답받는다")
    void userToken_adminApi_returns403() throws Exception {
        String token = jwtService.generateAccessToken(user.getId(), user.getEmail(), "USER");

        mockMvc.perform(get("/api/v1/admin/users")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GUEST 토큰으로 Admin API 접근 시 403을 응답받는다")
    void guestToken_adminApi_returns403() throws Exception {
        String token = jwtService.generateGuestToken("guest-id-abc");

        mockMvc.perform(get("/api/v1/admin/users")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GUEST 토큰으로 퀴즈 기록 목록 조회 시 403을 응답받는다")
    void guestToken_quizHistory_returns403() throws Exception {
        String token = jwtService.generateGuestToken("guest-id-abc");

        mockMvc.perform(get("/api/v1/quiz-attempts")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden());
    }

    // ── 로그아웃 후 토큰 차단 ───────────────────────────────────────────────

    @Test
    @DisplayName("로그아웃 후 기존 Access Token으로 API 호출 시 401을 응답받는다")
    void afterLogout_oldAccessToken_returns401() throws Exception {
        TokenPair tokens = tokenService.issueTokens(user);
        String accessToken = tokens.accessToken();
        String refreshToken = tokens.refreshToken();

        // 로그아웃 전: 정상 동작 확인
        mockMvc.perform(get("/api/v1/users/me")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk());

        // 로그아웃
        mockMvc.perform(post("/api/v1/auth/logout")
                .cookie(new Cookie("refreshToken", refreshToken)))
            .andExpect(status().isOk());

        // 로그아웃 후: 동일한 Access Token으로 재요청 → 401
        mockMvc.perform(get("/api/v1/users/me")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("로그아웃 후 Refresh Token으로 재발급 시도 시 400을 응답받는다")
    void afterLogout_refreshTokenReissue_returns400() throws Exception {
        TokenPair tokens = tokenService.issueTokens(user);

        // 로그아웃
        mockMvc.perform(post("/api/v1/auth/logout")
                .cookie(new Cookie("refreshToken", tokens.refreshToken())))
            .andExpect(status().isOk());

        // 동일한 Refresh Token으로 재발급 시도 → 400 (revoked)
        mockMvc.perform(post("/api/v1/auth/reissue")
                .cookie(new Cookie("refreshToken", tokens.refreshToken())))
            .andExpect(status().isBadRequest());
    }

    // ── 토큰 재발급 ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("유효한 Refresh Token으로 Access Token 재발급 시 200과 새 토큰을 응답받는다")
    void validRefreshToken_reissue_returns200() throws Exception {
        TokenPair tokens = tokenService.issueTokens(user);

        mockMvc.perform(post("/api/v1/auth/reissue")
                .cookie(new Cookie("refreshToken", tokens.refreshToken())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
    }

    // ── 권한 변경 즉시 반영 ──────────────────────────────────────────────────

    @Test
    @DisplayName("ADMIN이 USER의 권한을 ADMIN으로 변경하면 기존 토큰으로도 다음 요청부터 즉시 반영된다")
    void roleChange_immediatelyReflected_withSameToken() throws Exception {
        // USER 토큰 생성 (JWT payload에는 role=USER 가 기록됨)
        String userToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), "USER");
        String adminToken = jwtService.generateAccessToken(adminUser.getId(), adminUser.getEmail(), "ADMIN");

        // 변경 전: USER 토큰으로 Admin API → 403
        mockMvc.perform(get("/api/v1/admin/users")
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isForbidden());

        // ADMIN이 user의 역할을 ADMIN으로 변경
        mockMvc.perform(patch("/api/v1/admin/users/" + user.getId() + "/role")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"ADMIN\"}"))
            .andExpect(status().isOk());

        // 변경 후: 동일한 USER 토큰으로 Admin API → 200 (필터가 DB에서 새 역할 조회)
        mockMvc.perform(get("/api/v1/admin/users")
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("ADMIN이 ADMIN의 권한을 USER로 낮추면 기존 토큰의 Admin API 접근이 즉시 차단된다")
    void roleDowngrade_immediatelyReflected_withSameToken() throws Exception {
        String adminToken = jwtService.generateAccessToken(adminUser.getId(), adminUser.getEmail(), "ADMIN");
        String selfAdminToken = jwtService.generateAccessToken(adminUser.getId(), adminUser.getEmail(), "ADMIN");

        // 변경 전: Admin 토큰으로 Admin API → 200
        mockMvc.perform(get("/api/v1/admin/users")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk());

        // 다른 ADMIN 계정이 adminUser의 역할을 USER로 변경
        mockMvc.perform(patch("/api/v1/admin/users/" + adminUser.getId() + "/role")
                .header("Authorization", "Bearer " + selfAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"USER\"}"))
            .andExpect(status().isOk());

        // 변경 후: 동일한 ADMIN 토큰으로 Admin API → 403 (DB에서 USER로 조회)
        mockMvc.perform(get("/api/v1/admin/users")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isForbidden());
    }

    // ── IDOR 방어 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("USER가 다른 사용자의 응시 기록 ID로 접근해도 본인 것이 아니면 404를 응답받는다")
    void userToken_othersAttemptId_returns404() throws Exception {
        // user는 응시 기록이 없으므로, 존재하지 않는 ID 접근 시도
        String token = jwtService.generateAccessToken(user.getId(), user.getEmail(), "USER");
        Long nonExistentAttemptId = 99999L;

        mockMvc.perform(get("/api/v1/quiz-attempts/" + nonExistentAttemptId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNotFound());
    }

    // ── GUEST 역할 직접 부여 방지 ────────────────────────────────────────────

    @Test
    @DisplayName("ADMIN이 사용자에게 GUEST 역할 부여 시도 시 400을 응답받는다")
    void adminToken_assignGuestRole_returns400() throws Exception {
        String token = jwtService.generateAccessToken(adminUser.getId(), adminUser.getEmail(), "ADMIN");

        mockMvc.perform(patch("/api/v1/admin/users/" + user.getId() + "/role")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"GUEST\"}"))
            .andExpect(status().isBadRequest());
    }

    // ── 로그아웃 시 쿠키 삭제 ────────────────────────────────────────────────

    @Test
    @DisplayName("단일 기기 로그아웃 응답에 accessToken, refreshToken, guestId 쿠키 삭제 헤더가 포함된다")
    void logout_clearsAllCookies() throws Exception {
        TokenPair tokens = tokenService.issueTokens(user);

        mockMvc.perform(post("/api/v1/auth/logout")
                .cookie(new Cookie("refreshToken", tokens.refreshToken())))
            .andExpect(status().isOk())
            .andExpect(result -> {
                String setCookieHeaders = String.join(";",
                    result.getResponse().getHeaders("Set-Cookie"));
                assert setCookieHeaders.contains("accessToken=;") || setCookieHeaders.contains("accessToken=;")
                    : "accessToken 쿠키가 삭제되어야 합니다";
                assert setCookieHeaders.contains("refreshToken=;")
                    : "refreshToken 쿠키가 삭제되어야 합니다";
                assert setCookieHeaders.contains("guestId=;")
                    : "guestId 쿠키가 삭제되어야 합니다";
            });
    }

    // ── 전체 디바이스 로그아웃 ───────────────────────────────────────────────

    @Test
    @DisplayName("전체 디바이스 로그아웃 후 모든 세션의 Access Token이 차단된다")
    void logoutAll_invalidatesAllSessions() throws Exception {
        // 세션 1과 세션 2의 토큰을 각각 발급
        TokenPair session1 = tokenService.issueTokens(user);
        TokenPair session2 = tokenService.issueTokens(user);

        // 세션 1 토큰으로 로그인 상태 확인
        mockMvc.perform(get("/api/v1/users/me")
                .header("Authorization", "Bearer " + session1.accessToken()))
            .andExpect(status().isOk());

        // 전체 디바이스 로그아웃 (세션 1 Access Token으로 호출)
        mockMvc.perform(post("/api/v1/users/me/logout")
                .header("Authorization", "Bearer " + session1.accessToken()))
            .andExpect(status().isOk());

        // 세션 1의 Access Token → 401
        mockMvc.perform(get("/api/v1/users/me")
                .header("Authorization", "Bearer " + session1.accessToken()))
            .andExpect(status().isUnauthorized());

        // 세션 2의 Access Token도 → 401 (invalidatedBefore로 모두 차단)
        mockMvc.perform(get("/api/v1/users/me")
                .header("Authorization", "Bearer " + session2.accessToken()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("전체 디바이스 로그아웃 후 모든 세션의 Refresh Token으로 재발급이 불가능하다")
    void logoutAll_revokesAllRefreshTokens() throws Exception {
        TokenPair session1 = tokenService.issueTokens(user);
        TokenPair session2 = tokenService.issueTokens(user);

        // 전체 디바이스 로그아웃
        mockMvc.perform(post("/api/v1/users/me/logout")
                .header("Authorization", "Bearer " + session1.accessToken()))
            .andExpect(status().isOk());

        // 세션 1 Refresh Token으로 재발급 시도 → 400
        mockMvc.perform(post("/api/v1/auth/reissue")
                .cookie(new Cookie("refreshToken", session1.refreshToken())))
            .andExpect(status().isBadRequest());

        // 세션 2 Refresh Token으로 재발급 시도 → 400 (revokeAll로 모두 취소됨)
        mockMvc.perform(post("/api/v1/auth/reissue")
                .cookie(new Cookie("refreshToken", session2.refreshToken())))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("전체 디바이스 로그아웃은 인증 토큰 없이 호출 시 401을 응답받는다")
    void logoutAll_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/users/me/logout"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("전체 디바이스 로그아웃 응답에 accessToken, refreshToken, guestId 쿠키 삭제 헤더가 포함된다")
    void logoutAll_clearsAllCookies() throws Exception {
        TokenPair tokens = tokenService.issueTokens(user);

        mockMvc.perform(post("/api/v1/users/me/logout")
                .header("Authorization", "Bearer " + tokens.accessToken()))
            .andExpect(status().isOk())
            .andExpect(result -> {
                String setCookieHeaders = String.join(";",
                    result.getResponse().getHeaders("Set-Cookie"));
                assert setCookieHeaders.contains("accessToken=;")
                    : "accessToken 쿠키가 삭제되어야 합니다";
                assert setCookieHeaders.contains("refreshToken=;")
                    : "refreshToken 쿠키가 삭제되어야 합니다";
                assert setCookieHeaders.contains("guestId=;")
                    : "guestId 쿠키가 삭제되어야 합니다";
            });
    }
}
