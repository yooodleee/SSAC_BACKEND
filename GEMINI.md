# GEMINI.md — SSAC Backend Gemini 에이전트 진입점

## 작업 진행 전 확인 원칙

모든 작업(파일 생성·수정·삭제, 명령 실행, 설계 변경)을 진행하기 전에 반드시 한국어로 확인을 받아야 한다.

### 확인 메시지 형식

작업 전에 아래 형식으로 사용자에게 확인을 요청한다.

```
다음 작업을 진행하겠습니까?

[작업 유형] 파일 생성 / 수정 / 삭제 / 명령 실행 / 설계 변경
[대상 파일] 정확한 파일 경로 (예: src/main/java/com/ssac/ssacbackend/service/UserService.java)
[변경 내용] 무엇을 왜 변경하는지 한국어로 설명

진행하시겠습니까? (예 / 아니오)
```

### 확인이 필요한 작업 목록

| 작업 종류 | 확인 필수 여부 |
|-----------|----------------|
| 새 파일 생성 | 필수 |
| 기존 파일 수정 | 필수 |
| 파일 삭제 | 필수 |
| `./gradlew` 명령 실행 | 필수 |
| 브랜치 생성 / 전환 | 필수 |
| 커밋 / PR 생성 | 필수 |
| 패키지 구조 변경 | 필수 |
| 의존성(`build.gradle`) 추가·제거 | 필수 |
| docs/ 문서 수정 | 필수 |

### 파일 경로 명시 규칙

확인 메시지에서 파일을 언급할 때는 반드시 **프로젝트 루트 기준 전체 경로**를 표기한다.

```
# 잘못된 표기 (금지)
UserService.java
service/UserService.java

# 올바른 표기
src/main/java/com/ssac/ssacbackend/service/UserService.java
src/test/java/com/ssac/ssacbackend/service/UserServiceTest.java
src/main/resources/application.properties
docs/architecture.md
build.gradle
```

대화 중 파일을 언급할 때도 항상 전체 경로를 사용한다.

### 복수 파일 작업 시

여러 파일을 동시에 변경할 경우, 묶어서 한 번에 확인을 받는다.

```
다음 작업들을 순서대로 진행하겠습니까?

1. [생성] src/main/java/com/ssac/ssacbackend/domain/user/User.java
   → User 엔티티 클래스 생성
2. [생성] src/main/java/com/ssac/ssacbackend/domain/user/UserRepository.java
   → UserRepository 인터페이스 생성
3. [생성] src/main/java/com/ssac/ssacbackend/service/UserService.java
   → UserService 비즈니스 로직 클래스 생성
4. [수정] docs/architecture.md
   → User 도메인 패키지 구조 추가

진행하시겠습니까? (예 / 아니오)
```

### 사용자가 거절한 경우

- 임의로 다른 방법을 선택하지 않는다
- 거절 이유를 물어보고 사용자의 의도에 맞게 계획을 수정한다
- 수정된 계획도 동일한 확인 형식으로 다시 요청한다

---

## 이 저장소는 무엇인가

SSAC 서비스의 Spring Boot 백엔드 API 서버다.
Java 17, Spring Boot 4.x, Spring Security, JPA(MySQL) 스택으로 구성된다.

---

## 코드 작성 전 반드시 읽어야 할 파일

| 파일 | 읽어야 하는 이유 |
|------|----------------|
| `docs/architecture.md` | 레이어 구조와 의존성 방향 — 위반 시 빌드 실패 |
| `docs/conventions.md` | 네이밍, 패키지, 응답 형식 컨벤션 |
| `docs/swagger-guide.md` | 컨트롤러 작성 시 필수 — @Operation description 작성 기준 |
| `docs/decisions/` | 이미 결정된 사항을 다시 논의하지 않기 위해 |

---

## 레이어 구조

```
com.ssac.ssacbackend
├── domain/          # 엔티티, VO, 도메인 예외, Repository 인터페이스
├── repository/      # JPA Repository 구현체
├── service/         # 비즈니스 로직, @Transactional 경계
├── controller/      # REST 컨트롤러, 요청/응답 매핑
├── dto/             # Request / Response DTO
├── config/          # Spring 설정 클래스
└── common/          # 공통 유틸, 전역 예외 핸들러, 상수
```

### 의존성 방향 (위반 불가 — ArchUnit이 자동 검증)

```
controller → service → repository → domain
```

- `controller`는 `repository`를 직접 호출할 수 없다
- `domain`은 외부 레이어를 import할 수 없다
- `dto`, `config`, `common`은 모든 레이어에서 참조 가능

---

## 네이밍 규칙

| 대상 | 규칙 | 예시 |
|------|------|------|
| 클래스 | PascalCase | `UserService`, `PostController` |
| 메서드/변수 | camelCase | `findUserById`, `userName` |
| 상수 | UPPER_SNAKE_CASE | `MAX_PAGE_SIZE` |
| 패키지 | 소문자, 단수형 | `com.ssac.ssacbackend.domain.user` |
| DB 컬럼 | snake_case | `created_at`, `user_id` |
| API 경로 | kebab-case, 복수형 | `/api/v1/users` |
| DTO | 접미사로 `Request` / `Response` | `CreateUserRequest`, `UserResponse` |

### 클래스 접미사

