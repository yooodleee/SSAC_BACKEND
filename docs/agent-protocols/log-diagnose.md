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

## Railway 운영 환경 진단 절차

### 트리거 조건
- 운영 환경에서 오류 발생 시
- Railway 배포 실패 시
- Redis 관련 오류 발생 시
- CLAUDE.md "즉시 log-diagnose.md" 참조 시

### 실행 순서
STEP 1. 진단 환경 확인
STEP 2. Railway 서비스 상태 확인
STEP 3. 애플리케이션 로그 수집
STEP 4. Redis 공개 Proxy URL 확인
STEP 5. Redis 데이터 직접 조회
STEP 6. 오류 원인 분류 및 조치
STEP 7. 결과 기록

---

### STEP 1. 진단 환경 확인

Railway CLI가 설치되어 있는지 확인한다:
```bash
railway --version
# → Railway CLI 버전 출력 확인

# 미설치 시
npm install -g @railway/cli
```

Railway 로그인 상태를 확인한다:
```bash
railway whoami
# → 계정 이메일 출력 확인

# 미로그인 시
railway login
```

프로젝트 연결 상태를 확인한다:
```bash
railway status
# → 현재 연결된 프로젝트 / 환경 출력 확인

# 프로젝트 미연결 시
railway link
# → 프로젝트 선택 후 연결
```

---

### STEP 2. Railway 서비스 상태 확인

전체 서비스 목록과 상태를 확인한다:
```bash
railway status
```

서비스별 배포 상태를 확인한다:
```bash
# BE 서비스 배포 상태
railway logs --service ssac-backend --deployment

# Redis 서비스 상태
railway logs --service Redis
```

배포 실패 여부를 아래 키워드로 스캔한다:
```bash
railway logs --service ssac-backend --tail | \
    grep -E "ERROR|FATAL|Exception|BUILD FAILED"
```

Health Check 엔드포인트로 서비스 상태를 직접 확인한다:
```bash
curl https://api.ssac.io/actuator/health
# → {"status": "UP"} 확인

# DOWN 또는 연결 실패 시 STEP 3으로 진행
```

배포 실패 분류표:

| 로그 패턴 | 가설 원인 | 확인 위치 |
|---------|---------|---------|
| `Could not resolve placeholder` | 환경 변수 누락 | Railway Variables 탭 |
| `BUILD FAILURE` | Gradle 빌드 오류 | `build.gradle` 의존성 / Checkstyle |
| `FlywayException` / `SchemaManagementException` | DB 마이그레이션 실패 | `V*.sql` 파일 |
| `Connection refused` | DB/Redis 연결 실패 | Railway Plugin 연결 상태 |
| `Port already in use` | 포트 충돌 | `PORT` 환경 변수 |
| `OutOfMemoryError` | 메모리 부족 | JVM 힙 설정 |
| `Health check failed` | 헬스체크 타임아웃 | `healthcheckTimeout` 설정 |
| `UnusedImports` / `Checkstyle` | Checkstyle 규칙 위반 | 해당 Java 파일 미사용 import |

---

### STEP 3. 애플리케이션 로그 수집

실시간 로그를 수집한다:
```bash
# 최근 100줄 수집
railway logs --service ssac-backend --tail 100

# 특정 오류 키워드 필터링
railway logs --service ssac-backend --tail 200 | \
    grep -E "ERROR|WARN|Exception"

# Redis 관련 오류만 필터링
railway logs --service ssac-backend --tail 200 | \
    grep -i "redis\|cache\|serializ"

# 특정 시간대 로그 수집
railway logs --service ssac-backend --deployment

# 오류 로그만 필터링
railway logs --service ssac-backend --deployment 2>&1 | \
    grep -i "error\|failed\|exception\|caused by" | head -20

# 환경 변수 관련 오류만 필터링
railway logs --service ssac-backend --deployment 2>&1 | \
    grep -i "placeholder\|environment\|variable" | head -10
```

