# E2E 브라우저 진단 프로토콜 (BE)

## 역할
Playwright를 통해 실제 브라우저 환경에서
운영 서버를 진단하고 수집된 데이터를
AI에게 넘겨 원인을 추론한다.

## 트리거 조건
- 운영 환경에서 인증 / 쿠키 관련 오류 발생
- API 401 / 403 응답이 반복될 때
- Cookie / CORS / HTTPS 관련 의심 증상 발생 시
- log-diagnose.md만으로 원인 파악이 불가할 때
- 사용자가 "브라우저 진단", "e2e 진단" 언급 시

## 실행 순서
STEP 1. Playwright 환경 준비
STEP 2. 진단 시나리오 실행
STEP 3. 데이터 수집
STEP 4. AI 원인 추론
STEP 5. 결과 기록

---

## STEP 1. Playwright 환경 준비

### 의존성 설치 확인
```bash
# Playwright 설치 확인
npx playwright --version

# 미설치 시
npm install -D @playwright/test
npx playwright install chromium
```

### 진단 스크립트 위치
```
scripts/e2e-diagnose/
├── runner.ts          ← 진단 실행 진입점
├── scenarios/
│   ├── auth.ts        ← 인증 / 쿠키 시나리오
│   ├── api.ts         ← API 호출 시나리오
│   └── storage.ts     ← 스토리지 시나리오
├── collectors/
│   ├── index.ts       ← 전체 데이터 수집 진입점
│   ├── network.ts     ← Network Trace 수집
│   ├── console.ts     ← Console Log 수집
│   ├── cookie.ts      ← Cookie 수집
│   ├── storage.ts     ← LocalStorage / SessionStorage 수집
│   └── railway.ts     ← Railway Log 수집
└── output/            ← 수집 결과 저장 (gitignore)
```

### 실행 명령어
```bash
# 인증 시나리오
npm run e2e:diagnose:auth

# 환경 변수 설정 후 실행
E2E_TARGET_URL=https://ssac.io \
E2E_API_URL=https://api.ssac.io \
E2E_ADMIN_CODE=secret \
npx ts-node scripts/e2e-diagnose/runner.ts
```

---

## STEP 2. 진단 시나리오 실행

아래 핵심 시나리오를 순서대로 실행한다:

### 시나리오 A: 로그인 직후 상태 수집
가장 많은 인증 오류가 발생하는 구간.
Cookie 저장 여부, Set-Cookie 헤더, 로그인 직후 /api/v1/users/me 응답을 수집한다.

### 시나리오 B: Access Token 재발급 흐름
Refresh Token → /api/v1/auth/reissue → 새 Access Token 흐름 검증.

### 시나리오 C: 인증 필요 API 호출
로그인 후 /api/v1/users/me 등 인증 필요 API 직접 호출 및 응답 코드 수집.

### 시나리오 D: 온보딩 플로우
비로그인 온보딩 → 로그인 → 자동 제출 흐름 검증.

---

## STEP 3. 데이터 수집

수집 대상:

| 수집 항목 | 수집 방법 | 주의 사항 |
|---------|---------|---------|
| Cookie 목록 | `context.cookies()` | 토큰 값은 [MASKED] 처리 |
| LocalStorage | `page.evaluate()` | 토큰 키는 [MASKED] 처리 |
| SessionStorage | `page.evaluate()` | 토큰 키는 [MASKED] 처리 |
| Network Trace | `page.on('request/response')` | 인증 관련만 필터링 |
| Console Log | `page.on('console')` | ERROR/WARN만 필터링 |
| Railway Log | `railway logs --tail 50` | CLI 미연결 시 skip |
| Screenshot | `page.screenshot()` | output/ 에 저장 |

수집 결과 저장 위치:
```
scripts/e2e-diagnose/output/
├── network.har       ← 전체 네트워크 트레이스 (HAR)
├── screenshot.png    ← 진단 시점 화면
└── result.json       ← 수집 데이터 JSON
```

---

## STEP 4. AI 원인 추론

수집된 데이터를 아래 형식으로 정리하여 AI에게 전달한다:

### 출력 형식

```
=== E2E 진단 결과 ===
시각: {timestamp}
대상: {url}

--- Cookie ---
refresh_token : {존재 / 없음}
  domain  : {domain}
  secure  : {true / false}
  httpOnly: {true / false}
  sameSite: {None / Lax / Strict / 없음}
access_token  : {존재 / 없음}
  (메모리 저장이므로 Cookie에 없어야 정상)

--- API 호출 결과 ---
GET /api/v1/auth/status : {status}
GET /api/v1/users/me   : {status}

--- Network Set-Cookie ---
{Set-Cookie 헤더 존재 여부 / 값}

--- Console Log (오류만) ---
{ERROR / WARN 레벨 로그}

--- Railway Log (오류만) ---
{ERROR 키워드 포함 로그}

--- SessionStorage ---
onboarding_answers: {존재 / 없음}
```

