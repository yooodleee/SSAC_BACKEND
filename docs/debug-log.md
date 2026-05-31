# Debug Log

## 개요
이 파일은 운영 오류 진단 / ADR 생성 / 하네스 감사 결과를 누적 기록하는 이력 문서입니다.

## 기록 규칙
```
□ 최신 기록을 파일 상단에 추가한다 (역순)
□ 기록 유형은 아래 3가지로 구분한다:
    [DIAGNOSE]  : log-diagnose.md 실행 결과
    [ADR]       : adr-create.md 실행 결과
    [AUDIT]     : harness-audit.md 실행 결과
□ 해결 완료된 항목은 ✅로 표시한다
□ 미해결 항목은 🔴로 표시한다
□ 이 파일을 직접 편집하여 내용을 삭제하지 않는다
```

## 반복 오류 판단 기준
동일한 오류 메시지 / 원인이 3회 이상 기록된 경우 adr-create.md를 즉시 실행한다.

---

## 기록 양식

### [DIAGNOSE] 운영 오류 진단 기록

```
**[DIAGNOSE] YYYY-MM-DD HH:mm**
상태: 🔴 미해결 / ✅ 해결 완료

#### 오류 개요
- 발생 환경  : Railway 운영 / 로컬
- 서비스     : ssac-backend / Redis / FE
- 오류 유형  : Redis 연결 / 직렬화 / Flyway / 기타
- 오류 메시지:
{핵심 오류 메시지 또는 스택 트레이스 요약}

#### 진단 과정
- STEP 2 서비스 상태: 정상 / 비정상
- STEP 3 로그 분석  : {핵심 발견 내용}
- STEP 4 Redis URL : 확인 / 미확인 / 해당 없음
- STEP 5 Redis 조회: {조회 결과 요약 / 해당 없음}

#### 근본 원인
{Why 분석 결과 요약}

#### 조치 내용
{수행한 조치 내용}

#### 재발 방지
- 프로토콜 갱신: Y → {갱신 대상 파일} / N
- ADR 작성    : Y → {ADR 번호 및 제목} / N
- 관련 SC     : {백로그 SC 번호}

#### 해결 시각
{YYYY-MM-DD HH:mm}
```

---

### [ADR] 기술 의사결정 기록

```
**[ADR] YYYY-MM-DD HH:mm**
상태: ✅ 완료

#### ADR 정보
- 파일명   : docs/adr/ADR-{번호}-{제목}.md
- 트리거   : 반복 오류 N회 / 기술 의사결정
- 핵심 결정:
    {한 줄 요약}

#### 프로토콜 갱신 항목
- {프로토콜 파일명} → {갱신 내용}

#### 등록된 백로그 SC
- {SC 번호}: {SC 제목}

#### 완료 시각
{YYYY-MM-DD HH:mm}
```

---

### [AUDIT] 하네스 감사 기록

```
**[AUDIT] YYYY-MM-DD HH:mm**
상태: ✅ 완료

#### 감사 요약
| 구분     | 건수 |
|---------|-----|
| Critical | N개 |
| High     | N개 |
| Medium   | N개 |
| Low      | N개 |
| 정상     | N개 |

#### 주요 발견 항목
- ❌ {Critical / High 항목}
- ⚠️ {Medium 항목}

#### 등록된 보완 백로그 SC
- HARNESS-N: {SC 제목}

#### 다음 감사 예정
{YYYY-MM-DD} (스프린트 종료 시)

#### 완료 시각
{YYYY-MM-DD HH:mm}
```

---

## 반복 오류 자동 감지 규칙

에이전트는 log-diagnose.md 실행 후 debug-log.md에 [DIAGNOSE] 기록을 추가하기 전
아래 절차를 수행한다:

```
STEP A. 동일 오류 이력 검색
→ debug-log.md에서 동일한 오류 메시지 / 근본 원인 키워드를 검색한다

STEP B. 반복 횟수 집계
→ 동일 항목이 몇 회 기록되어 있는가?

STEP C. 판단
→ 2회 이하: 일반 기록 후 종료
→ 3회 이상: 기록 후 즉시 adr-create.md 실행
            "동일 오류 3회 감지 → adr-create.md를 실행합니다."

판단 출력 형식:
[반복 오류 감지]
오류 유형  : {오류 유형}
발생 횟수  : N회 ({날짜} / {날짜} / 오늘)
판단       : adr-create.md 즉시 실행
```

---

<!-- 아래에 기록을 추가한다 (최신순) -->

---

