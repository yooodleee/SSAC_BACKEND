# Debug Log

## Controller 테스트 커버리지 분류 (2025-05-30)

### 개요
- 총 Controller 수: 28개
- 기존 테스트 보유: 6개 (NaverOAuthController, TokenController ×2, AuthTokenController, AuthStatusController, OnboardingController)
- 미테스트 Controller: 22개 → P1~P4 분류

---

### P1 — 즉시 작성 (회원가입 핵심 플로우)

| Controller | 메서드 | 이유 |
|-----------|--------|------|
| AuthController | saveTerms, register, checkNickname | 신규 회원 가입 전체 플로우 핵심 경로 |

**작성 완료:** `AuthControllerTest.java` (4 tests)

---

### P2 — 이번 스프린트 (주요 사용자/관리자 기능)

| Controller | 메서드 | 이유 |
|-----------|--------|------|
| UserController | getMyPage, updateInterests, updateUserType, updateProfile, updateNickname, withdraw, getViewedContents | 마이페이지 핵심 |
| AdminController | createAdminCode, getHome, getFeedbacks, updateFeedbackStatus, listUsers, updateUserRole | 관리자 핵심 |

**작성 완료:** `UserControllerTest.java` (7 tests), `AdminControllerTest.java` (6 tests)

**참고:** `OnboardingControllerTest.java` 이미 존재 (7 tests — 비로그인/로그인 분기 검증)

---

### P3 — 다음 스프린트

| Controller | 메서드 수 | 이유 |
|-----------|---------|------|
| ContentController | 4 | 학습 완료/조회 핵심 |
| QuizAttemptController | 5 | 퀴즈 이력 핵심 |
| AuthV1Controller | 4 | 이메일 회원가입/로그인 |
| ProfileController | 3 | 프로필 수정 |
| ResumeController | 2 | 이어보기 |
| NotificationController | 3 | 알림 |
| SearchController | 2 | 검색 |
| HomeController | 1 | 홈 화면 |
| AdminLogController | 1 | 관리자 로그 조회 |

---

### P4 — 추후 (단순 위임 / 외부 처리 / 개발 전용)

| Controller | 이유 |
|-----------|------|
| KakaoOAuthController | Swagger 더미 — Spring Security가 직접 처리 / **excludes 등록** |
| DevAuthController | `@Profile("!prod")` 개발 환경 전용 / **excludes 등록** |
| NaverOAuthController | OAuth redirect, Spring Security 처리 / NaverOAuthServiceTest로 커버 |
| AdminMenuStatsController | 단순 조회 1개 메서드 |
| AdminContentController | 관리자 Notion 동기화 (외부 의존) |
| AdminLoginController | 관리자 로그인 (AdminService 위임) |
| FeedbackController | 공개 피드백 제출 단순 위임 |
| RecommendationController | 로그인 필요 단순 조회 |
| UserSegmentController | 로그인 필요 단순 조회 |
| AbTestController | 공개 단순 조회 |
| NewsController | 공개 단순 조회 |
| MenuClickEventController | 공개 이벤트 기록 |

---

### JaCoCo Rule 3/4 단계적 상향 계획

| 단계 | Rule 3 (전체) | Rule 4 (클래스별) | 시기 |
|-----|------------|----------------|-----|
| 현재 (1단계) | 60% | 40% | 2025-05-30 |
| 2단계 | 70% | 50% | P3 완료 후 |

---

### 진단 기록

| 일자 | 이슈 | 원인 | 조치 |
|-----|------|------|------|
| 2025-05-30 | Controller 전체 커버리지 21% | 22개 Controller 미테스트 | Rule 3/4 추가 + P1/P2 테스트 작성 |
