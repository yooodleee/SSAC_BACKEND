# BE Log Diagnosis Protocol

## STEP 1. 로그 수집 (상황별 분기)

오류 상황에 따라 아래 순서대로 로그를 수집한다. **HTTP API는 서버가 실행 중일 때만** 사용 가능하다.

### 상황별 수집 방법

| 상황 | 1순위 수집 방법 | 2순위 |
|------|--------------|------|
| 컴파일 오류 | `logs/agent-capture.log` 읽기 (Hook 자동 저장) | Bash 출력 직접 참조 |
| 테스트 실패 | `logs/agent-capture.log` 읽기 (Hook 자동 저장) | Bash 출력에서 FAILED 블록 추출 |
| 런타임 오류 (서버 실행 중) | `logs/app.log` 읽기 (파일 Appender 자동 저장) | HTTP API 조회 |
| 런타임 오류 (서버 미실행) | `logs/app.log` 읽기 (마지막 실행 로그) | — |

### 1-A. Bash 오류 — `logs/agent-capture.log` 읽기

PostToolUse Hook이 Bash 명령 실패 시 자동으로 저장한다. 오류 발생 직후 다음 파일을 읽는다:

```
Read: logs/agent-capture.log
```

파일 형식:
```
[2024-01-01 12:00:00]
Command: ./gradlew test
---
(전체 stdout/stderr 출력)
```

### 1-B. 런타임 오류 — `logs/app.log` 읽기

서버 실행 중 발생한 WARN/ERROR는 파일 Appender가 자동 저장한다:

```
Read: logs/app.log
```

로그 형식:
```
2024-01-01 12:00:00.000 ERROR [traceId=abc-123] [userId=user-456] [GET /api/news] c.s.SomeClass - 메시지
```

`traceId`가 있으면 추출하여 STEP 1-C로 이동한다.

### 1-C. traceId 기반 HTTP API 조회 (서버 실행 중일 때만)

```
GET /api/v1/admin/logs/errors?traceId={traceId}
Authorization: ADMIN 권한 필요
```

응답 구조:
```json
{
  "traceId": "abc-123",
  "logs": [
    {
      "timestamp": "datetime",
      "level": "ERROR",
      "message": "string",
      "stackTrace": "string | null"
    }
  ]
}
```

### 1-D. 서버 상태 확인 (런타임 오류 의심 시)

```
GET /actuator/health
```

기대 응답:
```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "redis": { "status": "UP" },
    "externalApi": { "status": "UP", "details": { "kakao": "UP", "naver": "UP" } }
  }
}
```

---

## STEP 2. 에러 유형 분류

| 패턴 | 가설 원인 | 확인 위치 |
|------|---------|---------|
| `JPA EntityNotFoundException` | 존재하지 않는 ID 조회 | Repository 쿼리 |
| `DataIntegrityViolationException` | 중복 데이터 삽입 | 유니크 제약 조건 |
| `JwtException` | 토큰 파싱 실패 | JWT 시크릿 키 / 형식 |
| `HikariCP timeout` | DB 커넥션 풀 고갈 | 커넥션 풀 설정 |
| `RedisConnectionException` | Redis 연결 실패 | Redis 서버 상태 |
| `HttpClientErrorException` | 외부 API 호출 실패 | 카카오/네이버 API 상태 |

### 에러 레벨별 로그 정책

| HTTP 상태 | 레벨 | 로그 포함 항목 |
|----------|------|------------|
| 400 Bad Request | WARN | ErrorCode, 요청 경로, traceId |
| 401 Unauthorized | WARN | ErrorCode, 요청 경로, traceId |
| 403 Forbidden | WARN | ErrorCode, 요청 경로, traceId |
| 404 Not Found | WARN | ErrorCode, 요청 경로, traceId |
| 409 Conflict | WARN | ErrorCode, 요청 경로, traceId |
| 500 Server Error | ERROR | 스택 트레이스 포함 |

WARN 로그 형식 예시:
```
WARN [AUTH-002] GET /api/news | traceId=abc-123 | userId=user-456
-> 토큰이 만료되었습니다.
```

---

## STEP 3. 원인 검증

- [ ] 로그의 ErrorCode가 `GlobalExceptionHandler`에 등록되어 있는가
- [ ] 동일한 `traceId`의 로그 흐름이 일관성 있는가
- [ ] DB / Redis 연결 상태가 정상인가 (`/actuator/health`)
- [ ] 외부 API (카카오/네이버) 상태가 정상인가 (`/actuator/health` → `components.externalApi`)

### /actuator/health 기대 응답 구조

```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "redis": { "status": "UP" },
    "externalApi": {
      "status": "UP",
      "details": {
        "kakao": "UP",
        "naver": "UP"
      }
    }
  }
}
```

---

## STEP 4. 수정 및 재진단

- [ ] 원인 확정 후에만 코드 수정
- [ ] 수정 후 관련 테스트 통과 확인
- [ ] `X-Trace-Id`를 FE 팀에 공유 (FE 연관 오류인 경우)

---

## STEP 5. 진단 결과 기록

→ `docs/debug-log.md`에 기록  
→ 반복 오류 시 `docs/quality.md` 기술 부채 항목 추가

---

## CORS 오류 진단

허용되지 않은 Origin 접근 시 로그 항목:
- 요청 Origin
- 허용된 Origin 목록
- 거부된 헤더 목록

응답 구조:
```json
{
  "status": 403,
  "code": "CORS-001",
  "message": "허용되지 않은 Origin입니다.",
  "requestedOrigin": "http://localhost:3000"
}
```

---

## 로그 보존 정책

| 레벨 | 보존 기간 | 삭제 방식 |
|------|---------|---------|
| WARN | 7일 | 매일 자정 배치 자동 삭제 |
| ERROR | 30일 | 매일 자정 배치 자동 삭제 |

배치: `ErrorLogBatchService.purgeExpiredLogs()` — `@Async`로 서비스 응답에 영향 없음
