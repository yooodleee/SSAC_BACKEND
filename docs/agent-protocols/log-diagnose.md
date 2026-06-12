# BE Log Diagnosis Protocol

## STEP 1. 로그 수집

| 상황 | 환경 | 수집 방법 |
|------|------|---------|
| 컴파일 / 테스트 실패 | 공통 | `Read: logs/agent-capture.log` (Hook 자동 저장) |
| 런타임 오류 | Railway(prod) | `railway logs --service ssac-backend --tail 100` |
| 런타임 오류 | local/dev | `Read: logs/app.log` |

> **Railway 주의:** `prod` 프로파일은 FILE Appender 없음. `logs/app.log` 미존재 — 반드시 `railway logs` 사용.

`traceId` 확인 시 → `GET /api/v1/admin/logs/errors?traceId={traceId}` (서버 실행 중일 때만, ADMIN 권한 필요)

서버 상태 확인 → `GET /actuator/health`
```
{"status":"UP","components":{"db":{"status":"UP"},"redis":{"status":"UP"},"externalApi":{"status":"UP"}}}
```

---

## STEP 2. 오류 유형 분류

### 런타임 오류 패턴

| 패턴 | 원인 | 확인 위치 |
|------|------|---------|
| `JPA EntityNotFoundException` | 존재하지 않는 ID | Repository 쿼리 |
| `DataIntegrityViolationException` | 중복 삽입 | 유니크 제약 조건 |
| `JwtException` | 토큰 파싱 실패 | JWT 시크릿 키 / 형식 |
| `HikariCP timeout` | DB 커넥션 풀 고갈 | 커넥션 풀 설정 |
| `RedisConnectionException` | Redis 연결 실패 | Redis 서버 상태 |
| `SerializationException` / `ClassCastException` | Redis 직렬화 오류 | ADR-003 / CACHE-1 |
| `HttpClientErrorException` | 외부 API 호출 실패 | 카카오 / 네이버 API 상태 |
| `FlywayException` | 마이그레이션 실패 | `V*.sql` 문법 |
| `BeanCreationException` | Spring 컨텍스트 오류 | 환경 변수 / 설정 누락 |

### Railway 배포 실패 패턴

| 로그 패턴 | 원인 | 확인 위치 |
|---------|------|---------|
| `Could not resolve placeholder` | 환경 변수 누락 | Railway Variables 탭 |
| `BUILD FAILURE` | Gradle 빌드 오류 | `build.gradle` / Checkstyle |
| `FlywayException` / `SchemaManagementException` | DB 마이그레이션 실패 | `V*.sql` |
| `Connection refused` | DB / Redis 연결 실패 | Railway Plugin 연결 상태 |
| `OutOfMemoryError` | 메모리 부족 | JVM 힙 설정 |
| `Health check failed` | 헬스체크 타임아웃 | `healthcheckTimeout` 설정 |
| `UnusedImports` / `Checkstyle` | Checkstyle 규칙 위반 | 해당 Java 파일 |

---

## STEP 3. 원인 검증

- [ ] ErrorCode가 `GlobalExceptionHandler`에 등록되어 있는가
- [ ] 동일 `traceId` 로그 흐름이 일관성 있는가
- [ ] DB / Redis 연결 정상 (`/actuator/health`)
- [ ] 외부 API(카카오/네이버) 정상 (`/actuator/health → components.externalApi`)

---

## STEP 4. 수정 및 재진단

- [ ] 원인 확정 후에만 코드 수정 (추측 기반 수정 금지)
- [ ] 수정 후 관련 테스트 통과 확인
- [ ] FE 연관 오류 시 `X-Trace-Id` FE 팀에 공유

---

## STEP 5. 결과 기록

→ `docs/debug-log.md`에 [DIAGNOSE] 양식으로 기록  
→ 반복 오류(3회 이상) 시 `adr-create.md` 실행

---

## Railway 운영 환경 추가 진단

Railway 배포 실패 또는 운영 오류 발생 시 STEP 1~5에 추가하여 실행한다.

### 환경 확인
```bash
railway whoami    # 로그인 확인
railway status    # 프로젝트 연결 확인
```

### 로그 수집
```bash
railway logs --service ssac-backend --tail 100
railway logs --service ssac-backend --deployment 2>&1 | grep -i "error\|failed\|exception" | head -20
```

### Redis 직접 조회 (RedisConnectionException 발생 시)
```bash
railway variables --service Redis          # → REDIS_PUBLIC_URL 확인
redis-cli -h {HOST} -p {PORT} -a {PASSWORD}
> PING                                     # → PONG 확인
> KEYS contents:*                          # → 캐시 키 목록
> TTL "contents:v4:list:null:null:null"    # → -1이면 TTL 미설정(이상)
```

Redis 오류 조치 기준:
- `RedisConnectionException` → Railway Variables `SPRING_REDIS_*` 재확인 및 Redis Plugin 상태 확인
- `SerializationException` → `self-diagnose.md` CACHE-1 점검 / ADR-003 참고 → `StringRedisTemplate`으로 전환
- TTL = -1 → TTL 미설정 코드 수정 후 재배포
