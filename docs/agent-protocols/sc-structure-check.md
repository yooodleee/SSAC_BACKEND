# SC 프로젝트 구조 점검 하네스 (BE)

## 트리거 조건 (자동 실행)
- SC가 포함된 작업 지시를 받았을 때
- 새로운 API / 엔티티 / 서비스 추가 요청 시
- sc-harness.md 실행 완료 직후

## 실행 순서
STEP 1. 현재 프로젝트 구조 파악
STEP 2. SC 항목별 구조 충돌 점검
STEP 3. 점검 결과 출력
STEP 4. 진행 여부 판단

---

## STEP 1. 현재 프로젝트 구조 파악

> backlog-generate.md를 이미 실행했다면 STEP 1에서 수집한 구조 정보를 그대로 활용한다.
> backlog-generate.md를 실행하지 않은 경우에만 아래 구조를 직접 확인한다.

작업 시작 전 아래 항목을 확인한다:

### 패키지 구조 확인
```
src/main/java/com/ssac/ssacbackend/
├── config/         ← Spring Security, JWT, OAuth2, Cookie 등 설정
├── controller/     ← REST API 엔드포인트 (레이어 단위, 도메인 구분 없음)
├── service/        ← 비즈니스 로직
├── repository/     ← Spring Data JPA 인터페이스
├── domain/         ← Entity, Enum (도메인별 하위 패키지)
│   ├── user/       (User, UserRole, UserLevel, UserType, RefreshToken)
│   ├── quiz/       (Question, QuizAttempt, AttemptAnswer)
│   ├── news/       (News, NewsView)
│   ├── onboarding/ (OnboardingQuestion, UserInterest)
│   ├── social/     (SocialAccount, OAuthProvider)
│   ├── auth/       (PendingRegistration, AuthCode)
│   ├── notification/ (Notification)
│   └── event/      (MenuClickEvent)
├── dto/            ← 요청/응답 DTO (Java record 타입)
│   ├── request/
│   └── response/
└── common/         ← 공통 유틸리티
    ├── exception/  (BusinessException 계층, GlobalExceptionHandler, ErrorCode)
    ├── response/   (ApiResponse, ErrorResponse)
    └── util/       (CookieUtils)
```

### 확인 항목
□ 새로 추가할 Entity가 domain/{도메인명}/ 하위에 위치하는가?
□ 새로 추가할 Controller / Service / Repository가 각 레이어 패키지에 위치하는가?
□ 기존 엔티티에 필드 추가인가? 신규 엔티티 생성인가?
□ 기존 API 경로와 충돌하는 엔드포인트가 있는가?
□ 기존 ErrorCode와 중복되는 코드가 있는가?
□ Flyway 마이그레이션 버전이 순차적으로 유지되는가?

---

## STEP 2. SC 항목별 구조 충돌 점검

실제 파일을 읽거나 검색하여 충돌 여부를 확인한다.
API 설계 패턴 / MySQL 문법 / 보안 패턴은 backlog-generate.md에서 이미 검토됐다.
이 단계는 코드베이스와의 실제 충돌만 검증한다.

### API 엔드포인트 충돌 검증
□ 새로 추가할 API 경로가 기존 경로와 중복되지 않는가?
  → 확인 방법: controller/ 패키지에서 @RequestMapping / @PostMapping 등 검색
  예) 기존: POST /api/v1/auth/register
      신규: POST /api/v1/auth/register  ← 중복 ❌

### 엔티티 / DB 충돌 검증
□ 새로 추가할 필드가 기존 엔티티에 이미 존재하는가?
  → 확인 방법: 해당 Entity 파일을 직접 읽어 필드 목록 확인

□ Flyway 마이그레이션 버전이 순차적인가?
  → 확인 방법: db/migration/ 디렉터리의 V*.sql 파일 목록 확인
  예) 현재 V20 존재 → 신규는 V21로 생성
      V20을 건너뛰고 V22 생성 ❌

□ 추가할 컬럼명이 DB 예약어와 충돌하지 않는가?
  → 예약어 사용 금지: type, order, group, value, key, index 등
  → 충돌 시 접두어 추가: user_type, sort_order 등

### 서비스 / 의존성 충돌 검증
□ 새로 추가할 서비스 기능이 기존 서비스에 이미 구현되어 있는가?
  → 확인 방법: 관련 Service 파일을 읽어 메서드 목록 확인
  → 중복 기능은 신규 서비스 생성 대신 기존 서비스에 메서드 추가로 처리

□ 추상화 인터페이스가 필요한 경우 기존 패턴을 따르는가?
  → Service → Interface → Impl 구조 (TokenStore 패턴 참고)

### ErrorCode 충돌 검증
□ 새로 추가할 ErrorCode 값이 기존 코드와 중복되는가?
  → 확인 방법: ErrorCode.java 전체 목록에서 코드 문자열 확인
  예) 기존: USER-001 / 신규: USER-001 ← 중복 ❌

□ 새로 추가할 ErrorCode 번호가 해당 도메인의 다음 순번인가?
  → 확인 방법: ErrorCode.java에서 해당 도메인의 마지막 번호 확인
  예) AUTH-010까지 존재 → 신규는 AUTH-011부터 사용

### 테스트 구조 충돌 검증
□ 새로 추가할 테스트 파일명이 기존 테스트 파일과 중복되지 않는가?
  → 확인 방법: src/test/ 디렉터리에서 동일 파일명 검색

---

## STEP 3. 점검 결과 출력

아래 형식으로 점검 결과를 출력한다:

## BE 프로젝트 구조 점검 결과

### API 엔드포인트
✅ POST /api/v1/users/type — 기존 경로와 충돌 없음
❌ POST /api/v1/auth/register — 기존 API 존재
   → 기존 API에 userType 필드 추가로 처리 필요

### 엔티티 / DB
✅ user_type 컬럼 — 기존 users 테이블에 미존재
✅ Flyway V5 — 현재 최신 V4 다음 순번으로 적합
⚠️ type 컬럼명 — DB 예약어 가능성
   → user_type으로 변경 권장

### ErrorCode
✅ USER-TYPE-001 — 기존 코드와 중복 없음
❌ USER-001 — 기존 ErrorCode와 중복
   → USER-TYPE-002로 변경 필요

### 점검 요약
- 전체 SC 항목 수 : N개
- ✅ 구조 적합     : N개
- ❌ 구조 충돌     : N개 → 수정 후 진행 필요
- ⚠️ 주의 필요     : N개 → 검토 후 진행

---

## STEP 4. 진행 여부 판단

if (❌ 구조 충돌 항목이 1개 이상):
  → 구현을 시작하지 않는다
  → 충돌 항목 목록을 출력한다
  → "기존 프로젝트 구조와 충돌이 발견되었습니다.
     위 항목을 검토 후 SC를 수정하여 다시 진행해주세요."
  → 대기한다

if (⚠️ 주의 항목만 존재):
  → 주의 항목 내용을 출력한다
  → "주의가 필요한 항목이 있습니다.
     검토 후 진행 여부를 결정해주세요."
  → 사용자 확인 후 진행한다

if (모든 항목 ✅):
  → "프로젝트 구조 점검 완료. 구현을 시작합니다."
  → 구현을 진행한다