| 접미사 | 역할 |
|--------|------|
| `Controller` | REST 엔드포인트 |
| `Service` | 비즈니스 로직 |
| `Repository` | 데이터 접근 (interface) |
| 없음 | 엔티티는 도메인 이름만 사용 (`User`, `Post`) |
| `Request` | 인바운드 DTO |
| `Response` | 아웃바운드 DTO |
| `Exception` | 커스텀 예외 |
| `Config` | Spring 설정 빈 |

---

## API 응답 형식

모든 API는 `ApiResponse<T>` 래퍼를 사용한다.

```json
{ "success": true, "data": { ... }, "message": null }
{ "success": false, "data": null, "message": "사용자를 찾을 수 없습니다." }
```

- 성공: HTTP 200/201, `success: true`
- 실패: 4xx/5xx, `success: false`, `message`에 한국어 설명
- `GlobalExceptionHandler`에서 예외를 변환하며, 컨트롤러에서 직접 에러 응답을 만들지 않는다

---

## 코드 작성 규칙

### 트랜잭션
- `@Transactional`은 **Service 레이어에만** 붙인다
- 읽기 전용 메서드: `@Transactional(readOnly = true)`
- Controller에 `@Transactional` 절대 금지

### 예외 처리
- 비즈니스 예외는 `BusinessException`(또는 하위 클래스)을 사용한다
- `RuntimeException`을 직접 던지지 않는다
- 예외 메시지는 한국어로 작성한다

```java
throw new BusinessException("사용자를 찾을 수 없습니다.");  // OK
throw new RuntimeException("User not found");              // 금지
```

### 로깅
- `@Slf4j` (Lombok) 사용
- Controller 진입/종료: DEBUG 레벨
- 비즈니스 예외: WARN 레벨
- 시스템 예외: ERROR 레벨
- 로그에 비밀번호·토큰·개인정보 포함 금지

### Lombok
- 엔티티: `@Getter`, `@NoArgsConstructor(access = AccessLevel.PROTECTED)` 기본
- `@Data`는 엔티티에서 금지 — `@Getter` + 필요한 어노테이션만 사용
- DTO: `@Getter`, `@Builder` 또는 `record` 사용

### 파일 길이
- 클래스: 300줄 이내 권장
- 메서드: 30줄 이내 권장

---

## Swagger 어노테이션 규칙

모든 `@Operation`의 `description`에 아래 3가지를 포함해야 한다.

| 항목 | 작성 형식 |
|------|-----------|
| 호출 화면/상황 | `[호출 화면]` 태그 |
| 권한 조건 | `[권한 조건]` 태그 |
| 특이 동작 | `[특이 동작]` 태그 (없으면 생략 가능) |

```java
@Operation(
    summary = "특정 사용자 정보 조회",
    description = """
        [호출 화면] 프로필 페이지 진입 시 호출.
        [권한 조건] 로그인한 사용자 본인 또는 ADMIN 역할만 접근 가능.
        [특이 동작] 비활성 계정은 404가 아닌 403 반환 (의도된 설계).
        """
)
```

에러 응답 코드 명시 기준:

| 코드 | 명시 필수 조건 |
|------|----------------|
| `400` | 요청 파라미터/바디 검증 실패 가능성이 있는 모든 엔드포인트 |
| `403` | 역할/권한 분기가 있는 엔드포인트 |
| `404` | ID 기반 조회 엔드포인트 |
| `409` | 중복 생성 가능성이 있는 엔드포인트 |
| `401`, `500` | `SwaggerConfig`가 자동 추가 — 개별 명시 불필요 |

DTO에 `@Schema` 어노테이션으로 `example` 값을 반드시 포함한다.

---

## 브랜치 및 커밋 규칙

브랜치:
```
master
  └── feature/SSACBE-{이슈번호}-{설명}
  └── fix/SSACBE-{이슈번호}-{설명}
```

커밋 메시지:
```
{type}: [{이슈번호}] {한국어 설명}

예: feat: [SSACBE-12] 유저 로그인 API 구현
```

| type | 의미 |
|------|------|
| `feat` | 새 기능 |
| `fix` | 버그 수정 |
| `refactor` | 동작 변경 없는 리팩토링 |
| `docs` | 문서만 변경 |
| `test` | 테스트만 추가/변경 |
| `chore` | 빌드, 설정 변경 |

---

## 커밋 전 체크리스트

```
[ ] ./gradlew build -x test    # 컴파일 오류 없음
[ ] ./gradlew test             # 테스트 전체 통과 (ArchUnit 포함)
[ ] ./gradlew checkstyleMain   # 스타일 위반 없음
[ ] 새 레이어 의존성 추가 시 docs/architecture.md 업데이트
[ ] 새 설계 결정 시 docs/decisions/ 에 ADR 추가
```

---

## 금지 행동

- `master` 브랜치 직접 커밋
- `controller` → `repository` 직접 의존 (service 레이어 우회)
- `domain` 패키지에서 외부 레이어 클래스 import
- 환경변수·비밀키를 소스코드에 하드코딩
- `@Transactional`을 `controller`에 붙이는 것
- docs/ 없이 아키텍처 결정을 구두로만 처리

---

## 모르는 것이 있을 때

- 레이어/의존성 → `docs/architecture.md`
- 네이밍/포맷 → `docs/conventions.md`
- 이미 내린 결정 → `docs/decisions/`
- 온보딩/환경 설정 → `docs/onboarding.md`
- 현재 기술 부채 → `docs/quality.md`

답을 찾지 못했다면 임의로 결정하지 말고 PR 설명란에 `[QUESTION]` 태그로 질문을 남겨라.
