# 관리자 토큰 갱신 운영 절차 (Runbook)

> 최종 수정: 2026-06-19
> 대상 환경: Railway 운영 (ssac-backend)

---

## 개요

관리자 인증은 **AdminCode 1회성 코드 → JWT AT/RT 발급** 흐름으로 동작한다.
일반 사용자 로그인과 달리 AdminCode는 발급·사용·만료 이력이 DB에 남는다.

```
[1] 기존 관리자 → POST /api/v1/admin/codes → rawCode 발급
[2] 신규/재로그인 → POST /api/v1/auth/admin/login (rawCode 사용) → AT + RT(쿠키)
[3] AT 만료 시   → POST /api/v1/auth/token/reissue (RT 쿠키 자동 전송) → 새 AT + 새 RT
[4] 긴급 종료    → DELETE /api/v1/auth/logout/all → 모든 세션 즉시 차단
```

---

## 시나리오 A — 정기 관리자 로그인 (최초 또는 세션 만료 후)

### STEP 1. AdminCode 발급 (기존 로그인 상태의 관리자가 수행)

**요청:**
```http
POST /api/v1/admin/codes
Authorization: Bearer {현재_AT}
Content-Type: application/json

{
  "adminUserId": {ADMIN_역할_사용자_ID},
  "expiresAt": "2026-06-20T23:59:59+09:00"   // 생략 시 무기한
}
```

**응답 (201):**
```json
{
  "codeId": "42",
  "rawCode": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "adminUserId": 1,
  "expiresAt": "2026-06-20T23:59:59+09:00",
  "createdAt": "2026-06-19T15:00:00+09:00"
}
```

**주의 사항:**
- `rawCode` 는 이 응답에서 **단 한 번만** 노출된다. 반드시 안전한 채널(Signal 등)로 전달한다.
- `adminUserId` 는 DB에서 `role = 'ADMIN'` 인 사용자 ID 여야 한다. USER/GUEST 이면 400 반환.
- `expiresAt` 은 KST(`+09:00`) 기준으로 입력하는 것을 권장한다. UTC 입력도 가능하며 서버가 KST로 변환한다.

---

### STEP 2. AdminCode로 로그인

**요청:**
```http
POST /api/v1/auth/admin/login
Content-Type: application/json

{
  "adminCode": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
}
```

**응답 (200):**
```json
{
  "accessToken": "eyJ...",
  "tokenType": "Bearer",
  "expiresIn": 3600000,
  "user": {
    "id": "1",
    "nickname": "관리자",
    "role": "ADMIN",
    "redirectTo": "/admin"
  }
}
```
- Refresh Token은 `HttpOnly; Secure; SameSite=None` 쿠키로 자동 설정된다.
- `adminCode` 는 로그인 성공 즉시 `used = true` 처리되어 재사용 불가.
- 코드 오류/만료/사용완료 모두 `401 ADMIN_CODE_INVALID` 반환 (구분 없음, 보안 설계).

---

### STEP 3. AT 만료 시 토큰 갱신

AT는 브라우저가 자동으로 갱신 요청을 보낸다. 수동으로 갱신이 필요할 경우:

**요청 (RT 쿠키 자동 첨부):**
```http
POST /api/v1/auth/token/reissue
```

**응답 (200):**
```json
{
  "accessToken": "eyJ...(새 AT)...",
  "refreshToken": "새_RT_원문"
}
```
- 헤더 `X-Reissued: true` 가 포함된다. FE는 이 헤더 수신 시 동시 재발급 루프를 방지해야 한다.
- RT가 원자적으로 교체된다. 동시 재발급 시 먼저 처리된 요청만 성공하고 나머지는 `400 TOKEN_INVALID`.

---

## 시나리오 B — AT 탈취 의심 / 긴급 세션 무효화

### STEP 1. 모든 세션 즉시 차단

```http
DELETE /api/v1/auth/logout/all
Authorization: Bearer {현재_AT}
```