**[DIAGNOSE] 2026-05-31 — 네이버 로그인 상태 유지 불가**
상태: ✅ 해결 완료

#### 오류 개요
- 발생 환경  : 운영 (Railway)
- 서비스     : ssac-backend (AuthTokenController / TokenController)
- 오류 유형  : 인증 / Cookie 미설정
- 오류 메시지: 네이버 로그인 후 새로고침 시 로그인 상태 초기화

#### 진단 과정
- 시나리오   : E2E 패턴 2 (401 반복) + 패턴 1 (Cookie 미저장)
- 로그인 API : `POST /api/v1/auth/token` → JSON 바디에 accessToken / refreshToken 반환
- Cookie    : refreshToken **쿠키 설정 없음** (SET-COOKIE 헤더 미발행)
- reissue   : `POST /api/v1/auth/reissue` → `@CookieValue(name="refreshToken")` 쿠키 요구
- /users/me : 새로고침 후 401 (refreshToken 쿠키 없어 reissue 실패)

#### AI 추론 결과 (패턴 1 + 패턴 2 복합)
**근본 원인 — 5-Why:**
1. 왜 새로고침 시 로그인이 풀리는가?
   → `refreshToken`이 없어 자동 로그인(`reissue`)이 불가하기 때문
2. 왜 `refreshToken`이 없는가?
   → `reissue`는 `refreshToken` **쿠키**를 요구하지만, 쿠키가 없기 때문
3. 왜 `refreshToken` 쿠키가 없는가?
   → 최초 로그인(`POST /api/v1/auth/token`)이 `refreshToken`을 JSON 바디로만 반환하고, HttpOnly 쿠키로 설정하지 않기 때문
4. 왜 쿠키를 설정하지 않는가?
   → `AuthTokenController.exchangeToken()`이 `HttpServletResponse` 파라미터를 갖지 않아 `CookieUtils.addRefreshTokenCookie()`를 호출할 수 없음
5. 왜 이 불일치가 발생했는가?
   → `TokenController.reissue()`는 쿠키 기반으로 설계되었지만, `AuthTokenController.exchangeToken()`은 JSON 응답 기반으로 구현되어 두 엔드포인트 간 설계 불일치 발생

**핵심 코드 위치:**
- `AuthTokenController.java:77` — `ResponseEntity.ok(AuthTokenResponse.existingUser(...))` 쿠키 미설정
- `TokenController.java:65` — `@CookieValue(name = "refreshToken", required = false)` 쿠키 요구
- `CookieUtils.java:27` — `addRefreshTokenCookie()` 미사용 (path=/api/v1/auth)

#### 조치 내용
수정 필요: `AuthTokenController.exchangeToken()`에 `HttpServletResponse` 파라미터 추가 후
기존 회원 응답 시 `CookieUtils.addRefreshTokenCookie(response, refreshToken, cookieProperties)` 호출

#### 재발 방지
- 프로토콜 갱신: N
- ADR 작성    : N (단순 구현 누락)
- 관련 SC     : 신규 SC 등록 필요 (authCode 교환 시 refreshToken 쿠키 설정)

#### E2E 추가 진단 (2026-05-31 21:30)
- BE 완전 정상: Naver 콜백 → authCode → 토큰 교환 전 구간 성공 (Railway 로그 13:16 확인)
- refreshToken 쿠키 설정 확인: COOKIE_SECURE=true, COOKIE_SAME_SITE=None (Railway 환경변수)
- FE 홈 초기화 패턴: `GET /api/v1/contents` 만 호출, `/api/v1/auth/reissue` 미호출
- 잔여 문제: FE 앱 초기화 시 reissue 호출 없음 → 새로고침 시 로그인 상태 초기화
- 조치 주체: FE 팀 — 앱 초기화 시 Zustand.accessToken === null 이면 POST /api/v1/auth/reissue 호출 필요

#### 해결 시각
2026-05-31 21:49 (BE 수정 완료) / FE 수정 대기 중

---

**[DIAGNOSE] 2026-05-31**
상태: ✅ 해결 완료

#### 오류 개요
- 발생 환경  : 로컬 (캐싱 전수 점검)
- 서비스     : HomeService / NotionSyncService
- 오류 유형  : Redis 직렬화 / Redis fallback 미적용
- 오류 메시지:
```
P1: HomeService — RedisTemplate<String, Object> + GenericJackson2JsonRedisSerializer 사용
    activateDefaultTyping으로 타입 정보 포함 → 클래스 구조 변경 시 역직렬화 오류 발생 가능
P2: NotionSyncService — stringRedisTemplate.opsForValue().get() 호출 시 try-catch 없음
    Redis 연결 오류 시 예외 전파 → 콘텐츠 목록 API 전체 실패 가능
```

