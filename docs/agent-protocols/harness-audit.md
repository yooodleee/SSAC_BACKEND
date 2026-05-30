# Harness Audit Protocol (BE)

## 역할
하네스 시스템 전체의 건강 상태를 점검하고
누락 / 미반영 / 형식 오류 항목을 발견하여
보고한다.
구현을 수행하지 않는다. 점검과 보고만 수행한다.

## 트리거 조건
- 사용자가 "하네스 점검", "감사", "audit" 언급 시
- 스프린트 종료 시점
- 반복 오류 3회 이상 발생 후 adr-create.md 실행 전
- CLAUDE.md "수동 실행" 테이블 참조 시

## 실행 순서
STEP 1. 프로토콜 파일 존재 여부 점검
STEP 2. CLAUDE.md 참조 정합성 점검
STEP 3. 각 프로토콜 내용 품질 점검
STEP 4. 트리거 연쇄 점검
STEP 5. 감사 결과 출력
STEP 6. debug-log.md 기록

---

## STEP 1. 프로토콜 파일 존재 여부 점검

아래 파일이 모두 존재하는지 확인한다:

### 필수 프로토콜 파일
```
Glob: docs/agent-protocols/*.md
```

□ docs/agent-protocols/token-optimize.md
□ docs/agent-protocols/sc-harness.md
□ docs/agent-protocols/backlog-generate.md
□ docs/agent-protocols/sc-structure-check.md
□ docs/agent-protocols/new-feature.md
□ docs/agent-protocols/testing.md
□ docs/agent-protocols/self-diagnose.md
□ docs/agent-protocols/log-diagnose.md
□ docs/agent-protocols/adr-create.md
□ docs/agent-protocols/harness-audit.md

### 보조 문서 파일
□ docs/conventions/flyway.md
□ docs/debug-log.md
□ contract/api-spec.yaml
□ contract/error-contract.yml

### 판정 기준
```
존재 → ✅
누락 → ❌ (Critical 또는 Low 분류)
```

| 분류 | 대상 | 기준 |
|------|------|------|
| Critical | 필수 프로토콜 파일 | CLAUDE.md 프로토콜 테이블에 등재된 파일 |
| Low      | 보조 문서 파일    | 프로토콜이 참조하지만 비필수 문서 |

---

## STEP 2. CLAUDE.md 참조 정합성 점검

CLAUDE.md에 명시된 프로토콜 참조가
실제 파일과 일치하는지 확인한다:

```
Read: CLAUDE.md
```

### Protocol Execution Order 점검
□ 작업 전 순서에 명시된 파일이
    모두 존재하는가?
□ 작업 완료 후 순서에 명시된 파일이
    모두 존재하는가?
□ 오류 발생 시 순서에 명시된 파일이
    모두 존재하는가?

### 하네스 목록 테이블 점검
□ 테이블에 등록된 파일이 실제로 존재하는가?
□ 트리거 조건이 파일 내용과 일치하는가?
□ 실행 방식(자동/수동)이 올바르게 표기되어 있는가?

### 금지 규칙 점검
□ SC 검토 없이 구현 금지 규칙이 명시되어 있는가?
□ 빌드/테스트 실패 시 완료 보고 금지 규칙이
    명시되어 있는가?
□ Flyway 마이그레이션 작성 규칙이
    명시되어 있는가?

---

## STEP 3. 각 프로토콜 내용 품질 점검

각 프로토콜 파일에 대해 아래 기준으로
내용을 점검한다:

### token-optimize.md
□ 컨텍스트 최소화 절차가 명시되어 있는가?
□ harness-audit.md 트리거 참조가 있는가?

### sc-harness.md
□ BE/FE 관심사 분리 기준이 명시되어 있는가?
□ 혼재 항목 재작성 규칙이 있는가?

### backlog-generate.md
□ 프로젝트 구조 파악 절차 5단계가 있는가?
□ 핵심 파일 목록(CLAUDE.md / ErrorCode.java 등)이
    명시되어 있는가?
□ SC 충돌 감지 및 수정 절차가 있는가?

### sc-structure-check.md
□ API 경로 / 엔티티 / ErrorCode / Flyway
    충돌 점검 항목이 있는가?
□ MySQL 호환 Flyway 문법 점검 항목이 있는가?
    (ADD COLUMN IF NOT EXISTS 금지 여부)

### testing.md
□ compileJava → test → jacocoVerification
    순서가 명시되어 있는가?
□ 오류 유형별 수정 절차가 있는가?
□ 커버리지 70% 기준이 명시되어 있는가?
□ 자동 실행 트리거 조건이 명시되어 있는가?

