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

작업 시작 전 아래 항목을 확인한다:

### 패키지 구조 확인
```
src/main/java/com/ssac/
├── domain/         ← 도메인별 패키지 구조 확인
│   ├── user/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── entity/
│   │   └── dto/
│   ├── news/
│   ├── quiz/
│   └── auth/
├── global/         ← 공통 설정 / 예외 처리
│   ├── config/
│   ├── exception/
│   └── response/
└── infra/          ← 외부 연동
```

### 확인 항목
□ 새로 추가할 도메인 패키지가 이미 존재하는가?
□ 기존 엔티티에 필드 추가인가? 신규 엔티티 생성인가?
□ 기존 API 경로와 충돌하는 엔드포인트가 있는가?
□ 기존 ErrorCode와 중복되는 코드가 있는가?
□ Flyway 마이그레이션 버전이 순차적으로 유지되는가?

---

## STEP 2. SC 항목별 구조 충돌 점검

### API 엔드포인트 점검
□ 새로 추가할 API 경로가 기존 경로와 중복되지 않는가?
  → 확인: @RequestMapping / @GetMapping 전체 검색
  예) 기존: POST /api/v1/auth/register
      신규: POST /api/v1/auth/register  ← 중복 ❌

□ API 경로 네이밍 컨벤션을 준수하는가?
  → 기준: /api/v1/{도메인}/{리소스}
  예) ✅ /api/v1/users/type
      ❌ /api/userType

□ HTTP 메서드가 RESTful 원칙에 맞는가?
  → 조회: GET / 생성: POST / 수정: PATCH / 삭제: DELETE

### 엔티티 / DB 점검
□ 새로 추가할 필드가 기존 엔티티에 이미 존재하는가?
  → 중복 필드 추가 금지

□ Flyway 마이그레이션 버전이 순차적인가?
  → 현재 최신 버전 확인 후 다음 버전으로 생성
  예) 현재 V5 존재 → 신규는 V6으로 생성
      V5를 건너뛰고 V7 생성 ❌

□ 기존 테이블 구조와 충돌하는 컬럼명이 없는가?
  → 예약어 사용 금지 (type, order, group 등)

### 서비스 / 의존성 점검
□ 새로 추가할 서비스가 기존 서비스와 기능이 중복되는가?
  → 중복 기능은 기존 서비스에 메서드 추가로 처리

□ 새로 추가할 Repository가 TokenStore 등
  추상화 인터페이스 원칙을 준수하는가?
  → Service → Interface → Impl 구조 유지

### ErrorCode 점검
□ 새로 추가할 ErrorCode가 기존 코드와 중복되는가?
  → ErrorCode Enum 전체 검색
  예) 기존: USER-001 / 신규: USER-001 ← 중복 ❌

□ ErrorCode 네이밍 컨벤션을 준수하는가?
  → 기준: {도메인}-{순번}
  예) ✅ USER-TYPE-001
      ❌ USERTYPE001

### 테스트 구조 점검
□ 기존 테스트 파일과 동일한 이름의 파일이 없는가?
□ 새로 추가할 테스트가 기존 테스트 커버리지에
  영향을 주지 않는가?

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