**응답 (200):** 본문 없음

- `user.invalidatedBefore` 가 현재 시각(KST)으로 갱신된다.
- 이 시각 이전에 발급된 모든 AT가 다음 요청 시 `401` 처리된다.
- DB의 모든 RT 레코드가 삭제된다.

### STEP 2. AdminCode 재발급 및 재로그인

시나리오 A STEP 1→2 반복. 이전 AdminCode는 이미 `used = true` 이므로 사용 불가.

---

## 시나리오 C — 관리자 계정 신규 등록

현재 GUEST → ADMIN 역할 변경은 **기존 ADMIN이 API로 수행**한다.

```http
PATCH /api/v1/admin/users/{userId}/role
Authorization: Bearer {관리자_AT}
Content-Type: application/json

{
  "role": "ADMIN"
}
```

- `GUEST` 로의 변경은 서버에서 차단된다 (`400 ROLE_ASSIGNMENT_INVALID`).
- 역할 변경 후 해당 사용자에게 AdminCode를 발급하면 관리자 로그인이 가능하다.

---

## 시나리오 D — AdminCode 없이 관리자가 잠긴 경우 (비상 복구)

> Railway DB에 직접 접근 가능한 경우에만 수행한다.

1. Railway 대시보드 → 서비스 → Variables → `DATABASE_URL` 확인
2. DB 접속 후 수동으로 AdminCode 행 삽입:

```sql
-- rawCode = "emergency-recovery-code" 의 SHA-256 해시를 미리 계산한다
-- Python: import hashlib; print(hashlib.sha256(b"emergency-recovery-code").hexdigest())
INSERT INTO admin_codes (code_hash, admin_user_id, used, expires_at, created_at)
VALUES (
  '직접_계산한_SHA256_해시',
  {ADMIN_USER_ID},
  false,
  DATE_ADD(NOW(), INTERVAL 1 HOUR),  -- 1시간 후 만료
  NOW()
);
```

3. rawCode로 `POST /api/v1/auth/admin/login` 호출
4. 로그인 성공 후 즉시 해당 DB 행 삭제 또는 만료 확인

**주의:** 비상 복구 rawCode는 사용 직후 반드시 폐기하고 debug-log.md에 [DIAGNOSE] 기록을 남긴다.

---

## 토큰 유효 기간 참고

| 항목 | 값 | 비고 |
|------|-----|------|
| Access Token TTL | 1시간 (`expiresIn`) | `application.yml` `jwt.expiration-ms` |
| Refresh Token TTL | 7일 | `application.yml` `jwt.refresh-expiration-ms` |
| AdminCode 만료 | 발급 시 지정 (null=무기한) | KST 기준 |
| AdminCode 재사용 | 불가 (`used = true`) | 로그인 성공 즉시 소비 |

---

## 체크리스트 — 배포 후 관리자 인증 확인

```
□ 관리자 계정(role=ADMIN)이 DB에 존재하는지 확인
□ 유효한(used=false, 만료 전) AdminCode가 발급되어 있는지 확인
□ POST /api/v1/auth/admin/login 로그인 성공 확인
□ POST /api/v1/auth/token/reissue AT 갱신 성공 확인
□ X-Reissued 헤더가 응답에 포함되는지 확인
□ CORS exposedHeaders에 X-Reissued 포함 여부 확인
```

---

## 관련 파일

| 파일 | 역할 |
|------|------|
| `service/AdminLoginService.java` | 코드 검증 + 1회 소비 + 토큰 발급 |
| `service/AdminService.java` | AdminCode 생성 (`createAdminCode`) |
| `domain/auth/AdminCode.java` | `isExpired()` / `isUsed()` / `markAsUsed()` |
| `controller/AdminLoginController.java` | `POST /api/v1/auth/admin/login` |
| `controller/AdminController.java` | `POST /api/v1/admin/codes` |
| `docs/adr/ADR-001-refresh-token-rotation.md` | RT Rotation 정책 ADR |
