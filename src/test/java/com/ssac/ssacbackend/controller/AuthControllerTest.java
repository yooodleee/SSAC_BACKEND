package com.ssac.ssacbackend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.ssac.ssacbackend.domain.user.UserLevel;
import com.ssac.ssacbackend.domain.user.UserType;
import com.ssac.ssacbackend.dto.request.RegisterRequest;
import com.ssac.ssacbackend.dto.request.TermsRequest;
import com.ssac.ssacbackend.dto.response.NicknameCheckResponse;
import com.ssac.ssacbackend.dto.response.RegisterResponse;
import com.ssac.ssacbackend.service.RegistrationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private RegistrationService registrationService;

    @InjectMocks
    private AuthController controller;

    @Test
    @DisplayName("saveTerms - 약관 동의 저장 성공 시 204를 반환한다")
    void saveTerms_정상() {
        TermsRequest request = new TermsRequest(
            "temp-token-abc",
            new TermsRequest.Agreements(true, true, true, null)
        );

        ResponseEntity<Void> result = controller.saveTerms(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(registrationService).saveTerms(request);
    }

    @Test
    @DisplayName("register - 회원가입 성공 시 RegisterResponse를 포함한 200을 반환한다")
    void register_정상() {
        RegisterRequest request = new RegisterRequest(
            "temp-token-abc", "홍길동", UserType.HIGH_SCHOOL, null);
        RegisterResponse mockResponse = new RegisterResponse(
            "access-token", "refresh-token",
            new RegisterResponse.UserInfo(1L, "홍길동", UserType.HIGH_SCHOOL, UserLevel.SEED, false),
            new RegisterResponse.MergedInfo(0)
        );
        given(registrationService.register(request)).willReturn(mockResponse);

        ResponseEntity<RegisterResponse> result = controller.register(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(mockResponse);
    }

    @Test
    @DisplayName("checkNickname - 사용 가능한 닉네임이면 isAvailable: true를 반환한다")
    void checkNickname_사용가능() {
        given(registrationService.checkNickname("홍길동")).willReturn(new NicknameCheckResponse(true));

        ResponseEntity<NicknameCheckResponse> result = controller.checkNickname("홍길동");

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().isAvailable()).isTrue();
    }

    @Test
    @DisplayName("checkNickname - 중복 닉네임이면 isAvailable: false를 반환한다")
    void checkNickname_중복() {
        given(registrationService.checkNickname("중복이름")).willReturn(new NicknameCheckResponse(false));

        ResponseEntity<NicknameCheckResponse> result = controller.checkNickname("중복이름");

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().isAvailable()).isFalse();
    }
}
