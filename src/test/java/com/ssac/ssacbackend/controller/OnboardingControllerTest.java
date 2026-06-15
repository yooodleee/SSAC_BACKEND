package com.ssac.ssacbackend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.ssac.ssacbackend.common.exception.BusinessException;
import com.ssac.ssacbackend.domain.user.UserLevel;
import com.ssac.ssacbackend.domain.user.UserType;
import com.ssac.ssacbackend.dto.request.OnboardingInterestsRequest;
import com.ssac.ssacbackend.dto.request.OnboardingSubmitRequest;
import com.ssac.ssacbackend.dto.response.OnboardingQuestionsResponse;
import com.ssac.ssacbackend.dto.response.OnboardingResultResponse;
import com.ssac.ssacbackend.dto.response.OnboardingSkipResponse;
import com.ssac.ssacbackend.dto.response.OnboardingSubmitResponse;
import com.ssac.ssacbackend.service.OnboardingService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * OnboardingController 단위 테스트.
 *
 * <p>비로그인/로그인 분기 처리 및 userType 파라미터 검증 로직을 검증한다.
 * Spring Security 필터에 의한 401 처리는 RbacIntegrationTest에서 검증한다.
 */
class OnboardingControllerTest {

    private OnboardingService onboardingService;
    private OnboardingController controller;

    @BeforeEach
    void setUp() {
        onboardingService = mock(OnboardingService.class);
        controller = new OnboardingController(onboardingService);
    }

    // ── 비로그인 분기 ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("비로그인 + HIGH_SCHOOL userType으로 문제 조회 성공")
    void 비로그인_HIGH_SCHOOL_userType_문제_조회_성공() {
        OnboardingQuestionsResponse mockResponse = new OnboardingQuestionsResponse(
            UserType.HIGH_SCHOOL, 5, List.of());
        given(onboardingService.getQuestionsByUserType(UserType.HIGH_SCHOOL))
            .willReturn(mockResponse);

        ResponseEntity<?> response = controller.getQuestions(null, "HIGH_SCHOOL");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(onboardingService).getQuestionsByUserType(UserType.HIGH_SCHOOL);
    }

    @Test
    @DisplayName("비로그인 + EARLY_CAREER userType으로 문제 조회 성공")
    void 비로그인_EARLY_CAREER_userType_문제_조회_성공() {
        OnboardingQuestionsResponse mockResponse = new OnboardingQuestionsResponse(
            UserType.EARLY_CAREER, 5, List.of());
        given(onboardingService.getQuestionsByUserType(UserType.EARLY_CAREER))
            .willReturn(mockResponse);

        ResponseEntity<?> response = controller.getQuestions(null, "EARLY_CAREER");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(onboardingService).getQuestionsByUserType(UserType.EARLY_CAREER);
    }

    @Test
    @DisplayName("비로그인 + userType 파라미터 없이 요청 시 400 (ONBOARDING-001)")
    void 비로그인_userType_없이_요청_시_400() {
        assertThatThrownBy(() -> controller.getQuestions(null, null))
            .isInstanceOf(BusinessException.class)
            .satisfies(ex -> {
                BusinessException be = (BusinessException) ex;
                assertThat(be.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                assertThat(be.getCode()).isEqualTo("ONBOARDING-001");
            });
    }

    @Test
    @DisplayName("비로그인 + 유효하지 않은 userType 요청 시 400 (USER-TYPE-002)")
    void 비로그인_유효하지_않은_userType_요청_시_400() {
        assertThatThrownBy(() -> controller.getQuestions(null, "INVALID_TYPE"))
            .isInstanceOf(BusinessException.class)
            .satisfies(ex -> {
                BusinessException be = (BusinessException) ex;
                assertThat(be.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                assertThat(be.getCode()).isEqualTo("USER-TYPE-002");
            });
    }

    @Test
    @DisplayName("AnonymousAuthentication으로 요청 시 비로그인 분기 처리")
    void AnonymousAuthentication_비로그인_분기_처리() {
        AnonymousAuthenticationToken anonAuth = new AnonymousAuthenticationToken(
            "key", "anonymousUser",
            List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));

        assertThatThrownBy(() -> controller.getQuestions(anonAuth, null))
            .isInstanceOf(BusinessException.class)
            .satisfies(ex -> {
                BusinessException be = (BusinessException) ex;
                assertThat(be.getCode()).isEqualTo("ONBOARDING-001");
            });
    }

    // ── 로그인 분기 ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("로그인 사용자 요청 시 계정 userType 기반 응답")
    void 로그인_사용자_계정_userType_기반_응답() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
            "user@test.com", null,
            List.of(new SimpleGrantedAuthority("ROLE_USER")));
        OnboardingQuestionsResponse mockResponse = new OnboardingQuestionsResponse(
            UserType.HIGH_SCHOOL, 5, List.of());
        given(onboardingService.getQuestions("user@test.com")).willReturn(mockResponse);

