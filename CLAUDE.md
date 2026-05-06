# SSAC Backend — Claude Code 지침

## 에이전트 하네스 (Agent Protocol Harness)

| 목적 | 프로토콜 파일 | 실행 시점 | 실행 방식 |
|------|------------|---------|---------|
| 로그 기반 진단 | docs/agent-protocols/log-diagnose.md | 오류 발생 즉시 | 자동 (오류 즉시) |

---

## ⚡ 오류 발생 시 Protocol Execution Order

1순위 `log-diagnose.md`  → 로그 기반 원인 진단 (즉시, 추측 기반 수정 금지)
2순위 `self-diagnose.md` → 자가 점검
3순위 `testing.md`       → 재발 방지 테스트 추가
4순위 `adr-create.md`    → 반복 오류 3회 이상 시 의사결정 기록

---

## 🚫 진단 없는 수정 금지 규칙

오류 발생 시 아래 행동은 금지된다:
- `log-diagnose.md` 실행 없이 코드 수정 시작
- 원인 불명 상태에서 추측 기반 수정
