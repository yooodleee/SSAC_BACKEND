# SSAC Backend — Claude Code 지침

## 🛡️ Agent Harness Protocols

| 프로토콜 | 파일 위치 | 트리거 조건 | 실행 방식 |
|---------|----------|-----------|---------|
| SC 관심사 점검 | docs/agent-protocols/sc-harness.md | SC 포함 작업 지시 시 | 자동 (구현 전 필수) |
| SC 생성/수정 | docs/agent-protocols/backlog-generate.md | SC 포함 작업 지시 시 | 자동 (sc-harness 직후) |
| 구조 충돌 점검 | docs/agent-protocols/sc-structure-check.md | SC 포함 작업 지시 시 | 자동 (sc-harness 직후) |
| 신규 기능 개발 | docs/agent-protocols/new-feature.md | 신규 기능 구현 시작 시 | 자동 (sc-structure-check 직후) |
| 로그 기반 진단 | docs/agent-protocols/log-diagnose.md | 오류 발생 즉시 | 자동 (오류 즉시) |
| 계약 동기화 | docs/agent-protocols/contract-sync.md | ErrorCode 변경 또는 API 경로 변경 시 | 자동 (구현 후) |
| 자가 진단 | docs/agent-protocols/self-diagnose.md | 구현 완료 후 | 자동 (구현 후) |
| 테스트 작성 | docs/agent-protocols/testing.md | 구현 완료 후 | 자동 (구현 후) |
| 토큰 최적화 | docs/agent-protocols/token-optimize.md | 스프린트 종료 시 | 수동 |
| ADR 생성 | docs/agent-protocols/adr-create.md | 반복 오류 3회 이상 / 기술 의사결정 시 | 수동 |
| 하네스 감사 | docs/agent-protocols/harness-audit.md | 스프린트 종료 / 중간 점검 시 | 수동 |

---

## ⚡ Protocol Execution Order

### [작업 시작 전]
1순위 `sc-harness.md`           → SC 관심사 점검
2순위 `backlog-generate.md`     → 프로젝트 구조 파악 후 SC 생성 / 수정
3순위 `sc-structure-check.md`   → 프로젝트 구조 충돌 점검
4순위 `new-feature.md`          → 신규 기능 개발

### [작업 완료 후 — 자동 실행]
4순위 `contract-sync.md`        → ErrorCode / Swagger 어노테이션 계약 동기화
5순위 `testing.md`              → compileJava → test → 커버리지 검증
6순위 `self-diagnose.md`        → 자가 점검

### [오류 발생 시 — 즉시 실행]
즉시   `log-diagnose.md`        → 로그 기반 원인 진단
1순위 `self-diagnose.md`        → 자가 점검
2순위 `testing.md`              → 재발 방지 테스트 추가
3순위 `adr-create.md`           → 반복 오류 3회 이상 시 의사결정 기록

### [수동 실행]
-     `harness-audit.md`        → 스프린트 종료 / 중간 점검 시 전체 하네스 감사
-     `adr-create.md`           → 반복 오류 3회 이상 / 기술 의사결정 시 기록
-     `token-optimize.md`       → 스프린트 종료 시 최적화 (harness-audit 직후 연쇄)

---

---

## 🚂 Railway 배포 실패 시 자동 진단 규칙

**트리거 조건:**
- "Railway 배포 실패", "빌드 실패", "배포 안 됨" 언급 시
- Railway 대시보드에서 Failed 상태 확인 시

**자동 실행 절차:**
1. `railway logs --deployment` 실행하여 로그 수집
2. `log-diagnose.md` Railway 섹션 분류표 기준으로 원인 분류
3. 원인 확정 후 수정 진행
4. 수정 완료 후 `railway logs --tail`로 재배포 확인

**금지 규칙:**
- 로그 확인 없이 추측 기반으로 코드 수정 금지
- 사용자가 로그를 직접 복사하여 제공하도록 요청 금지

---

## 🚫 진단 없는 수정 금지 규칙

오류 발생 시 아래 행동은 금지된다:
- `log-diagnose.md` 실행 없이 코드 수정 시작
- 원인 불명 상태에서 추측 기반 수정

---

## 🚫 SC 관심사 점검 없이 구현 금지 규칙

아래 행동은 금지된다:
- sc-harness.md 실행 없이 구현 시작
- FE SC 항목을 BE 코드에 구현
- UI 동작을 BE 비즈니스 로직으로 처리
- FE 에러 메시지 문구를 BE에서 하드코딩

