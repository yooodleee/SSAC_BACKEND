# AGENTS.md — SSAC Backend 에이전트 진입점

## 이 저장소는 무엇인가

SSAC 서비스의 Spring Boot 백엔드 API 서버다.
Java 17, Spring Boot 4.x, Spring Security, JPA(MySQL) 스택으로 구성된다.

---

## 하네스 핵심 원칙

이 프로젝트는 AI 에이전트와의 협업을 전제로 설계된다. 코드를 작성하기 전에 아래 원칙을 반드시 숙지하라.

### 원칙 #1: Agent 중심 아키텍처 (ADR 002)

> **인간은 의도(intent)만 정의하고, 실제 구현은 AI 에이전트가 수행한다.**
> 수동 코딩을 전제로 설계하지 않는다.

| 역할 | 담당 |
|------|------|
| **인간** | 무엇을/왜 만드는지 정의, 완료 기준 지정, 최종 승인 |
| **에이전트** | 코드 작성, 컨벤션 준수, 테스트, 문서화 |

→ 상세 내용: `docs/decisions/002-agent-centric-architecture.md`

### 원칙 #2: 환경 중심 설계 (ADR 003)

> **긴 프롬프트가 아닌 구조화된 환경을 기반으로 동작해야 한다.**
> 이 파일(AGENTS.md)은 진입점 인덱스일 뿐, 단일 지시 파일이 아니다.

규칙 배치 우선순위:

```
1순위: 도구로 강제   →  ArchUnit, Checkstyle, CI 빌드 실패
2순위: 구조로 강제   →  패키지 분리, 설정 파일
3순위: 문서로 안내   →  docs/ 하위 각 파일
4순위 (지양): 지시   →  이 파일(AGENTS.md) 텍스트 나열
```

> 새 규칙을 여기에 직접 쓰기 전에 먼저 1~3순위로 표현할 수 없는지 검토하라.

→ 상세 내용: `docs/decisions/003-environment-centric-design.md`

---

## 코드 작성 전 반드시 읽어야 할 파일

| 파일 | 읽어야 하는 이유 |
|------|----------------|
| `docs/architecture.md` | 레이어 구조와 의존성 방향 — 위반 시 빌드 실패 |
| `docs/conventions.md` | 네이밍, 패키지, 응답 형식 컨벤션 |
| `docs/swagger-guide.md` | **컨트롤러 작성 시 필수** — @Operation description 작성 기준 |
| `docs/decisions/` | 이미 결정된 사항을 다시 논의하지 않기 위해 |

> 위 파일을 읽지 않고 코드를 작성하면 PR이 거절된다.

---

## 커밋 전 체크리스트

```
[ ] ./gradlew build -x test   # 컴파일 오류 없음
[ ] ./gradlew test             # 테스트 전체 통과 (ArchUnit 포함)
[ ] ./gradlew checkstyleMain   # 스타일 위반 없음
[ ] 새 레이어 의존성 추가 시 docs/architecture.md 업데이트
[ ] 새 설계 결정 시 docs/decisions/ 에 ADR 추가
```

---

## 금지 행동

- `master` 브랜치 직접 커밋 — PR을 통해서만 병합
- `controller` → `repository` 직접 의존 — `service` 레이어를 반드시 경유
- `domain` 패키지에서 외부 레이어 클래스 import
- 환경변수·비밀키를 소스코드에 하드코딩
- `@Transactional`을 `controller`에 붙이는 것
- docs/ 없이 아키텍처 결정 구두로만 처리

---

## 모르는 것이 있을 때

- 레이어/의존성 → `docs/architecture.md`
- 네이밍/포맷 → `docs/conventions.md`
- 이미 내린 결정 → `docs/decisions/`
- 온보딩/환경 설정 → `docs/onboarding.md`
- 현재 기술 부채 → `docs/quality.md`

답을 찾지 못했다면 임의로 결정하지 말고 PR 설명란에 `[QUESTION]` 태그로 질문을 남겨라.
