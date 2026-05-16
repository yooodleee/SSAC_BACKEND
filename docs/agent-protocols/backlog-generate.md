# Backlog SC 생성 하네스 (BE)

## 역할
외부에서 제공된 백로그 SC를 그대로 사용하지 않는다.
반드시 프로젝트 구조와 맥락을 먼저 파악한 후
기존 코드와 충돌 없는 SC를 직접 생성하거나 수정합니다.

## 트리거 조건 (자동 실행)
- 사용자가 백로그 SC를 제공했을 때
- 사용자가 "백로그", "SC", "구현해줘" 언급 시
- 새로운 기능 추가 요청을 받았을 때
- sc-harness.md 실행 완료 직후

## 실행 순서
STEP 1. 프로젝트 구조 파악
STEP 2. 관련 기존 코드 분석
STEP 3. 제공된 SC 검토
STEP 4. 충돌 감지 및 SC 수정
STEP 5. 최종 SC 확정 및 출력

---

## STEP 1. 프로젝트 구조 파악

백로그 SC 검토 전 반드시 아래 항목을 확인한다.

### 1-1. 패키지 구조 확인
src/main/java/com/ssac/ 디렉터리 구조 파악
-> 도메인별 패키지 목록 확인
-> 공통 설정 / 예외 처리 구조 확인

### 1-2. 핵심 파일 목록 확인
아래 파일을 순서대로 읽는다:

□ CLAUDE.md
    -> 전역 규칙 / 프로토콜 실행 순서 파악

□ docs/conventions/flyway.md
    -> 마이그레이션 컨벤션 파악
    -> 현재 최신 마이그레이션 버전 확인

□ src/main/java/com/ssac/ssacbackend/common/exception/ErrorCode.java
    -> 기존 ErrorCode 전체 목록 파악
    -> 신규 ErrorCode 번호 결정 시 충돌 방지

□ src/main/java/com/ssac/ssacbackend/common/response/ApiResponse.java
    -> 공통 응답 구조 파악

□ src/main/resources/application-docker.yml
    -> 현재 환경 변수 / 설정 파악

□ docs/api/swagger.json (존재하는 경우)
    -> 기존 API 경로 전체 목록 파악
    -> 신규 API 경로 중복 방지
    -> 미존재 시: controller/ 패키지에서 @RequestMapping / @GetMapping 등을 grep하여 경로 목록 수집

### 1-3. 관련 도메인 파일 확인
SC와 관련된 패키지의 아래 파일을 읽는다.
□ Controller -> 기존 API 경로 / 요청-응답 구조
□ Service    -> 기존 비즈니스 로직
□ Repository -> 기존 쿼리 메서드
□ Entity     -> 기존 필드 / 제약 조건
□ DTO        -> 기존 요청-응답 DTO 구조

파악 완료 후 아래 형식으로 출력한다:

[프로젝트 구조 파악 완료]
- 최신 Flyway 버전: VN
- 다음 마이그레이션: V(N+1)
- 기존 API 경로 수: N개
- 관련 도메인 파일: XxxController / XxxService / XxxEntity

---

## STEP 2. 관련 기존 코드 분석

SC와 관련된 기존 코드를 아래 기준으로 분석한다:

### 2-1. 기존 API 분석
□ SC에서 언급된 API가 이미 존재하는가?
    -> 존재: 기존 API 수정 / 미존재: 신규 API 생성
□ 기존 API의 요청 / 응답 구조는 무엇인가?
    -> 공통 응답 래퍼(ApiResponse) 사용 여부 확인
□ 기존 API의 인증 방식은 무엇인가?
    -> SecurityConfig의 PUBLIC / AUTHENTICATED 설정 확인

### 2-2. 기존 엔티티 분석
□ SC에서 추가가 필요한 필드가 이미 존재하는가?
□ 기존 엔티티의 연관 관계는 어떻게 되어 있는가?
□ 기존 제약 조건(NOT NULL / UNIQUE 등)은 무엇인가?

### 2-3. 기존 예외 처리 분석
□ SC에서 요구하는 ErrorCode가 이미 존재하는가?
□ 기존 예외 처리 방식은 어떻게 되어 있는가?
    -> GlobalExceptionHandler 처리 방식 확인

### 2-4. 기존 토큰 / 인증 분석 (인증 관련 SC인 경우)
□ Refresh Token 저장 방식은 무엇인가?
    -> HttpOnly Cookie / Redis / DB 여부 확인
□ 기존 CookieUtils 활용 방식 확인
□ SecurityConfig에서 PUBLIC으로 설정된 경로 확인

분석 완료 후 아래 형식으로 출력한다:

[기존 코드 분석 완료]
- 기존 API 존재 여부: 존재 / 미존재
- 기존 응답 구조: ApiResponse<XxxResponse>
- 인증 방식: @CookieValue / @RequestHeader
- 관련 ErrorCode: AUTH-001 ~ AUTH-006 기존 사용 중

---

## STEP 3. 제공된 SC 검토 — 설계 패턴