로그에서 아래 오류 패턴을 탐색한다:

```
오류 패턴                     → 의심 원인
─────────────────────────────────────────────────
RedisConnectionException      → Redis 연결 실패
SerializationException        → 직렬화 방식 오류
ClassCastException            → 타입 불일치
JedisConnectionException      → Jedis 연결 오류
io.lettuce.core.*             → Lettuce 연결 오류
FlywayException               → 마이그레이션 실패
BeanCreationException         → Spring 컨텍스트 오류
```

---

### STEP 4. Redis 공개 Proxy URL 확인

Railway의 Redis 공개 Proxy URL을 확인한다:
```bash
# Redis 서비스 환경 변수 전체 조회
railway variables --service Redis

# → 출력 예시:
# REDIS_URL=redis://default:password@host:port
# REDISHOST=containers-us-west-xxx.railway.app
# REDISPORT=6379
# REDISPASSWORD=xxxxxxxxxxx
# REDIS_PUBLIC_URL=redis://default:password@host:port  ← 이 값 사용
```

공개 Proxy URL에서 연결 정보를 추출한다:
```
# REDIS_PUBLIC_URL 형식 파싱
redis://default:{password}@{host}:{port}

# 예시
REDIS_PUBLIC_URL=redis://default:abc123@containers-us-west-123.railway.app:12345

→ HOST    : containers-us-west-123.railway.app
→ PORT    : 12345
→ PASSWORD: abc123
```

공개 Proxy URL이 없는 경우:
```
# Railway 대시보드에서 수동 활성화 필요
→ Railway 대시보드 접속
→ Redis 서비스 선택
→ Settings → Public Networking → Enable
→ 공개 URL 생성 확인
```

---

### STEP 5. Redis 데이터 직접 조회

STEP 4에서 확인한 공개 Proxy URL로 Redis에 직접 연결하여 데이터를 조회한다.

#### 방법 A: redis-cli 사용 (권장)
```bash
# redis-cli 설치 확인
redis-cli --version

# Redis 연결
redis-cli -h {HOST} -p {PORT} -a {PASSWORD}

# 연결 확인
127.0.0.1:PORT> PING
# → PONG 응답 확인

# 전체 키 목록 조회 (운영 환경 주의)
127.0.0.1:PORT> KEYS *
# → 저장된 캐시 키 목록 출력

# 특정 패턴 키 조회
127.0.0.1:PORT> KEYS contents:*
127.0.0.1:PORT> KEYS home:*

# 특정 키 값 조회
127.0.0.1:PORT> GET "contents:v4:list:null:null:null"

# 특정 키 TTL 확인 (초 단위)
127.0.0.1:PORT> TTL "contents:v4:list:null:null:null"
# → -1: TTL 없음 (무제한) ← 이상
# → -2: 키 없음
# → N : 남은 TTL(초)

# 특정 키 삭제 (캐시 초기화)
127.0.0.1:PORT> DEL "contents:v4:list:null:null:null"

# 패턴으로 일괄 삭제
127.0.0.1:PORT> KEYS contents:* | xargs redis-cli \
    -h {HOST} -p {PORT} -a {PASSWORD} DEL

# Redis 메모리 사용량 확인
127.0.0.1:PORT> INFO memory
# → used_memory_human 확인

# 연결된 클라이언트 수 확인
127.0.0.1:PORT> INFO clients
# → connected_clients 확인
```

