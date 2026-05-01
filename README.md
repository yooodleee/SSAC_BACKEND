# SSAC Backend

## 로컬 환경 실행 가이드

사전 요구사항만 충족되어 있다면 **10분 이내**에 로컬 환경을 실행할 수 있습니다.

---

### 사전 요구사항

| 도구 | 최소 버전 | 확인 명령어 |
|------|-----------|-------------|
| Docker | 24.0 이상 | `docker --version` |
| Docker Compose | 2.20 이상 | `docker compose version` |

---

### 1. 환경 변수 설정

`.env.example`을 복사하여 `.env` 파일을 생성하고, 빈 값을 채웁니다.

```bash
cp .env.example .env
```

`.env`에서 반드시 채워야 하는 항목:

| 항목 | 설명 |
|------|------|
| `DB_PASSWORD` | MySQL 사용자 비밀번호 (임의 값 지정) |
| `DB_ROOT_PASSWORD` | MySQL root 비밀번호 (임의 값 지정) |
| `JWT_SECRET` | 32자 이상의 임의 문자열 |
| `KAKAO_CLIENT_ID` / `KAKAO_CLIENT_SECRET` | [Kakao Developers](https://developers.kakao.com) 콘솔에서 발급 |
| `NAVER_CLIENT_ID` / `NAVER_CLIENT_SECRET` | [Naver Developers](https://developers.naver.com) 콘솔에서 발급 |

---

### 2. 실행 명령어

**개발 환경** (Hot Reload + 원격 디버그 포트 포함)

```bash
docker compose -f docker-compose.yml -f docker-compose.dev.yml up
```

**이미지 재빌드 후 실행**

```bash
docker compose -f docker-compose.yml -f docker-compose.dev.yml up --build
```

**운영 환경** (백그라운드 실행)

```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

---

### 3. 종료 명령어

```bash
docker compose down
```

---

### 4. 전체 초기화 (볼륨 포함)

> **주의:** DB 데이터가 모두 삭제됩니다.

```bash
docker compose down -v
```

---

### 서비스 구성

| 서비스 | 포트 (개발 환경) | 설명 |
|--------|-----------------|------|
| `app` | 8080 | Spring Boot 애플리케이션 |
| `db` | 3306 | MySQL 8.0 |
| `redis` | 6379 | Redis 7 |
| `app` (디버그) | 5005 | IntelliJ Remote JVM Debug 연결 포트 |

---

### Hot Reload 설정 (IntelliJ 기준)

개발 환경에서 소스 코드 변경 시 자동 재시작이 동작하려면 다음 설정이 필요합니다.

1. `Settings > Build, Execution, Deployment > Compiler` → **Build project automatically** 체크
2. `Settings > Advanced Settings` → **Allow auto-make to start even if developed application is currently running** 체크
3. 앱 실행 중 소스 수정 → IntelliJ가 자동 컴파일 → DevTools가 컨텍스트 재시작

---

### 자주 발생하는 오류 및 해결 방법

**`app` 서비스가 DB 연결 오류로 종료되는 경우**

DB 헬스체크가 통과되기 전에 앱이 기동 시도했을 수 있습니다. 재실행하면 해결됩니다.

```bash
docker compose down
docker compose -f docker-compose.yml -f docker-compose.dev.yml up
```

**포트 충돌 오류 (`address already in use`)**

```bash
# macOS / Linux
lsof -i :8080

# Windows
netstat -ano | findstr :8080
```

해당 포트를 사용 중인 프로세스를 종료하거나, `.env`에 `APP_PORT`를 추가해 포트를 변경하세요.

**`./gradlew: Permission denied` 오류**

```bash
git update-index --chmod=+x gradlew
git commit -m "fix: gradlew 실행 권한 추가"
```

**볼륨 데이터는 유지하면서 컨테이너만 재시작**

```bash
docker compose restart app
```

**DB 스키마 변경 후 `ValidationException` 발생**

Flyway 마이그레이션 파일이 추가되었는지 확인하세요. (`src/main/resources/db/migration/`)  
마이그레이션 파일 없이 스키마를 변경하면 `validate` 모드에서 기동이 실패합니다.
