# Swagger 에이전트 친화적 작성 가이드

이 파일은 컨트롤러 코드에 Swagger 어노테이션을 작성하는 기준이다.
에이전트가 이 규칙을 따르지 않으면 PR이 거절된다.

---

## 핵심 원칙

> **에이전트(프론트엔드/AI)는 코드를 보지 않는다. `description`만 본다.**
> `description`이 불완전하면 에이전트는 잘못된 방식으로 API를 호출한다.

---

## 필수 작성 항목

모든 `@Operation`의 `description`에 아래 3가지를 반드시 포함해야 한다.

| 항목 | 작성 형식 | 예시 |
|------|-----------|------|
| **호출 화면/상황** | `[호출 화면]` 태그 | `[호출 화면] 프로필 페이지 진입 시 호출.` |
| **권한 조건** | `[권한 조건]` 태그 | `[권한 조건] 본인 또는 ADMIN만 가능.` |
| **특이 동작** | `[특이 동작]` 태그 | `[특이 동작] 비활성 계정은 403 반환 (의도됨).` |

특이 동작이 없는 경우 해당 태그는 생략 가능.

---

## 예시 코드 — 나쁜 예 vs. 좋은 예

### ❌ 나쁜 예 (에이전트가 오해하는 문서)

```java
@GetMapping("/users/{userId}")
@Operation(
    summary = "Get user",
    description = "유저를 가져옵니다."
)
@ApiResponse(responseCode = "200", description = "성공")
public ApiResponse<UserResponse> getUser(@PathVariable Long userId) { ... }
```

**문제점**:
- 어느 화면에서 호출하는지 알 수 없다
- 403이 언제 발생하는지, 의도된 것인지 알 수 없다
- 401, 403 등 에러 응답이 누락되어 에이전트가 에러 핸들링을 못한다

---

### ✅ 좋은 예 (에이전트가 신뢰하는 문서)

```java
@GetMapping("/users/{userId}")
@Operation(
    summary = "특정 사용자 정보 조회",
    description = """
        [호출 화면] 프로필 페이지(/profile/{userId}), 설정 페이지 진입 시 호출.
        [권한 조건] 로그인한 사용자 본인 또는 ADMIN 역할만 접근 가능.
        [특이 동작] 비활성 계정은 404가 아닌 403 반환 — 계정 존재 여부 노출 방지 (의도된 설계).
                    탈퇴한 계정(소프트 삭제)은 410 반환.
        """
)
@ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "조회 성공",
        content = @Content(schema = @Schema(ref = "#/components/schemas/ApiResponseSuccess"))
    ),
    @ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료"),
    @ApiResponse(responseCode = "403", description = "권한 없음 또는 비활성 계정 (의도된 설계)"),
    @ApiResponse(responseCode = "410", description = "탈퇴한 계정")
})
public ApiResponse<UserResponse> getUser(@PathVariable Long userId) { ... }
```

---

## 에러 응답 코드 기준

아래 코드는 각 상황에서 반드시 명시해야 한다.

| HTTP 코드 | 명시 필수 조건 |
|-----------|----------------|
| `400` | 요청 파라미터/바디 검증 실패 가능성이 있는 모든 엔드포인트 |
| `401` | Spring Security가 인증을 요구하는 모든 엔드포인트 (전역 설정으로 자동 추가됨) |
| `403` | 역할/권한 분기가 있는 엔드포인트 |
| `404` | ID 기반 조회 엔드포인트 |
| `409` | 중복 생성 가능성이 있는 엔드포인트 |
| `410` | 소프트 삭제된 리소스 조회 가능성이 있는 엔드포인트 |
| `500` | 전역 설정으로 자동 추가됨 — 개별 명시 불필요 |

> **주의**: `401`과 `500`은 `SwaggerConfig.globalErrorResponseCustomizer()`가 자동으로 추가한다.
> 개별 컨트롤러에서 중복 명시해도 무방하지만, 생략 가능.

---

## 스키마 작성 기준

DTO에 `@Schema` 어노테이션으로 `example` 값을 반드시 포함한다.

```java
public record UserResponse(
    @Schema(description = "사용자 ID", example = "42")
    Long id,

    @Schema(description = "이메일 주소", example = "user@ssac.com")
    String email,

    @Schema(description = "계정 상태", example = "ACTIVE",
            allowableValues = {"ACTIVE", "INACTIVE", "DELETED"})
    String status
) {}
```

---

## 태그(tag) 기준

컨트롤러 클래스에 `@Tag`를 달아 Swagger UI에서 도메인별로 그룹화한다.

```java
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User", description = "사용자 관련 API — 가입, 조회, 수정, 탈퇴")
public class UserController { ... }
```
