# SSAC Backend — Claude Code 지침

## 에이전트 하네스 (Agent Protocol Harness)

| 목적 | 프로토콜 파일 | 실행 시점 | 실행 방식 |
|------|------------|---------|---------|
| 로그 기반 진단 | docs/agent-protocols/log-diagnose.md | 오류 발생 즉시 | 자동 (오류 즉시) |
| 토큰 최적화 | docs/agent-protocols/token-optimize.md | 작업 시작 전 / "느려","토큰","최적화" 언급 시 | 자동 |
| Railway 배포 진단 | docs/agent-protocols/log-diagnose.md#railway-배포-실패-시-로그-수집-절차 | "Railway 배포 실패", "빌드 실패", "배포 안 됨" 언급 시 | 자동 |
| 구조 충돌 점검 | docs/agent-protocols/sc-structure-check.md | SC 포함 작업 시 | 자동 (sc-harness 직후) |

---

## ⚡ 작업 시작 전 Protocol Execution Order

0순위 `token-optimize.md`       → 컨텍스트 최소화
1순위 `sc-harness.md`           → SC 관심사 점검
2순위 `sc-structure-check.md`   → 프로젝트 구조 충돌 점검
3순위 `new-feature.md`          → 신규 기능 개발

## ⚡ 오류 발생 시 Protocol Execution Order

1순위 `log-diagnose.md`  → 로그 기반 원인 진단 (즉시, 추측 기반 수정 금지)
2순위 `self-diagnose.md` → 자가 점검
3순위 `testing.md`       → 재발 방지 테스트 추가
4순위 `adr-create.md`    → 반복 오류 3회 이상 시 의사결정 기록

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
