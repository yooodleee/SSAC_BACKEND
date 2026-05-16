# Contract Sync Protocol (BE)

## 역할
코드베이스와 계약 파일 간 정합성을 검증하고 동기화한다.

API 계약(docs/api/swagger.json)은 springdoc-openapi가 자동 생성하므로
에이전트가 직접 편집하지 않는다 (ADR 004).
에이전트 관리 대상은 아래 두 가지다:
- (A) `ErrorCode.java` ↔ `contract/error-contract.yml`
- (B) `controller/` 패키지의 `@Operation` / `@ApiResponses` 어노테이션 완성도

## 트리거 조건 (자동 실행)
- `ErrorCode.java` 수정 시 → (A) 실행
- Controller 메서드 추가 또는 API 경로 수정 시 → (B) 실행
- new-feature.md STEP 7 실행 직후 → 자동

## 수동 실행
- 스프린트 단위 전체 감사 시

## 실행 순서
STEP 1. ErrorCode 계약 동기화
STEP 2. Swagger 어노테이션 감사
STEP 3. 점검 결과 출력 및 완료 판단

---

## STEP 1. ErrorCode 계약 동기화

### 검증 명령
```bash
./gradlew validateErrorContract
```

### 성공 시
→ ✅ error-contract.yml 정합성 확인. STEP 2로 진행한다.

### 실패 시 — 갱신 절차

**1) 추가된 ErrorCode**
`contract/error-contract.yml`의 `errors` 목록에 항목을 추가한다:
```yaml
- code: "DOMAIN-NNN"
  status: 4XX
  message: "에러 메시지"
  domain: "DOMAIN"
```
version 증가 규칙: patch (예: 1.0.2 → 1.0.3)

**2) 메시지/상태코드 수정된 ErrorCode**
해당 항목을 수정한다.
version 증가 규칙: minor (예: 1.0.2 → 1.1.0)

**3) 삭제 또는 코드 변경된 ErrorCode**
해당 항목을 제거하거나 code 값을 변경한다.
version 증가 규칙: major (예: 1.0.2 → 2.0.0)

**4) updatedAt 갱신**
```yaml
updatedAt: "YYYY-MM-DD"  # 오늘 날짜
```

**5) 재검증**
갱신 완료 후 `./gradlew validateErrorContract`를 재실행하여 통과를 확인한다.

---

## STEP 2. Swagger 어노테이션 감사

### 탐지 대상
`controller/` 패키지에서 HTTP 매핑 어노테이션이 있는 메서드 중
`@Operation`이 없는 항목을 탐지한다.

탐지 방법:
```bash
# @Operation 누락 후보 파일 확인 (GetMapping/PostMapping/PatchMapping/DeleteMapping 보유)
grep -rL "@Operation" src/main/java/com/ssac/ssacbackend/controller/
```

### 누락 시 — ADR 004 기준으로 어노테이션 추가

```java
@Operation(
    summary = "기능 한 줄 요약",
    description = """
        [호출 화면] 어느 화면에서 이 API를 호출하는가.
        [권한 조건] 누가 호출할 수 있는가 (공개 / 로그인 필요 / ADMIN 전용).
        [특이 동작] 일반적이지 않은 응답 동작이 있으면 기술한다.
        """
)
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "성공"),
    @ApiResponse(responseCode = "400", description = "잘못된 요청"),
    @ApiResponse(responseCode = "401", description = "인증 필요"),
    @ApiResponse(responseCode = "404", description = "리소스 없음")
    // 실제 발생 가능한 상태코드만 명시한다
})
```

> `docs/api/swagger.json`은 CI(master 머지 시)가 자동으로 재생성하므로
> 에이전트가 직접 편집하지 않는다.

### 감사 완료 기준
- 신규/수정 메서드에 `@Operation`과 `@ApiResponses`가 모두 존재한다
- `description`에 `[호출 화면]`, `[권한 조건]` 항목이 포함되어 있다

---

## STEP 3. 점검 결과 출력 및 완료 판단

아래 형식으로 출력한다:

```
[계약 동기화 결과]
(A) error-contract.yml : ✅ 정합 (validateErrorContract 통과)
(B) @Operation 감사    : ✅ 전체 완성 / ❌ N개 메서드 누락
```

### 완료 판단 규칙

**모든 항목 ✅ 인 경우:**
→ "계약 동기화 완료." 선언 후 다음 단계(testing.md)로 진행한다.

**❌ 항목이 1개 이상인 경우:**
→ 수정 완료 후 해당 STEP을 재실행한다.
→ 모든 ❌ 해소 전까지 완료 선언 금지.