#### 진단 과정
- STEP 2 서비스 상태: 정상
- STEP 3 로그 분석  : self-diagnose.md CACHE-1 기준 캐싱 서비스 4개 전수 점검
- STEP 4 Redis URL : 해당 없음 (로컬 점검)
- STEP 5 Redis 조회: 해당 없음

#### 근본 원인
HomeService가 RedisTemplate\<String, Object\>에 GenericJackson2JsonRedisSerializer + activateDefaultTyping(NON_FINAL)을 사용하여 타입 정보를 JSON에 포함. 클래스 리팩토링 시 역직렬화 실패 위험. (ADR-003 패턴 위반)

#### 조치 내용
- HomeService: StringRedisTemplate + ObjectMapper(JavaTimeModule) 수동 직렬화로 전환
- HomeCacheEvictService: StringRedisTemplate으로 교체
- RedisConfig: GenericJackson2JsonRedisSerializer Bean 제거 (StringRedisTemplate 자동 구성)
- HomeServiceTest / HomeCacheEvictServiceTest: 목 타입 수정

#### 재발 방지
- 프로토콜 갱신: Y → self-diagnose.md (CACHE-1~5 추가), sc-structure-check.md (Redis 캐싱 구조 점검), CLAUDE.md (Redis 캐싱 금지 규칙)
- ADR 작성    : Y → ADR-003-redis-serialization-strategy.md
- 관련 SC     : HARNESS-5 (self-diagnose 캐싱 점검 추가)

#### 해결 시각
2026-05-31

---

**[ADR] 2026-05-31**
상태: ✅ 완료

#### ADR 정보
- 파일명   : docs/adr/ADR-003-redis-serialization-strategy.md
- 트리거   : 기술 의사결정 (GenericJackson2JsonRedisSerializer 역직렬화 오류 위험)
- 핵심 결정:
    @Cacheable + GenericJackson2JsonRedisSerializer → StringRedisTemplate 수동 캐싱으로 전환 (HomeService, NotionSyncService 모두 적용)

#### 프로토콜 갱신 항목
- self-diagnose.md → CACHE-1~5 캐싱 직렬화 점검 섹션 추가 (STEP 5로 신규 등록)
- sc-structure-check.md → Redis 캐싱 구조 점검 항목 추가
- CLAUDE.md → Redis 캐싱 금지 규칙 추가 (GenericJackson2JsonRedisSerializer / TTL 미설정 / 키 임의 작성 금지)

#### 등록된 백로그 SC
- HARNESS-5: self-diagnose.md 캐싱 직렬화 점검 추가

#### 완료 시각
2026-05-31

---

**[AUDIT] 2026-05-31**
상태: ✅ 완료

#### 감사 요약
| 구분     | 건수 |
|---------|-----|
| Critical | 2개 |
| High     | 1개 |
| Medium   | 3개 |
| Low      | 1개 |
| 정상     | —  |

#### 주요 발견 항목
- ❌ harness-audit.md 누락 (Critical) → 신규 생성 완료
- ❌ adr-create.md 누락 (Critical) → 신규 생성 완료
- 🟠 JaCoCo 커버리지 임계값 집계 기준 문제 (High) → Rule 3/4 추가 완료
- ⚠️ Controller 테스트 21% (6/28) (Medium) → P1/P2 완료, P3 다음 스프린트 예정
- ⚠️ self-diagnose.md 캐싱 점검 항목 없음 (Medium) → CACHE-1~5 추가 완료
- ⚠️ log-diagnose.md Railway Redis 진단 절차 미흡 (Medium) → STEP 1~7 추가 완료
- ℹ️ debug-log.md 미존재 (Low) → 신규 생성 완료

#### 등록된 보완 백로그 SC
- HARNESS-1: harness-audit.md 신규 생성
- HARNESS-2: adr-create.md 신규 생성
- HARNESS-3: JaCoCo 커버리지 임계값 개선
- HARNESS-4: Controller 테스트 커버리지 강제화
- HARNESS-5: self-diagnose.md 캐싱 직렬화 점검 추가
- HARNESS-6: log-diagnose.md Railway Redis 진단 절차 보완
- HARNESS-7: debug-log.md 신규 생성

#### 다음 감사 예정
2026-06-30 (다음 스프린트 종료 시)

#### 완료 시각
2026-05-31

---

## 참고 — Controller 테스트 커버리지 분류 (2025-05-30)

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
