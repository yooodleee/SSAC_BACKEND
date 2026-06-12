# E2E 브라우저 진단 프로토콜 (BE)

## 역할
Playwright로 실제 브라우저 환경에서 운영 서버를 진단하고 인증/쿠키/CORS 오류 원인을 추론한다.  
트리거: 인증 오류(401/403) 반복 / Cookie·CORS·HTTPS 의심 증상 / `log-diagnose.md`로 원인 파악 불가 시

## 실행 순서
STEP 1(환경 준비) → 2(시나리오 실행) → 3(데이터 수집) → 4(AI 원인 추론) → 5(결과 기록)

---

## STEP 1. Playwright 환경 준비

```bash
npx playwright --version          # 설치 확인
npm install -D @playwright/test   # 미설치 시
npx playwright install chromium
```

진단 스크립트 위치: `scripts/e2e-diagnose/`
- `runner.ts` — 진입점
- `scenarios/` — auth.ts / api.ts / storage.ts
- `collectors/` — network.ts / console.ts / cookie.ts / storage.ts / railway.ts
- `output/` — 수집 결과 저장 (gitignore)

```bash
# 실행 (환경 변수 설정 후)
E2E_TARGET_URL=https://ssac.io E2E_API_URL=https://api.ssac.io E2E_ADMIN_CODE=secret \
npx ts-node scripts/e2e-diagnose/runner.ts
```

---

## STEP 2. 진단 시나리오

| 시나리오 | 검증 항목 |
|---------|---------|
| A. 로그인 직후 | Cookie 저장 여부 / Set-Cookie 헤더 / `/api/v1/users/me` 응답 |
| B. Token 재발급 | Refresh Token → `/api/v1/auth/reissue` → 새 Access Token 흐름 |
| C. 인증 API 호출 | 로그인 후 인증 필요 API 직접 호출 및 응답 코드 |
| D. 온보딩 플로우 | 비로그인 온보딩 → 로그인 → 자동 제출 흐름 |

---

## STEP 3. 데이터 수집

| 수집 항목 | 수집 방법 | 주의 사항 |
|---------|---------|---------|
| Cookie 목록 | `context.cookies()` | 토큰 값 [MASKED] 처리 |
| LocalStorage / SessionStorage | `page.evaluate()` | 토큰 키 [MASKED] 처리 |
| Network Trace | `page.on('request/response')` | 인증 관련만 필터링 |
| Console Log | `page.on('console')` | ERROR/WARN만 필터링 |
| Railway Log | `railway logs --tail 50` | CLI 미연결 시 skip |
| Screenshot | `page.screenshot()` | `output/screenshot.png` 저장 |

수집 결과: `output/network.har` / `output/result.json` / `output/screenshot.png`

---

## STEP 4. AI 원인 추론

수집 데이터를 아래 형식으로 정리 후 추론한다:

```
=== E2E 진단 결과 ===
Cookie  : refresh_token {존재/없음} / domain={값} / secure={t/f} / httpOnly={t/f} / sameSite={값}
API     : GET /api/v1/auth/status → {status} / GET /api/v1/users/me → {status}
Set-Cookie: {존재/없음}
Console : ERROR {N}건 / {대표 메시지}
Railway : ERROR {N}건 / {대표 메시지}
Storage : onboarding_answers {존재/없음}
```

### 증상별 원인 추론 기준

| 증상 | 원인 후보 | 확인 포인트 |
|------|---------|-----------|
| Set-Cookie 있으나 저장 안 됨 | SameSite=None 누락 (Chrome 80+ 기본 Lax) | Cookie sameSite 필드 |
| ^ | Secure=true인데 HTTP 접속 | 접속 URL scheme |
| ^ | 도메인 불일치 (api.ssac.io → ssac.io) | Cookie domain 필드 |
| ^ | FE `withCredentials: true` 미설정 | Network 요청 Cookie 헤더 포함 여부 |
| Cookie 존재하나 401 반복 | Access Token 메모리 초기화 (새로고침) | auth/status 재호출 여부 |
| ^ | Reissue Interceptor(AUTH-011) 미호출 | Console 재발급 시도 로그 |
| ^ | CORS preflight 실패로 본 요청 차단 | Network OPTIONS 405/403 여부 |
| Console `CORS error` | `Access-Control-Allow-Origin` 미설정 | Railway `CORS_ALLOWED_ORIGINS` 환경 변수 |
| ^ | wildcard(`*`) + `withCredentials` 충돌 | 응답 헤더 `Allow-Origin` 값 |
| ^ | 허용 메서드/헤더 누락 (PATCH·DELETE 등) | `Access-Control-Allow-Methods` 값 |

---

## STEP 5. 결과 기록

진단 완료 후 `docs/debug-log.md`에 [DIAGNOSE] 양식으로 기록한다:

```
## [DIAGNOSE] YYYY-MM-DD — E2E 브라우저 진단

### 오류 개요
- 환경: Railway 운영 / 유형: 인증/Cookie/CORS / 증상: {핵심 증상}

### 진단 결과
- 시나리오: {A/B/C/D} / Cookie: refresh_token {존재/없음} sameSite={값}
- Set-Cookie: {존재/없음} / /users/me: {status}
- Console: {N}건 / Railway: {N}건

### AI 추론 (패턴: {증상명})
{추론된 원인 및 근거}

### 조치 내용 / 재발 방지
{수행 조치} / ADR: {번호 또는 N} / 관련 SC: {번호}
```
