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
| 2026-05-31 | HomeService GenericJackson2JsonRedisSerializer 사용 | RedisConfig에서 RedisTemplate\<String, Object\>에 타입 정보 포함 직렬화 설정 | self-diagnose.md CACHE-1 점검 항목 추가 / CLAUDE.md 금지 규칙 추가 |
| 2026-05-31 | NotionSyncService Redis 조회 try-catch 미적용 | stringRedisTemplate.opsForValue().get() 호출 시 Redis 연결 오류 예외 전파 가능 | self-diagnose.md CACHE-2 fallback 점검 항목 추가 |

---

## Redis 캐싱 서비스 전수 점검 (2026-05-31)

### 점검 기준: self-diagnose.md CACHE-1 ~ CACHE-5

### NotionSyncService

| 항목 | 결과 | 비고 |
|-----|------|------|
| CACHE-1 직렬화 방식 | ✅ | StringRedisTemplate 수동 캐싱 (OBJECT_MAPPER + TypeReference) |
| CACHE-2 캐시 키 형식 | ✅ | `contents:v4:list:{categories}:{difficulty}:{domain}` |
| CACHE-2 직렬화 대칭 | ✅ | writeValueAsString / readValue 대칭 구현 |
| CACHE-2 fallback 처리 | ⚠️ | 역직렬화 실패 시 DB 재조회 있음. 단, Redis 연결 오류 시 (get/set 호출) try-catch 없어 예외 전파 가능 |
| CACHE-3 TTL 명시 | ✅ | CACHE_TTL_SECONDS = 3600L, Duration.ofSeconds 사용 |
| CACHE-4 캐시 무효화 | ✅ | syncAll() 완료 후 evictContentsCache() 호출, contents:v4:* 전체 삭제 |

### ContentService

| 항목 | 결과 | 비고 |
|-----|------|------|
| Redis 직접 사용 | ✅ | NotionSyncService에 위임 |
| 상세 조회 캐싱 | N/A | Notion 블록 실시간 조회 (의도적 설계) |

### HomeService

| 항목 | 결과 | 비고 |
|-----|------|------|
| CACHE-1 직렬화 방식 | ❌ | `RedisTemplate<String, Object>` + `GenericJackson2JsonRedisSerializer` 사용. activateDefaultTyping으로 타입 정보 포함 → 클래스 구조 변경 시 역직렬화 오류 위험 |
| CACHE-2 fallback 처리 | ✅ | try-catch로 Redis 불가용 시 DB 직접 조회 및 저장 실패 무시 |
| CACHE-3 TTL 명시 | ✅ | computeTtlUntilMidnight() (당일 자정), REC_HISTORY 7일 TTL |
| CACHE-4 캐시 무효화 | ✅ | HomeCacheEvictService.evict() — 콘텐츠 완료/온보딩/관심 도메인 변경 시 호출 |

### OnboardingService

| 항목 | 결과 | 비고 |
|-----|------|------|
| Redis 직접 사용 | ✅ | HomeCacheEvictService 위임 |
| CACHE-4 캐시 무효화 | ✅ | submit/skip/saveInterests/resetOnboarding 모두 evict() 호출 |

---

### 발견된 문제 요약

| 우선순위 | 서비스 | 문제 | 위험도 |
|---------|--------|------|--------|
| P1 | HomeService | GenericJackson2JsonRedisSerializer 사용 (CACHE-1 ❌) | 높음 — 클래스 리팩토링 시 역직렬화 오류 발생 가능 |
| P2 | NotionSyncService | Redis 조회/저장 시 연결 오류 catch 없음 (CACHE-2 ⚠️) | 중간 — Redis 일시 불가용 시 콘텐츠 목록 API 전체 실패 가능 |
