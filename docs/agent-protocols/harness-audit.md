# Harness Audit Protocol (BE)

## 역할
하네스 시스템 전체 건강 상태 점검. 점검·보고만 수행하며 구현은 하지 않는다.  
트리거: 사용자가 "하네스 점검/감사/audit" 언급 시 / 스프린트 종료 / 반복 오류 3회 이상 후 adr-create 실행 전

## 실행 순서
STEP 1(파일 존재) → 2(CLAUDE.md 정합성) → 3(프로토콜 품질) → 4(트리거 연쇄) → 5(결과 출력) → 6(기록)

---

## STEP 1. 프로토콜 파일 존재 여부

```
Glob: docs/agent-protocols/*.md
```

**필수 프로토콜** (누락 시 Critical):
`token-optimize` / `sc-harness` / `backlog-generate` / `sc-structure-check` / `new-feature` / `testing` / `self-diagnose` / `log-diagnose` / `adr-create` / `harness-audit` / `contract-sync` / `e2e-diagnose`

**보조 문서** (누락 시 Low):
`docs/conventions/flyway.md` / `docs/debug-log.md` / `contract/api-spec.yaml` / `contract/error-contract.yml`

---

## STEP 2. CLAUDE.md 참조 정합성

```
Read: CLAUDE.md
```

- [ ] Protocol Execution Order (작업 전/완료 후/오류 시) 에 명시된 파일이 모두 존재하는가
- [ ] 하네스 목록 테이블 파일명이 실제 파일과 일치하는가
- [ ] 트리거 조건 / 실행 방식(자동/수동) 표기가 올바른가
- [ ] SC 검토 없이 구현 금지 / 빌드 실패 시 완료 보고 금지 / Flyway 규칙이 명시되어 있는가

---

## STEP 3. 프로토콜 내용 품질

각 파일을 읽어 아래 핵심 항목 포함 여부를 확인한다:

| 파일 | 핵심 점검 항목 |
|------|-------------|
| `token-optimize.md` | 컨텍스트 최소화 절차 / harness-audit 트리거 참조 |
| `sc-harness.md` | BE/FE 관심사 분리 기준 / 혼재 항목 재작성 규칙 |
| `backlog-generate.md` | 프로젝트 구조 파악 5단계 / 핵심 파일 목록 / SC 충돌 감지 절차 |
| `sc-structure-check.md` | API 경로·엔티티·ErrorCode·Flyway 충돌 점검 / MySQL 호환 문법 점검 |
| `testing.md` | checkstyle→compileJava→test→jacoco 순서 / 오류 유형 조치 / 커버리지 70% 기준 / 자동 트리거 |
| `self-diagnose.md` | 레이어 책임·인증·응답 구조·Contract 점검 / Redis fallback(CACHE-2) 점검 |
| `log-diagnose.md` | Railway 로그 수집 절차 / Redis 직접 조회 절차 / debug-log.md 연결 |
| `adr-create.md` | 반복 오류 3회 트리거 / ADR 문서 생성 절차 / 의사결정 기록 양식 |

추가 점검: 200줄 초과 파일이 있는지 확인 → 초과 시 High로 분류

---

## STEP 4. 트리거 연쇄 점검

아래 연쇄 경로의 각 파일이 존재하고 내용이 연결되는지 확인한다:

- **작업 전**: CLAUDE.md → `sc-harness` → `backlog-generate` → `sc-structure-check` → `new-feature`
- **작업 후**: CLAUDE.md → `contract-sync` → `testing` → `self-diagnose`
- **오류 시**: CLAUDE.md → `log-diagnose` → `debug-log.md` 연결 / 3회 반복 → `adr-create`
- **수동**: CLAUDE.md → `harness-audit` / `adr-create` / `token-optimize` → `harness-audit` 연쇄 참조

연쇄가 끊어진 구간은 ❌ 트리거 단절로 분류하고 구간을 명시한다.

---

## STEP 5. 감사 결과 출력

```
[Harness Audit 결과]

파일 존재    : ✅ N개 / ❌ Critical N개 / ⚠️ Low N개
CLAUDE.md   : ✅ 정합 / ❌ {불일치 항목}
프로토콜 품질: ✅ N개 이상 없음 / ⚠️ {미흡 파일·항목} / ❌ {누락 파일·항목}
트리거 연쇄  : ✅ 전체 정상 / ❌ {단절 구간}

Critical N개 / High N개 / Low N개
→ Critical·High 존재 시 보완 백로그 SC 생성 후 보고 (구현 수행하지 않음)
```

---

## STEP 6. debug-log.md 기록

```
## ✅/🔴 [AUDIT] YYYY-MM-DD — Harness Audit

### 감사 요약
- Critical N개 / High N개 / Low N개

### 주요 발견 항목
- ❌ {누락 파일 또는 문제 항목}
- ⚠️ {주의 항목}

### 조치 계획
- HARNESS-N: {백로그 SC 제목}
```