#### 방법 B: Python redis 클라이언트 사용
```python
# redis 패키지 설치
# pip install redis

import redis
import json

# Redis 연결
r = redis.Redis(
    host='{HOST}',
    port={PORT},
    password='{PASSWORD}',
    decode_responses=True  # 문자열 자동 디코딩
)

# 연결 확인
print(r.ping())  # True 출력 확인

# 전체 키 목록 조회
keys = r.keys('*')
for key in keys:
    print(key)

# 특정 패턴 키 조회
content_keys = r.keys('contents:*')

# 특정 키 값 조회 및 JSON 파싱
value = r.get('contents:v4:list:null:null:null')
if value:
    data = json.loads(value)
    print(json.dumps(data, indent=2, ensure_ascii=False))

# 특정 키 TTL 확인
ttl = r.ttl('contents:v4:list:null:null:null')
print(f"TTL: {ttl}초")

# 캐시 무효화
r.delete('contents:v4:list:null:null:null')

# 패턴 키 일괄 삭제
keys_to_delete = r.keys('contents:*')
if keys_to_delete:
    r.delete(*keys_to_delete)
    print(f"{len(keys_to_delete)}개 키 삭제 완료")

# Redis 정보 조회
info = r.info()
print(f"메모리 사용량: {info['used_memory_human']}")
print(f"연결 클라이언트: {info['connected_clients']}")
```

#### Redis 조회 결과 해석 기준
```
정상:
→ PING → PONG
→ 캐시 키가 예상 패턴으로 존재
→ TTL이 설정값과 일치

이상:
→ 연결 거부   → Proxy URL / 방화벽 확인
→ 키가 없음   → 캐싱 로직 미동작 확인
→ TTL이 -1   → TTL 미설정 코드 확인 (CACHE-3)
→ 값 형식 이상 → 직렬화 방식 점검 (CACHE-1)
```

---

### STEP 6. 오류 원인 분류 및 조치

수집된 로그와 Redis 조회 결과를 아래 기준으로 분류하여 조치한다:

#### Redis 연결 오류
```
증상: RedisConnectionException / PONG 응답 없음
원인:
  → Railway Redis 서비스 중단
  → 환경 변수 SPRING_REDIS_* 설정 오류
  → 공개 Proxy URL 비활성화
조치:
  1. railway variables --service Redis 재확인
  2. Railway 대시보드 Redis 서비스 상태 확인
  3. 환경 변수 값과 실제 Redis URL 일치 여부 확인
```

#### 직렬화 오류
```
증상: SerializationException / ClassCastException
원인:
  → GenericJackson2JsonRedisSerializer 사용
  → 타입 정보 불일치
조치:
  1. self-diagnose.md CACHE-1 항목 점검
  2. StringRedisTemplate 수동 캐싱으로 전환
  3. ADR-003 참고
```

#### TTL 관련 오류
```
증상: 캐시가 무기한 유지 / 메모리 증가
원인: TTL 미설정
조치:
  1. Redis에서 해당 키 TTL 확인 (TTL 명령어)
  2. 코드에서 TTL 설정 누락 위치 파악
  3. CacheTtl 상수 적용 후 재배포
```

#### Flyway 마이그레이션 오류
```
증상: FlywayException / FAILED in schema_history
원인:
  → MySQL 미지원 문법 사용
  → 마이그레이션 버전 충돌
조치:
  1. docs/conventions/flyway.md 참고
  2. information_schema 조건부 패턴으로 수정
  3. 운영 DB schema_history 상태 확인
```

---

### STEP 7. 결과 기록

진단 완료 후 `docs/debug-log.md`에 아래 형식으로 결과를 기록한다:

```markdown
## [YYYY-MM-DD HH:mm] 운영 오류 진단

### 오류 개요
- 발생 환경 : Railway 운영
- 서비스    : ssac-backend / Redis
- 오류 유형 : {오류 분류}
- 오류 메시지: {핵심 오류 메시지}

### 진단 결과
- STEP 2 서비스 상태 : 정상 / 비정상
- STEP 3 로그 분석   : {핵심 발견 내용}
- STEP 4 Redis URL   : 확인 / 미확인
- STEP 5 Redis 조회  : {조회 결과 요약}

### 근본 원인
{5-Why 분석 결과}

### 조치 내용
{수행한 조치}

### 재발 방지
- 프로토콜 갱신 필요 여부: Y / N
- ADR 작성 필요 여부: Y / N
- 관련 SC: {백로그 SC 번호}

### 해결 완료 시각
{datetime}
```

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