### AI 추론 항목
위 데이터를 기반으로 아래 항목을 추론한다:

```
□ Cookie 미저장 원인 분석
  → SameSite=None 누락?
  → Secure=true인데 HTTP 사용?
  → 도메인 불일치 (api.ssac.io → ssac.io)?
  → HttpOnly 설정 오류?

□ 401 원인 분석
  → Cookie 미전송?
  → Access Token 누락?
  → CORS preflight 실패?

□ CORS 오류 원인 분석
  → Access-Control-Allow-Origin 미설정?
  → withCredentials + wildcard 충돌?
  → 허용 도메인 목록 불일치?
```

---

## AI 추론 레퍼런스 패턴

### 패턴 1: Cookie 저장 안 됨
```
증상:
→ Set-Cookie 응답 존재
→ Cookie 실제 저장 없음
→ GET /api/v1/users/me → 401

원인 후보:
A. SameSite=None 누락
   → Chrome 80+ 기본값이 Lax
   → 크로스 도메인 쿠키는 SameSite=None 필수
   확인: Cookie sameSite 필드 값

B. Secure=true인데 HTTP 사용
   → SameSite=None은 Secure 필수
   → 로컬 HTTP 환경에서 발생
   확인: 현재 접속 URL이 http://인가?

C. 도메인 불일치
   → BE: api.ssac.io에서 Set-Cookie
   → FE: ssac.io에서 수신
   → domain 속성 미설정 시 서브도메인 공유 불가
   확인: Cookie domain 필드 값

D. withCredentials 미설정
   → FE Axios에 withCredentials: true 없음
   → 크로스 도메인 쿠키 미전송
   확인: Network 요청 헤더의 Cookie 포함 여부
```

### 패턴 2: 401 반복
```
증상:
→ Cookie refresh_token 존재
→ GET /api/v1/users/me → 401
→ Axios Interceptor 재발급 시도 없음

원인 후보:
A. Access Token 메모리 초기화
   → 새로고침 시 메모리 상태 초기화
   → /api/v1/auth/status 재호출 필요
   확인: 새로고침 후 auth/status 호출 여부

B. Reissue API 미호출
   → Interceptor AUTH-011 분기 누락
   확인: Console Log에서 재발급 시도 로그

C. CORS preflight 실패
   → OPTIONS 요청 실패로 본 요청 차단
   확인: Network에서 OPTIONS 405/403 여부
```

### 패턴 3: CORS 오류
```
증상:
→ Console: CORS error / blocked by CORS policy
→ Network: OPTIONS 실패

원인 후보:
A. Access-Control-Allow-Origin 미설정
   → BE CorsConfig에 ssac.io 미포함
   확인: Railway 환경 변수 CORS_ALLOWED_ORIGINS

B. withCredentials + wildcard 충돌
   → Access-Control-Allow-Origin: * 상태에서
     withCredentials: true 불가
   확인: 응답 헤더 Access-Control-Allow-Origin 값

C. 허용 메서드 / 헤더 누락
   → PATCH / DELETE 등 미허용
   확인: Access-Control-Allow-Methods 값
```

---

## STEP 5. 결과 기록

진단 완료 후 `docs/debug-log.md`에 아래 형식으로 [DIAGNOSE] 기록을 추가한다:

```
**[DIAGNOSE] YYYY-MM-DD HH:mm**
상태: 🔴 미해결 / ✅ 해결 완료

#### 오류 개요
- 발생 환경  : Railway 운영
- 서비스     : ssac-backend (E2E 브라우저 진단)
- 오류 유형  : 인증 / Cookie / CORS
- 오류 메시지: {핵심 증상}

#### 진단 과정
- 시나리오  : {auth / api / storage}
- Cookie    : refresh_token {존재/없음}, sameSite={값}
- Set-Cookie: {존재/없음}
- /users/me : {status}
- Console 오류: N건
- Railway 오류: N건

#### AI 추론 결과 (패턴 N)
{추론된 원인 및 근거}

#### 조치 내용
{수행한 조치}

#### 재발 방지
- 프로토콜 갱신: Y → {파일} / N
- ADR 작성    : Y → {ADR 번호} / N
- 관련 SC     : {SC 번호}

#### 스크린샷
scripts/e2e-diagnose/output/screenshot.png

#### 해결 시각
{YYYY-MM-DD HH:mm}
```
