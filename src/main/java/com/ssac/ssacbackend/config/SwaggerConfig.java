package com.ssac.ssacbackend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI(Swagger) 전역 설정.
 *
 * <p>이 설정이 생성하는 문서는 GET /api-docs/swagger.json 에서 제공된다.
 * 프론트엔드 에이전트는 이 URL을 단일 계약 원본으로 신뢰한다.
 *
 * <p>변경 기준: docs/decisions/004-swagger-contract.md
 *
 * <p>에이전트 작성 규칙 (docs/swagger-guide.md 참고):
 * - 모든 @Operation에 summary + description 필수
 * - description에 ① 호출 화면/상황 ② 권한 조건 ③ 특이 동작 포함
 * - @ApiResponse로 발생 가능한 모든 HTTP 코드 명시
 */
@Configuration
public class SwaggerConfig {

    /**
     * 전역 OpenAPI 메타데이터, 인증 스키마, 공통 에러 응답 스키마를 등록한다.
     */
    @Bean
    public OpenAPI ssacOpenAPI() {
        return new OpenAPI()
            .info(buildInfo())
            .components(buildComponents())
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }

    /**
     * 모든 Operation에 공통 에러 응답(401, 500)을 자동으로 추가하는 커스터마이저.
     *
     * <p>컨트롤러에서 @ApiResponse로 이미 정의된 코드는 덮어쓰지 않는다.
     */
    @Bean
    public OperationCustomizer globalErrorResponseCustomizer() {
        return (operation, handlerMethod) -> {
            ApiResponses responses = operation.getResponses();
            if (responses == null) {
                responses = new ApiResponses();
                operation.setResponses(responses);
            }
            responses.addApiResponse("401", unauthorizedResponse());
            responses.addApiResponse("500", internalServerErrorResponse());
            return operation;
        };
    }

    // ── 메타데이터 ──────────────────────────────────────────────────────────

    private Info buildInfo() {
        return new Info()
            .title("SSAC Backend API")
            .version("v1")
            .description(buildDescription())
            .contact(new Contact()
                .name("SSAC Backend Team")
                .email("ssac-backend@ssac.com"));
    }

    private String buildDescription() {
        return """
            SSAC 서비스 백엔드 REST API 명세서.

            ## 에이전트 사용 지침

            이 문서는 프론트엔드 에이전트가 소비하는 **단일 계약 원본**이다.

            ### 응답 구조
            모든 응답은 `ApiResponse<T>` 래퍼를 따른다.
            ```json
            { "success": true,  "data": { ... }, "message": null }
            { "success": false, "data": null,    "message": "한국어 오류 메시지" }
            ```

            ### 인증
            로그인 API로 JWT 토큰을 발급받은 후,
            우측 상단 **Authorize** 버튼에 `Bearer <token>` 형식으로 입력.

            ### 에러 코드 해석
            | 코드 | 의미 |
            |------|------|
            | 400  | 요청 파라미터/바디 검증 실패 |
            | 401  | 인증 토큰 없음 또는 만료 |
            | 403  | 권한 없음 (의도적으로 404 대신 403을 반환하는 케이스 존재) |
            | 404  | 리소스 없음 |
            | 409  | 충돌 (중복 데이터 등) |
            | 410  | 리소스 영구 삭제됨 (탈퇴 계정 등) |
            | 500  | 서버 내부 오류 |

            Breaking Change 발생 시: Slack #api-contract 채널 공지 확인.
            """;
    }

    // ── 컴포넌트 (스키마 + 보안) ─────────────────────────────────────────────

    private Components buildComponents() {
        return new Components()
            .addSecuritySchemes("bearerAuth", buildBearerScheme())
            .addSchemas("ApiResponseSuccess", buildApiResponseSuccessSchema())
            .addSchemas("ApiResponseError", buildApiResponseErrorSchema());
    }

    private SecurityScheme buildBearerScheme() {
        return new SecurityScheme()
            .name("bearerAuth")
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")
            .description("로그인 후 발급받은 JWT 토큰. 형식: Bearer <token>");
    }

    @SuppressWarnings("rawtypes")
    private Schema buildApiResponseSuccessSchema() {
        Schema schema = new Schema<>();
        schema.setType("object");
        schema.addProperty("success", new Schema<Boolean>().type("boolean").example(true));
        schema.addProperty("data", new Schema<>().type("object").description("실제 응답 데이터"));
        schema.addProperty("message", new Schema<>().type("string").nullable(true).example(null));
        return schema;
    }

    @SuppressWarnings("rawtypes")
    private Schema buildApiResponseErrorSchema() {
        Schema schema = new Schema<>();
        schema.setType("object");
        schema.addProperty("success", new Schema<Boolean>().type("boolean").example(false));
        schema.addProperty("data", new Schema<>().type("object").nullable(true).example(null));
        schema.addProperty("message",
            new Schema<String>().type("string").example("사용자를 찾을 수 없습니다."));
        return schema;
    }

    // ── 공통 에러 응답 ────────────────────────────────────────────────────────

    private ApiResponse unauthorizedResponse() {
        return new ApiResponse()
            .description("인증 토큰이 없거나 만료되었습니다.")
            .content(errorContent("인증이 필요합니다. 토큰을 확인하세요."));
    }

    private ApiResponse internalServerErrorResponse() {
        return new ApiResponse()
            .description("서버 내부 오류. 동일한 요청이 반복되면 백엔드 팀에 문의하세요.")
            .content(errorContent("서버 내부 오류가 발생했습니다."));
    }

    @SuppressWarnings("rawtypes")
    private Content errorContent(String message) {
        Schema schema = new Schema<>().$ref("#/components/schemas/ApiResponseError");
        MediaType mediaType = new MediaType().schema(schema);
        mediaType.addExamples("error",
            new io.swagger.v3.oas.models.examples.Example()
                .value("{\"success\":false,\"data\":null,\"message\":\"" + message + "\"}"));
        return new Content().addMediaType("application/json", mediaType);
    }
}
