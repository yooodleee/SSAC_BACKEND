# ADR 007 — 환경별 프로파일 구조 및 운영 환경 설정 분리

> 작성일: 2026-05-02  
> 상태: 채택(Accepted)

---

## 맥락

운영 배포 시 개발·테스트 환경 설정(SQL 로그, 스택 트레이스 노출 등)이 함께 배포될 위험이 있었다.  
환경별 책임 범위를 명확히 분리하고 민감 값이 코드에 하드코딩되지 않도록 보장하는 구조가 필요했다.

---

## 프로파일 파일 구조

```
src/main/resources/
├── application.properties        # 공통: 모든 환경에서 항상 로드 (Swagger, JWT 키 형식, OAuth URI 등)
├── application-local.properties  # 로컬 개발: MySQL localhost, 비밀번호 직접 설정
├── application-test.properties   # CI / 단위 테스트: H2 인메모리 DB, 더미 값
├── application-docker.properties # Docker Compose: 환경 변수로 DB/Redis 연결
└── application-prod.yml          # 운영: 환경 변수 전용, 보안 강화 설정
```

> `application-prod.yml`은 YAML 형식으로 작성되어 계층적 설정 표현이 명확하다.  
> Spring Boot는 `.properties`와 `.yml`을 동일 프로파일에서 함께 로드할 수 있다.

---

## 프로파일 활성화 방법

| 환경 | 활성화 방법 | 로드되는 파일 |
|---|---|---|
| 로컬 개발 | `SPRING_PROFILES_ACTIVE=local` (bootRun 기본값) | application.properties + application-local.properties |
| CI 테스트 | `SPRING_PROFILES_ACTIVE=test` | application.properties + application-test.properties |
| Docker 개발 | `SPRING_PROFILES_ACTIVE=docker` | application.properties + application-docker.properties |
| 운영 | `SPRING_PROFILES_ACTIVE=prod` | application.properties + application-prod.yml |

---

## test vs prod 설정 비교

| 항목 | test | prod | 이유 |
|---|---|---|---|
| `spring.jpa.show-sql` | `true` | `false` | 운영 SQL 로그 노출 방지 |
| `spring.jpa.hibernate.ddl-auto` | `create-drop` | `validate` | 운영 스키마 자동 변경 금지 |
| `logging.level.root` | `WARN` | `WARN` | — |
| `logging.level.com.ssac` | `DEBUG` | `INFO` | 운영 로그 최소화 |
| `spring.datasource.hikari.maximum-pool-size` | `5` | `20` | 운영 트래픽 대응 |
| `management.endpoints.web.exposure.include` | `*` | `health, info` | 운영 공격 면 최소화 |
| `management.endpoint.health.show-details` | `always` | `never` | DB/Redis 정보 외부 노출 방지 |
| `server.error.include-stacktrace` | `always` | `never` | 운영 스택 트레이스 외부 노출 금지 |
| `server.error.include-message` | `always` | `never` | 운영 에러 메시지 외부 노출 금지 |

---

## 민감 값 관리 원칙

### 금지 패턴
```yaml
# application-prod.yml에 절대 하드코딩 금지
spring:
  datasource:
    password: actual_password_here   # ❌ 하드코딩 금지
```

### 권장 패턴
```yaml
# 환경 변수 참조 — 기본값 없이 선언하여 누락 시 즉시 기동 실패
spring:
  datasource:
    password: ${DB_PASSWORD}         # ✅ 누락 시 "Could not resolve placeholder" 오류
```

> `${DB_PASSWORD:}` (빈 기본값)와 `${DB_PASSWORD}` (기본값 없음)의 차이:  
> - `${DB_PASSWORD:}` → 빈 문자열로 대체되어 기동은 성공하지만 DB 연결 실패  
> - `${DB_PASSWORD}` → Spring 시작 시 즉시 예외 발생 (권장)

---

## prod 프로파일 필수 환경 변수 목록

```
DB_HOST          - MySQL 호스트
DB_PORT          - MySQL 포트 (기본: 3306)
DB_NAME          - 데이터베이스 이름
DB_USERNAME      - DB 사용자명
DB_PASSWORD      - DB 비밀번호 ← 누락 시 기동 실패

JWT_SECRET       - JWT HMAC-SHA256 시크릿 (32바이트 이상)

REDIS_HOST       - Redis 호스트
REDIS_PORT       - Redis 포트

KAKAO_CLIENT_ID       - Kakao OAuth 앱 키
KAKAO_CLIENT_SECRET   - Kakao OAuth 앱 시크릿
KAKAO_REDIRECT_URI    - Kakao OAuth 리디렉션 URI

NAVER_CLIENT_ID       - Naver OAuth 앱 키
NAVER_CLIENT_SECRET   - Naver OAuth 앱 시크릿
NAVER_REDIRECT_URI    - Naver OAuth 리디렉션 URI

CORS_ALLOWED_ORIGINS     - 허용된 프론트엔드 Origin
OAUTH2_DEFAULT_REDIRECT_URI - OAuth 성공 후 리디렉션 URI
```

---

## 운영 기동 시 자동 검증 항목

Spring Boot는 `prod` 프로파일로 기동 시 아래를 자동 검증한다:

| 검증 항목 | 오류 유형 | 예시 오류 메시지 |
|---|---|---|
| 환경 변수 누락 | `IllegalArgumentException` | `Could not resolve placeholder 'DB_PASSWORD'` |
| DB 스키마 불일치 | `SchemaManagementException` | `Schema-validation: missing column [xxx] in table [xxx]` |
| DB 연결 실패 | `DataSourceLookupFailureException` | `Failed to obtain JDBC Connection` |
| Redis 연결 실패 | 헬스체크 `DOWN` | `/actuator/health` → `{"status":"DOWN","components":{"redis":{"status":"DOWN"}}}` |

---

## CI 검증 게이트

`profile-validation` 잡이 모든 PR에서 아래 4개 항목을 검증한다:

1. **test 프로파일 기동 + `/actuator/health` 200 응답** — 설정 변경이 서버 기동을 깨뜨리지 않음 보장
2. **`application-prod.yml` 파일 존재 여부** — prod 설정 파일 삭제 방지
3. **하드코딩 시크릿 탐지** — `password:`, `secret:`, `client-secret:` 키의 리터럴 값 탐지
4. **필수 보안 설정 확인** — `ddl-auto: validate`, `include-stacktrace: never`, `show-sql: false`

---

## HikariCP 운영 설정 근거

| 파라미터 | 값 | 근거 |
|---|---|---|
| `maximum-pool-size` | 20 | MySQL 기본 최대 연결(151) 대비 충분한 여유, 단일 앱 인스턴스 기준 |
| `minimum-idle` | 5 | 트래픽 없을 때 최소 연결 유지로 첫 요청 지연 방지 |
| `connection-timeout` | 30,000ms | 풀 고갈 시 30초 내 응답 보장 |
| `idle-timeout` | 600,000ms | MySQL `wait_timeout`(28,800초)보다 충분히 짧게 설정 |
| `max-lifetime` | 1,800,000ms | 커넥션 재활용으로 MySQL 연결 제한 문제 예방 |
| `leak-detection-threshold` | 60,000ms | 1분 이상 반납 안 된 커넥션 경고 로그 출력 |
