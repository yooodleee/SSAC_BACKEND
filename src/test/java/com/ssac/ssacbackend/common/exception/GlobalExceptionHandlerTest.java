package com.ssac.ssacbackend.common.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GlobalExceptionHandler 예외 유형별 HTTP 상태 코드 및 ErrorCode 응답 검증.
 *
 * <p>MockMvc 독립 설정을 사용해 Spring Context 없이 빠르게 검증한다.
 */
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .standaloneSetup(new TestController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    // ── 400 Bad Request ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("400 Bad Request")
    class BadRequestTests {

        @Test
        @DisplayName("BadRequestException 발생 시 400과 올바른 ErrorCode를 응답한다")
        void badRequest_응답_400_INVALID_INPUT() throws Exception {
            mockMvc.perform(get("/test/bad-request"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.getCode()))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
        }

        @Test
        @DisplayName("BadRequestException — 커스텀 메시지도 응답에 포함된다")
        void badRequest_커스텀_메시지() throws Exception {
            mockMvc.perform(get("/test/bad-request-custom"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.PAGE_SIZE_EXCEEDED.getCode()))
                .andExpect(jsonPath("$.message").value("size는 최대 100까지 허용됩니다."));
        }
    }

    // ── 401 Unauthorized ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("401 Unauthorized")
    class UnauthorizedTests {

        @Test
        @DisplayName("UnauthorizedException 발생 시 401과 올바른 ErrorCode를 응답한다")
        void unauthorized_응답_401_TOKEN_INVALID() throws Exception {
            mockMvc.perform(get("/test/unauthorized"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value(ErrorCode.TOKEN_INVALID.getCode()))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
        }
    }

    // ── 403 Forbidden ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("403 Forbidden")
    class ForbiddenTests {

        @Test
        @DisplayName("ForbiddenException 발생 시 403과 올바른 ErrorCode를 응답한다")
        void forbidden_응답_403_ACCESS_DENIED() throws Exception {
            mockMvc.perform(get("/test/forbidden"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value(ErrorCode.ACCESS_DENIED.getCode()))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
        }
    }

    // ── 404 Not Found ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("404 Not Found")
    class NotFoundTests {

        @Test
        @DisplayName("NotFoundException 발생 시 404와 올바른 ErrorCode를 응답한다")
        void notFound_응답_404_NEWS_NOT_FOUND() throws Exception {
            mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value(ErrorCode.NEWS_NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorCode.NEWS_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
        }
    }

    // ── 409 Conflict ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("409 Conflict")
    class ConflictTests {

        @Test
        @DisplayName("ConflictException 발생 시 409와 올바른 ErrorCode를 응답한다")
        void conflict_응답_409_NICKNAME_DUPLICATED() throws Exception {
            mockMvc.perform(get("/test/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value(ErrorCode.NICKNAME_DUPLICATED.getCode()))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
        }
    }

    // ── 500 Internal Server Error ──────────────────────────────────────────────

    @Nested
    @DisplayName("500 Internal Server Error")
    class InternalServerErrorTests {

        @Test
        @DisplayName("예상치 못한 예외 발생 시 500을 응답하며 내부 정보를 포함하지 않는다")
        void unexpectedException_응답_500_내부정보_미포함() throws Exception {
            String responseBody = mockMvc.perform(get("/test/server-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.code").value(ErrorCode.INTERNAL_SERVER_ERROR.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorCode.INTERNAL_SERVER_ERROR.getMessage()))
                .andReturn()
                .getResponse()
                .getContentAsString();

            // NullPointerException 상세 정보가 응답에 포함되지 않아야 한다
            assertThat(responseBody).doesNotContain("NullPointerException");
            assertThat(responseBody).doesNotContain("TestController");
            assertThat(responseBody).doesNotContain("at com.ssac");
        }
    }

    // ── Bean Validation (@Valid) ───────────────────────────────────────────────

    @Nested
    @DisplayName("Bean Validation 실패")
    class ValidationTests {

        @Test
        @DisplayName("@Valid 검증 실패 시 400과 COMMON-001 코드 및 필드별 오류를 응답한다")
        void validation_실패_400_필드_오류_포함() throws Exception {
            // 빈 body → nickname 필드 검증 실패
            String requestBody = objectMapper.writeValueAsString(
                new TestController.NicknameRequest("")
            );

            mockMvc.perform(post("/test/validate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorCode.INVALID_INPUT.getMessage()))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0].field").value("nickname"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
        }

        @Test
        @DisplayName("@Valid 검증 성공 시 errors 필드가 응답에 포함되지 않는다")
        void validation_성공_시_errors_필드_없음() throws Exception {
            String requestBody = objectMapper.writeValueAsString(
                new TestController.NicknameRequest("valid")
            );

            String responseBody = mockMvc.perform(post("/test/validate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

            assertThat(responseBody).isEqualTo("ok");
        }
    }

    // ── 테스트용 컨트롤러 ────────────────────────────────────────────────────────

    @RestController
    @RequestMapping("/test")
    static class TestController {

        @GetMapping("/bad-request")
        void badRequest() {
            throw new BadRequestException(ErrorCode.INVALID_INPUT);
        }

        @GetMapping("/bad-request-custom")
        void badRequestCustom() {
            throw new BadRequestException(ErrorCode.PAGE_SIZE_EXCEEDED, "size는 최대 100까지 허용됩니다.");
        }

        @GetMapping("/unauthorized")
        void unauthorized() {
            throw new UnauthorizedException(ErrorCode.TOKEN_INVALID);
        }

        @GetMapping("/forbidden")
        void forbidden() {
            throw new ForbiddenException(ErrorCode.ACCESS_DENIED);
        }

        @GetMapping("/not-found")
        void notFound() {
            throw new NotFoundException(ErrorCode.NEWS_NOT_FOUND);
        }

        @GetMapping("/conflict")
        void conflict() {
            throw new ConflictException(ErrorCode.NICKNAME_DUPLICATED);
        }

        @GetMapping("/server-error")
        void serverError() {
            // 내부 상세 정보가 응답에 포함되지 않아야 하는 시나리오
            throw new NullPointerException("NullPointerException at TestController.java:42");
        }

        @PostMapping("/validate")
        String validate(@RequestBody @Valid NicknameRequest request) {
            return "ok";
        }

        record NicknameRequest(
            @NotBlank(message = "닉네임을 입력해주세요.")
            @Size(min = 2, max = 10, message = "닉네임은 2자 이상 10자 이하로 입력해주세요.")
            String nickname
        ) {
        }
    }
}