---

## 🚫 SC 검토 없이 구현 금지 규칙

아래 행동은 금지된다:
- 외부에서 제공된 SC를 검토 없이 그대로 구현
- 프로젝트 구조 파악 없이 SC 생성
- 기존 코드 분석 없이 ErrorCode / API 경로 결정
- backlog-generate.md 실행 없이 new-feature.md 실행

---

## 💰 Token Economy Rules (전역 적용)

### 파일 로드 규칙
□ 작업과 무관한 파일은 로드하지 않는다
□ 프로토콜 파일은 트리거 조건 충족 시에만 로드한다
□ Contract 파일은 API 연동 작업 시에만 로드한다

### 응답 생성 규칙
□ 코드 예시는 핵심 부분만 포함한다 (전체 파일 출력 금지)
□ 이미 확인된 내용을 반복 출력하지 않는다
□ 체크리스트는 변경/추가 항목만 출력한다

### 프로토콜 실행 규칙
□ 트리거 조건을 충족하지 않는 프로토콜은 실행하지 않는다
□ 이미 통과한 프로토콜을 동일 작업에서 재실행하지 않는다
□ 프로토콜 실행 결과는 요약본만 출력한다

위 규칙 위반 시:
→ 즉시 중단하고 규칙 위반 항목을 출력한 후 재실행한다

---

## 🗄️ Flyway 마이그레이션 작성 규칙

### MySQL / MariaDB 호환성 주의
아래 문법은 MariaDB 전용 — MySQL에서 절대 사용 금지:
❌ ALTER TABLE ... ADD COLUMN IF NOT EXISTS
❌ CREATE INDEX IF NOT EXISTS

### 멱등성 보장 규칙
□ CREATE TABLE → CREATE TABLE IF NOT EXISTS (MySQL 지원)

□ ADD COLUMN   → information_schema 조건부 패턴 사용
  (docs/conventions/flyway.md 템플릿 참고)

□ CREATE INDEX → information_schema 조건부 패턴 사용
  (docs/conventions/flyway.md 템플릿 참고)

□ INSERT       → INSERT IGNORE 또는
                  ON DUPLICATE KEY UPDATE 사용

### 작성 완료 후 검증 명령어
```bash
# MySQL 미지원 문법 확인
grep -rn "ADD COLUMN IF NOT EXISTS\|CREATE INDEX IF NOT EXISTS" \
  src/main/resources/db/migration/
# → 출력 없음 확인 (MySQL 미지원 문법 미사용)

# CREATE TABLE IF NOT EXISTS 누락 확인
grep -rn "CREATE TABLE " src/main/resources/db/migration/ | \
  grep -v "IF NOT EXISTS"
# → 출력 없음 확인
```

참고: docs/conventions/flyway.md

---

## 🚫 Redis 캐싱 금지 규칙

아래 행동은 금지된다:

□ @Cacheable + GenericJackson2JsonRedisSerializer 조합 사용 금지
  → 타입 정보 포함으로 역직렬화 오류 발생
  → 반드시 StringRedisTemplate 수동 캐싱 사용
  → 참고: ADR-003

□ TTL 미설정 캐시 저장 금지
  → 메모리 누수 발생 가능
  → 모든 캐시 항목에 TTL 명시 필수

□ 캐시 키 임의 작성 금지
  → 기존 키 형식(domain:type:{id}) 준수
  → sc-structure-check.md 캐시 키 일관성 점검 필수

---

## 🔨 빌드 / 테스트 자동 실행 규칙

아래 상황에서 반드시 `testing.md`를 자동 실행한다:

### 자동 실행 조건
□ 코드 파일 (.java) 수정 / 추가 / 삭제 완료 시
□ build.gradle 의존성 변경 시
□ Flyway 마이그레이션 파일 수정 / 추가 시
□ application*.yml 설정 변경 시

### 실행 순서
```
./gradlew checkstyleMain checkstyleTest
  ↓
./gradlew compileJava
  ↓
./gradlew test
  ↓
./gradlew jacocoTestReport jacocoTestCoverageVerification
```

또는 한 번에: `bash scripts/run-tests.sh`

### 절대 금지 규칙
→ 사용자 요청 없이 구현만 완료하고 빌드/테스트 실행 없이 종료 금지
→ 빌드/테스트 실패 상태에서 "완료"라고 보고 금지
→ 커버리지 70% 미달 상태에서 구현 완료로 간주 금지
