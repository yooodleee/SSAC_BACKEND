## 관련 이슈

- Closes #

---

## ErrorCode 변경 내용

<!-- 추가/수정/삭제된 ErrorCode를 아래 표 형식으로 작성하라. -->

| 변경 유형 | ErrorCode | code | status | 이유 |
|-----------|-----------|------|--------|------|
| 추가/수정/삭제 | `EXAMPLE` | `DOMAIN-00N` | 4xx | 변경 이유 |

---

## Contract 버전 변경

| 이전 version | 새 version | 변경 근거 |
|-------------|-----------|---------|
| `x.y.z` | `x.y.z+1` | 추가 → Patch / 수정 → Minor / 삭제·변경 → Major |

---

## 에이전트/작성자 체크리스트

> PR을 열기 전 모두 통과해야 한다.

- [ ] `./gradlew build -x test` 통과 (컴파일 오류 없음)
- [ ] `./gradlew checkstyleMain` 통과
- [ ] `./gradlew validateErrorContract` 통과 (Enum ↔ Contract 정합성 확인)

### ErrorCode 변경 전용

- [ ] `ErrorCode` Enum 변경 완료
- [ ] `contract/error-contract.yml` 갱신 완료
- [ ] `contract/error-contract.yml` version 갱신 완료 (추가→Patch / 수정→Minor / 삭제·변경→Major)
- [ ] `./gradlew validateErrorContract` 검증 통과 확인
- [ ] FE 팀에 변경 사항 공유 완료 (CI `error-contract-validation` 잡의 FE 이슈 생성 확인)

---

## 리뷰어 확인 사항

- [ ] 변경된 ErrorCode의 `status`가 실제 응답 HTTP 상태코드와 일치하는가?
- [ ] Contract 파일 version이 변경 유형에 맞게 올바르게 갱신되었는가?
- [ ] 삭제·변경된 ErrorCode가 기존 클라이언트에 미치는 영향을 검토했는가?
- [ ] FE 매핑 테이블 업데이트가 FE 이슈로 트래킹되고 있는가?

---

## [QUESTION] 불확실한 사항

<!-- 에이전트가 확신하지 못한 결정이 있다면 여기에 남겨라.
     예: "[QUESTION] NEWS-002의 status를 404 대신 410으로 해야 하는지 확인 필요" -->