SC의 내용이 이 프로젝트의 설계 패턴과 일치하는지 검토한다.
STEP 1-2에서 수집한 정보를 활용한다.
코드베이스 실제 충돌 검증(경로 중복, ErrorCode 중복, Flyway 버전)은
다음 단계인 sc-structure-check.md에서 수행한다.

### 3-1. API 설계 패턴 검토
□ API 경로가 기존 컨벤션(/api/v1/{도메인}/{리소스})을 따르는가?
□ API 요청 방식이 기존 구현과 일치하는가?
    예) Refresh Token -> body X / Cookie O
□ API 응답 구조가 ApiResponse<T> 래퍼를 사용하는가?
□ HTTP 메서드가 RESTful 원칙에 맞는가?
    조회: GET / 생성: POST / 수정: PATCH / 삭제: DELETE

### 3-2. ErrorCode 설계 검토
□ STEP 2에서 파악한 기존 ErrorCode를 기준으로,
  SC가 제시하는 ErrorCode의 도메인 분류가 적절한가?
    예) 인증 관련인데 USER-XXX로 분류되어 있으면 AUTH-XXX로 수정
□ ErrorCode 네이밍 컨벤션을 준수하는가?
    기준: {도메인}-{순번}  예) ✅ AUTH-011  ❌ AUTH011
※ 구체적인 번호 중복 / 순번 검증은 sc-structure-check.md에서 수행한다.

### 3-3. SQL / 마이그레이션 설계 검토
□ SC에 SQL 문법이 포함된 경우 MySQL 호환 문법을 사용하는가?
    -> ADD COLUMN IF NOT EXISTS 사용 여부 확인
        (MySQL 미지원 -> information_schema 패턴 사용)
    -> CREATE INDEX IF NOT EXISTS 사용 여부 확인
        (MySQL 미지원 -> information_schema 패턴 사용)
※ 칼럼/테이블 실제 존재 여부 및 버전 순번 검증은 sc-structure-check.md에서 수행한다.

### 3-4. 보안 설계 검토
□ Refresh Token을 body로 전달하는 SC가 있는가?
    -> 쿠키 기반으로 수정 (CookieUtils.addRefreshTokenCookie 활용)
□ 민감한 정보(token, password, 개인정보)가 응답 body에 포함되는가?
    -> HttpOnly Cookie로 전환

검토 완료 후 아래 형식으로 출력한다:

[SC 설계 패턴 검토 결과]
✅ 적합 항목 : N개
❌ 수정 필요 : N개
⚠️ 주의 항목 : N개

---

## STEP 4. 충돌 감지 및 SC 수정

STEP 3에서 발견된 충돌 항목을 아래 기준으로 수정합니다.

### 수정 유형별 처리 방법

[API 요청 방식 오류]
원본: body에 refreshToken 포함
수정: @CookieValue 기반 쿠키 방식으로 변경
근거: CookieUtils.addRefreshTokenCookie 기존 구현 참고

[API 응답 구조 오류]
원본: 응답 body에 refreshToken 포함
수정: ApiResponse<XxxResponse> 래퍼 유지
      refreshToken은 Set-Cookie 헤더로 처리
근거: ApiResponse.java 공통 응답 구조 참고

[ErrorCode 도메인 분류 오류]
원본: USER-001 (인증 관련 에러인데 USER 도메인으로 분류)
수정: AUTH-XXX (도메인 재분류)
근거: ErrorCode.java 도메인 그룹 기준 참고
※ 번호 중복/순번 오류 수정은 sc-structure-check.md에서 담당한다.

[MySQL 미지원 문법]
원본: ADD COLUMN IF NOT EXISTS
수정: information_schema 조건부 패턴으로 교체
근거: docs/conventions/flyway.md 참고

수정 완료 후 아래 형식으로 출력한다:

[충돌 수정 완료]
수정 항목 1:
- 원본: POST body { "refreshToken": "string" }
- 수정: @CookieValue(name = "refreshToken")
- 근거: CookieUtils 기존 구현 방식 준수

수정 항목 2:
- 원본: AUTH-005 (만료된 Refresh Token)
- 수정: AUTH-011 (기존 AUTH-005는 다른 의미로 사용 중)
- 근거: ErrorCode.java AUTH-005 기존 정의 확인

---

## STEP 5. 최종 SC 확정 및 출력

수정이 완료된 SC를 아래 형식으로 최종 출력한다:

## 최종 Success Criteria (BE 구현 기준)

### 검토 요약:
- 원본 SC 항목 수: N개
- 충돌 없음: N개
- 수정 완료: N개
- 제외 (FE 관심사): N개

### 충돌 수정 내역
[수정 1] {수정 항목}
❌ 수정 전: {원본}
✅ 수정 후: {수정본}
근거: {기존 코드 / 파일명}

### 최종 SC 목록
1. {항목 1}
2. {항목 2}
...

-> 최종 SC 확정 완료.
    sc-structure-check.md를 실행합니다.