        ResponseEntity<?> response = controller.getQuestions(auth, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(onboardingService).getQuestions("user@test.com");
    }

    @Test
    @DisplayName("로그인 사용자 요청 시 userType 파라미터는 무시됨")
    void 로그인_사용자_userType_파라미터_무시() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
            "user@test.com", null,
            List.of(new SimpleGrantedAuthority("ROLE_USER")));
        OnboardingQuestionsResponse mockResponse = new OnboardingQuestionsResponse(
            UserType.EARLY_CAREER, 5, List.of());
        given(onboardingService.getQuestions("user@test.com")).willReturn(mockResponse);

        ResponseEntity<?> response = controller.getQuestions(auth, "HIGH_SCHOOL");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(onboardingService).getQuestions("user@test.com");
    }

    // ── submit ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("온보딩 응답 제출 성공 시 200과 레벨 판정 결과를 반환한다")
    void 온보딩_응답_제출_성공() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
            "user@test.com", null,
            List.of(new SimpleGrantedAuthority("ROLE_USER")));
        OnboardingSubmitRequest request = new OnboardingSubmitRequest(List.of());
        OnboardingSubmitResponse mockResponse =
            new OnboardingSubmitResponse(UserLevel.SPROUT, 70, true);
        given(onboardingService.submit("user@test.com", request)).willReturn(mockResponse);

        ResponseEntity<?> response = controller.submit(auth, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(onboardingService).submit("user@test.com", request);
    }

    // ── skip ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("온보딩 건너뛰기 성공 시 200과 SEED 레벨을 반환한다")
    void 온보딩_건너뛰기_성공() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
            "user@test.com", null,
            List.of(new SimpleGrantedAuthority("ROLE_USER")));
        OnboardingSkipResponse mockResponse =
            new OnboardingSkipResponse(UserLevel.SEED, true, true);
        given(onboardingService.skip("user@test.com")).willReturn(mockResponse);

        ResponseEntity<?> response = controller.skip(auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(onboardingService).skip("user@test.com");
    }

    // ── getResult ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("온보딩 결과 조회 성공 시 200과 결과를 반환한다")
    void 온보딩_결과_조회_성공() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
            "user@test.com", null,
            List.of(new SimpleGrantedAuthority("ROLE_USER")));
        OnboardingResultResponse mockResponse =
            new OnboardingResultResponse(
                "HIGH_SCHOOL", "SPROUT", 70, 100, "새싹", "🌱", "성장 중", false, true, List.of());
        given(onboardingService.getResult("user@test.com")).willReturn(mockResponse);

        ResponseEntity<?> response = controller.getResult(auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(onboardingService).getResult("user@test.com");
    }

    // ── resetOnboarding ───────────────────────────────────────────────────────

    @Test
    @DisplayName("온보딩 초기화 성공 시 204를 반환한다")
    void 온보딩_초기화_성공() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
            "user@test.com", null,
            List.of(new SimpleGrantedAuthority("ROLE_USER")));

        ResponseEntity<Void> response = controller.resetOnboarding(auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(onboardingService).resetOnboarding("user@test.com");
    }

    // ── saveInterests ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("관심 도메인 저장 성공 시 204를 반환한다")
    void 관심_도메인_저장_성공() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
            "user@test.com", null,
            List.of(new SimpleGrantedAuthority("ROLE_USER")));
        OnboardingInterestsRequest request =
            new OnboardingInterestsRequest(List.of("finance", "it"));

        ResponseEntity<Void> response = controller.saveInterests(auth, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(onboardingService).saveInterests("user@test.com", request);
    }
}
