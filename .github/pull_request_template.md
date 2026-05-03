<!-- ErrorCode(Enum) 추가/수정/삭제 PR은 이 템플릿 대신 아래 링크를 사용하세요:
     PR URL 뒤에 ?template=errorcode-change.md 를 붙이거나
     GitHub PR 생성 화면에서 "errorcode-change" 템플릿을 선택하세요. -->

## 관련 이슈

- Closes #

---

## 변경 사항 요약

<!-- 무엇을 왜 변경했는지 2~4줄로 설명하라. "무엇"보다 "왜"에 집중하라. -->

---

## 변경 유형

- [ ] feat: 새 기능
- [ ] fix: 버그 수정
- [ ] refactor: 리팩토링 (동작 변경 없음)
- [ ] docs: 문서 변경
- [ ] chore: 빌드/설정 변경
- [ ] test: 테스트 추가/수정

---

## 에이전트/작성자 체크리스트

> PR을 열기 전 모두 통과해야 한다.

- [ ] `./gradlew build -x test` 통과 (컴파일 오류 없음)
- [ ] `./gradlew test` 통과 (ArchUnit 포함)
- [ ] `./gradlew checkstyleMain` 통과
- [ ] `docs/architecture.md`의 레이어 규칙을 위반하지 않음
- [ ] 비밀키/환경변수를 하드코딩하지 않음
- [ ] `@Transactional`을 Controller에 붙이지 않음

---

## 리뷰어 확인 사항

- [ ] 변경 의도가 코드에서 명확히 드러나는가?
- [ ] 레이어 의존성 방향이 올바른가? (`docs/architecture.md`)
- [ ] API 응답이 `ApiResponse<T>` 형식을 따르는가?
- [ ] 새 비즈니스 예외가 `BusinessException`을 상속하는가?
- [ ] 테스트가 실제 동작을 검증하는가? (Mock 남용 여부)

---

## docs/ 업데이트 필요 여부

> 이 PR이 병합된 후 아래 중 하나라도 해당하면 반드시 별도 PR 또는 이 PR에 포함하라.

- [ ] 새 도메인/레이어 추가 → `docs/architecture.md` 업데이트 필요
- [ ] 새 컨벤션/규칙 도입 → `docs/conventions.md` 업데이트 필요
- [ ] 중요 아키텍처 결정 → `docs/decisions/NNN-*.md` ADR 추가 필요
- [ ] 새 기술 부채 발생 → `docs/quality.md` 업데이트 필요
- [ ] 해당 없음

---

## [QUESTION] 불확실한 사항

<!-- 에이전트가 확신하지 못한 결정이 있다면 여기에 남겨라.
     예: "[QUESTION] UserResponse에 password 필드 제외가 맞는지 확인 필요" -->
