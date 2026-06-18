# Testing Protocol (BE)

## 역할
구현 완료 후 빌드·테스트·커버리지를 순서대로 검증한다. 사용자 요청 없이 자동 실행한다.

## 트리거 조건
- .java 파일 수정/추가/삭제 완료 시
- `build.gradle` 의존성 변경 시
- Flyway 마이그레이션 파일 수정/추가 시
- 사용자가 "완료", "구현했어" 등 완료 언급 시

## 실행 명령 (순서대로)
```bash
./gradlew checkstyleMain checkstyleTest --no-daemon
./gradlew compileJava
./gradlew test
./gradlew jacocoTestReport jacocoTestCoverageVerification
```
> 한 번에 실행: `bash scripts/run-tests.sh`  
> 각 단계에서 실패 시 → 즉시 수정 후 해당 단계부터 재실행 (3회 실패 시 사용자 보고)

---

## STEP 1. Checkstyle 실패 시

> Railway 빌드는 `./gradlew build -x test` 사용 → Checkstyle 위반이 배포를 막는다.  
> 보고서 위치: `build/reports/checkstyle/main.xml`, `build/reports/checkstyle/test.xml`

| 위반 유형 | 원인 | 조치 |
|---------|------|------|
| `UnusedImports` | 미사용 import | 해당 import 줄 삭제 |
| `MissingJavadoc` | public 클래스/메서드 Javadoc 없음 | Javadoc 추가 |
| `LineLength` | 한 줄 길이 초과 | 줄 분리 |
| `WhitespaceAround` | 연산자/키워드 주변 공백 없음 | 공백 추가 |
| `NeedBraces` | if/for 등 중괄호 없음 | 중괄호 추가 |

---

## STEP 2. compileJava 실패 시

| 오류 패턴 | 원인 | 조치 |
|---------|------|------|
| `cannot find symbol` | import 누락 / 오타 | 해당 파일 import 확인 |
| `incompatible types` | 타입 불일치 | 반환 타입 / 파라미터 타입 확인 |
| `method not found` | 메서드 미존재 | 메서드명 / 시그니처 확인 |
| `package does not exist` | 의존성 누락 | `build.gradle` 의존성 추가 |
| `class is not abstract` | 인터페이스 미구현 | 미구현 메서드 확인 |

---

## STEP 3. test 실패 시

실패 목록 위치: `build/reports/tests/test/index.html`

| 오류 유형 | 원인 | 조치 |
|---------|------|------|
| `AssertionError` | 예상값 / 실제값 불일치 | SC 기준 확인 / Mock 설정 검토 |
| `NullPointerException` | Mock 미설정 / null 참조 | `@Mock` 누락 / `given(...).willReturn(...)` 확인 |
| `DataIntegrityViolationException` | DB 제약 조건 위반 | NOT NULL / 유니크 / FK 확인 |
| `FlywayException` | 마이그레이션 오류 | `IF NOT EXISTS` 누락 / SQL 문법 / 버전 순번 확인 |
| `BeanCreationException` | Spring Context 생성 실패 | 어노테이션 누락 / 순환 참조 / `application-test.yml` 확인 |

수정 완료 후 → STEP 1부터 재실행

---

## STEP 4. 커버리지 검증

### 4-Rule 기준

| Rule | 대상 | 기준 |
|------|------|------|
| Rule 1 | 서비스 레이어 전체 (`com/ssac/*/service`) | Line ≥ 70% |
| Rule 2 | 개별 서비스 클래스 | Line ≥ 50% |
| Rule 3 | Controller 레이어 전체 (`com/ssac/*/controller`) | Line ≥ 90% |
| Rule 4 | 개별 Controller 클래스 | Line ≥ 70% |

### Rule 실패 시 조치

- **Rule 1/3 미달**: `build/reports/jacoco/test/html/index.html`에서 커버리지 낮은 클래스 확인 → 테스트 추가
- **Rule 2 미달**: 비즈니스 로직 포함 클래스 → 테스트 추가 / 배치·스케줄러·인터페이스 → 아래 제외 목록에 등록
- **Rule 4 미달**: Controller 단위 테스트 추가 (`new-feature.md` STEP 8 패턴 참고) / 단순 위임이면 제외 목록 등록

### 제외 클래스 등록 절차
1. `build.gradle` `jacocoTestCoverageVerification` Rule 2/4 `excludes`에 추가
2. 아래 제외 클래스 목록에 근거와 함께 추가
3. 중요 의사결정이면 `adr-create.md` 실행

### JaCoCo 제외 클래스 목록

| 패턴 | 제외 이유 |
|------|---------|
| `**/config/**` | Bean 등록만 수행 / 로직 없음 |
| `**/common/response/**` | 공통 응답 구조 / 단순 래퍼 |
| `**/common/exception/**` | 예외 정의 / 로직 없음 |
| `**/domain/**` | JPA 엔티티·Enum / Getter·Setter 자동 생성 |
| `**/dto/**` | 데이터 운반 객체 / 로직 없음 |

| `**/*Application*` | Spring Boot 진입점 |
| `**/service/ViewCountStore*` | 인터페이스 / 측정 불가 |
| `**/service/TokenStore*` | 인터페이스 / 측정 불가 |
| `**/service/ReissueResult*` | 레코드 (데이터 운반 객체) |

> 위 목록 외 클래스 제외 시 ADR 작성 필수

---

## STEP 5. 결과 출력

모든 단계 통과 시:
```
[빌드/테스트 결과]
checkstyle     : ✅ BUILD SUCCESSFUL
compileJava    : ✅ BUILD SUCCESSFUL
test           : ✅ N개 통과 / 0개 실패
Rule 1 (서비스 전체)    : N% ✅
Rule 2 (서비스 클래스)  : 최저 N% ✅
Rule 3 (Controller 전체): N% ✅
Rule 4 (Controller 클래스): 최저 N% ✅

→ self-diagnose.md 실행
```

실패 발생 시:
```
[빌드/테스트 결과]
{실패 단계} : ❌ {실패 내용 한 줄}
수정 내용   : {수정 사항}
재실행 결과 : ✅ BUILD SUCCESSFUL
```