### self-diagnose.md
□ 구현 완료 후 자가 점검 항목이 있는가?
□ 캐싱 직렬화 점검 항목이 있는가?
    (StringRedisTemplate + TypeReference 패턴
     또는 @Cacheable + Redis 직렬화 방식)
□ 최근 스프린트 트러블슈팅 반영 여부 확인

### log-diagnose.md
□ Railway 로그 수집 절차가 있는가?
□ railway variables --service Redis
    공개 proxy URL 확인 절차가 있는가?
□ Redis 직접 조회 절차가 있는가?
    (redis-cli 또는 Python 클라이언트)
□ STEP 5 결과 기록 → debug-log.md 연결이 있는가?

### adr-create.md
□ 반복 오류 3회 이상 트리거 조건이 있는가?
□ ADR 문서 생성 절차가 있는가?
□ 의사결정 기록 양식이 정의되어 있는가?

---

## STEP 4. 트리거 연쇄 점검

프로토콜 간 트리거 연결이 끊어지지 않았는지
아래 경로를 순서대로 추적한다:

### 작업 시작 전 연쇄
```
CLAUDE.md
    → 1순위 sc-harness.md 존재 확인
    → 2순위 backlog-generate.md 존재 확인
    → 3순위 sc-structure-check.md 존재 확인
    → 4순위 new-feature.md 존재 확인
```

### 작업 완료 후 연쇄
```
CLAUDE.md
    → 4순위 contract-sync.md 존재 확인
    → 5순위 testing.md 존재 확인
                → jacocoTestCoverageVerification 연결 확인
    → 6순위 self-diagnose.md 존재 확인
```

### 오류 발생 시 연쇄
```
CLAUDE.md
    → 즉시 log-diagnose.md 존재 확인
            → STEP 5 → debug-log.md 연결 확인
    → 반복 오류 3회 → adr-create.md 존재 확인
```

### 수동 실행 연쇄
```
CLAUDE.md 수동 실행 테이블
    → harness-audit.md 존재 확인 (이 파일)
    → adr-create.md 존재 확인
    → token-optimize.md 존재 확인
            → harness-audit.md 연쇄 참조 확인
```

연쇄가 끊어진 경우:
```
→ ❌ 트리거 단절로 분류
→ 어느 구간에서 끊어졌는지 명시
```

---

## STEP 5. 감사 결과 출력

아래 형식으로 감사 결과를 출력한다:

```
## Harness Audit 결과

### 프로토콜 파일 존재 여부
✅ token-optimize.md     존재
✅ sc-harness.md         존재
❌ harness-audit.md      누락  → Critical
❌ adr-create.md         누락  → Critical
⚠️ docs/debug-log.md     누락  → Low

### CLAUDE.md 참조 정합성
✅ Protocol Execution Order 파일 전체 존재
❌ 수동 실행 테이블 → harness-audit.md 누락
❌ 오류 3회 시 → adr-create.md 누락

### 프로토콜 내용 품질
✅ testing.md           커버리지 기준 명시
⚠️ self-diagnose.md     캐싱 직렬화 점검 항목 없음
⚠️ log-diagnose.md      Railway Redis 진단 절차 미흡

### 트리거 연쇄
✅ 작업 시작 전 연쇄    정상
✅ 작업 완료 후 연쇄    정상
❌ 오류 발생 시 연쇄    adr-create.md 누락으로 단절
❌ 수동 실행 연쇄       harness-audit.md 누락으로 단절

### 감사 요약
┌──────────────┬───────┐
│ 구분         │ 건수  │
├──────────────┼───────┤
│ Critical     │  N개  │
│ High         │  N개  │
│ Medium       │  N개  │
│ Low          │  N개  │
│ 정상         │  N개  │
└──────────────┴───────┘

→ Critical / High 항목이 존재하면
  즉시 보완 백로그 SC를 생성하여 보고한다.
→ 구현은 수행하지 않는다.
```

---

## STEP 6. debug-log.md 기록

감사 완료 후 아래 형식으로
`docs/debug-log.md`에 결과를 기록한다:

```markdown
## [YYYY-MM-DD] Harness Audit

### 감사 요약
- Critical  : N개
- High      : N개
- Medium    : N개
- Low       : N개

### 주요 발견 항목
- ❌ {누락 파일 또는 문제 항목}
- ⚠️ {주의 항목}

### 조치 계획
- HARNESS-N: {백로그 SC 제목}

### 감사 항목 시각
{datetime}
```

`docs/debug-log.md`가 존재하지 않는 경우
파일을 새로 생성한 후 위 형식으로 기록한다.
