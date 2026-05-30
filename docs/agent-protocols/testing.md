# Testing Protocol (BE)

## 역할
구현 완료 후 반드시 아래 절차를 수행하여
빌드 및 테스트 성공을 확인한다.
사용자가 요청하지 않아도 자동으로 실행한다.

## 트리거 조건 (자동 실행)
- 코드 구현이 완료되었을 때
- 파일 수정 / 추가 / 삭제가 발생했을 때
- 사용자가 "완료", "구현했어", "다 됐어" 언급 시
- Flyway 마이그레이션 파일이 수정되었을 때
- 의존성(build.gradle)이 변경되었을 때

## 실행 순서
STEP 1. checkstyle 실행
STEP 2. compileJava 실행
STEP 3. test 실행
STEP 4. 결과 분석 및 오류 수정
STEP 5. 커버리지 검증
STEP 6. 결과 기록

---

## STEP 1. checkstyle 실행

> Railway 빌드는 `./gradlew build -x test`를 사용하므로 Checkstyle이 실행된다.
> Checkstyle 위반은 테스트보다 먼저 빌드를 실패시키므로 가장 먼저 검증한다.

아래 명령어를 실행한다:
```
./gradlew checkstyleMain checkstyleTest --no-daemon
```

### 성공 시
→ "BUILD SUCCESSFUL" 확인
→ STEP 2로 진행

### 실패 시
→ 보고서 위치: `build/reports/checkstyle/main.xml`, `build/reports/checkstyle/test.xml`
→ 아래 위반 유형별 조치를 수행한다

| 위반 유형 | 원인 | 조치 |
|---------|-----|-----|
| Unused import | 사용하지 않는 import 구문 | 해당 import 줄 삭제 |
| Missing Javadoc | public 클래스/메서드에 Javadoc 없음 | Javadoc 추가 |
| LineLength | 한 줄이 설정 길이 초과 | 줄 분리 |
| WhitespaceAround | 연산자/키워드 주변 공백 없음 | 공백 추가 |
| NeedBraces | if/for 등에 중괄호 없음 | 중괄호 추가 |

→ 수정 완료 후 STEP 1 재실행
→ 3회 재시도 후에도 실패 시 사용자에게 보고

---

## STEP 2. compileJava 실행

아래 명령어를 실행한다:
```
./gradlew compileJava
```

### 성공 시
→ "BUILD SUCCESSFUL" 확인
→ STEP 3으로 진행

### 실패 시
→ 빌드 오류 로그를 즉시 분석한다
→ log-diagnose.md STEP 2 분류표 기준으로 원인 분류
→ 아래 오류 유형별 조치를 수행한다

| 오류 패턴 | 원인 | 조치 |
|---------|-----|-----|
| cannot find symbol | import 누락 / 오타 | 해당 파일 import 확인 |
| incompatible types | 타입 불일치 | 반환 타입 / 파라미터 타입 확인 |
| method not found | 메서드 미존재 | 메서드명 / 시그니처 확인 |
| package does not exist | 의존성 누락 | build.gradle 의존성 추가 |
| class is not abstract | 인터페이스 미구현 | 미구현 메서드 확인 |

→ 오류 수정 완료 후 STEP 2 재실행
→ 3회 재시도 후에도 실패 시 사용자에게 보고

---

## STEP 3. test 실행

아래 명령어를 실행한다:
```
./gradlew test
```

### 성공 시
→ "BUILD SUCCESSFUL" 확인
→ 전체 테스트 통과 수 확인
→ STEP 5로 진행

### 실패 시
→ 실패한 테스트 목록을 즉시 확인한다
  build/reports/tests/test/index.html 참고

→ 아래 형식으로 실패 내용을 분석한다:
```
[실패 테스트 분석]
- 테스트명  : {테스트 클래스}.{테스트 메서드}
- 실패 원인 : expected: X but was: Y
- 관련 파일 : src/main/java/.../XxxService.java
- 조치 방향 : (원인 분석 후 작성)
```

→ STEP 4로 진행하여 오류를 수정한다

---

## STEP 4. 결과 분석 및 오류 수정

테스트 실패 유형별 원인과 조치:

### AssertionError (단언 실패)
원인: 예상값과 실제값 불일치
조치:
□ 구현 로직이 SC 기준과 일치하는지 확인
□ 테스트 기댓값이 올바른지 확인
□ Mock 설정이 올바른지 확인

### NullPointerException
원인: null 참조 또는 Mock 미설정
조치:
□ @MockBean / @Mock 누락 여부 확인
□ when(...).thenReturn(...) 설정 누락 확인
□ 초기화 누락 확인

### DataIntegrityViolationException
원인: DB 제약 조건 위반
조치:
□ NOT NULL 컬럼 값 누락 확인
□ 유니크 제약 중복 확인
□ 외래 키 참조 오류 확인

### FlywayException
원인: 마이그레이션 스크립트 오류
조치:
□ IF NOT EXISTS 누락 여부 확인
□ SQL 문법 오류 확인
□ 마이그레이션 버전 순번 확인

### BeanCreationException
원인: Spring Context 생성 실패
조치:
□ @Service / @Repository 어노테이션 누락 확인
□ 의존성 순환 참조 확인
□ application-test.yml 설정 확인

→ 수정 완료 후 STEP 1부터 재실행
→ 모든 테스트 통과 확인 후 STEP 5로 진행

---

## STEP 5. 커버리지 검증

아래 명령어를 실행한다:
```
./gradlew jacocoTestReport jacocoTestCoverageVerification
```

### 커버리지 기준 (4-Rule 검증)

**Rule 1. 서비스 레이어 전체 집계**
→ 측정 대상: com/ssac/*/service 패키지
→ 기준: Line Coverage 70% 이상

**Rule 2. 개별 서비스 클래스 최소 커버리지**
→ 측정 대상: com/ssac/*/service/* 클래스 (인터페이스·제외 등록 클래스 제외)
→ 기준: Line Coverage 50% 이상
→ 목적: 0% 클래스가 집계에 은폐되는 것을 방지

**Rule 3. Controller 레이어 전체 집계**
→ 측정 대상: com/ssac/*/controller 패키지
→ 기준: Line Coverage 60% 이상
→ 단계적 상향 계획: 1단계 60% → 2단계 70%

**Rule 4. 개별 Controller 클래스 최소 커버리지**
→ 측정 대상: com/ssac/*/controller/* 클래스 (제외 등록 클래스 제외)
→ 기준: Line Coverage 40% 이상
→ 제외: DevAuthController (@Profile("!prod")), KakaoOAuthController (Swagger 더미)

### 성공 시
→ Rule 1~4 모두 "BUILD SUCCESSFUL" 확인
→ STEP 6으로 진행

### Rule 1 실패 시 (전체 70% 미달)
→ build/reports/jacoco/test/html/index.html 확인
→ 전체 커버리지가 낮은 패키지 탐색
→ 해당 서비스 테스트 추가

→ 아래 형식으로 부족한 항목을 분석한다:
```
[커버리지 부족 분석]
- 클래스   : XxxService
- 현재 커버리지: 58%
- 미테스트 메서드: calculateLevel(), skipOnboarding()
- 추가할 테스트:
  □ calculateLevel() 정상 동작 테스트
  □ skipOnboarding() 기본값 SEED 설정 테스트
```

### Rule 2 실패 시 (개별 클래스 50% 미달)
→ 해당 클래스가 비즈니스 로직을 포함하는가?
    포함 → 테스트 작성 필요 (분류 A)
    미포함 → excludes 목록에 근거와 함께 등록 (분류 B)

→ 배치·스케줄러 클래스는 분류 C로 jacocoTestCoverageVerification excludes에 등록

### 의도적 제외 클래스 등록 절차
1. build.gradle `jacocoTestCoverageVerification` Rule 2 `excludes`에 추가
2. build.gradle `jacocoTestReport` `exclude` 패턴에 추가 (필요 시)
3. 아래 제외 클래스 목록 테이블 갱신
4. 제외 근거가 중요한 경우 ADR 작성

→ 테스트 추가 또는 excludes 등록 완료 후 STEP 5 재실행
→ Rule 1~4 모두 통과 후 STEP 6으로 진행

### Rule 3 실패 시 (Controller 전체 60% 미달)
→ 미테스트 Controller 목록 확인 (docs/debug-log.md P1~P3 항목)
→ 우선순위 순으로 Controller 테스트 추가

### Rule 4 실패 시 (Controller 클래스 40% 미달)
→ 해당 Controller를 단위 테스트로 추가 작성
→ new-feature.md STEP 8 Controller 테스트 패턴 참고
→ 단순 위임 Controller이면 DevAuthController / KakaoOAuthController 패턴으로 excludes 등록

---

### JaCoCo 측정 제외 클래스 목록

아래 클래스는 테스트 커버리지 측정에서
제외된다:

| 패턴 | 제외 이유 |
|-----|---------|
| `**/config/**` | Bean 등록만 수행 / 로직 없음 |
| `**/common/response/**` | 공통 응답 구조 / 단순 래퍼 |
| `**/common/exception/**` | 예외 정의 / 로직 없음 |
| `**/domain/**` | JPA 엔티티·Enum / Getter·Setter 자동 생성 |
| `**/dto/**` | 데이터 운반 객체 / 로직 없음 |
| `**/component/**` | 단순 위임 컴포넌트 |
| `**/*Application*` | Spring Boot 진입점 |
| `**/service/ViewCountStore*` | 인터페이스 / 측정 불가 |
| `**/service/TokenStore*` | 인터페이스 / 측정 불가 |
| `**/service/ReissueResult*` | 레코드 (데이터 운반 객체) |
| `GuestDataCleanupService` | 스케줄러 배치 / 추후 고도화 (분류 C) |
| `ErrorLogBatchService` | 스케줄러 배치 / 추후 고도화 (분류 C) |

→ 위 목록 외 클래스를 제외하려면
  ADR을 작성하여 근거를 기록해야 한다.

---

## STEP 6. 결과 기록

모든 단계 통과 후 아래 형식으로 결과를 출력한다:

## 빌드 / 테스트 실행 결과

### 실행 명령어
- ./gradlew checkstyleMain checkstyleTest → ✅ BUILD SUCCESSFUL
- ./gradlew compileJava → ✅ BUILD SUCCESSFUL
- ./gradlew test        → ✅ BUILD SUCCESSFUL
- ./gradlew jacocoTestCoverageVerification → ✅ PASS

### 테스트 결과
- 전체 테스트 수 : N개
- 통과           : N개
- 실패           : 0개
- 건너뜀         : N개

### 커버리지 결과
- Rule 1 (서비스 레이어 전체): N%  → ✅ / ❌
- Rule 2 (개별 서비스 클래스 50% 이상):
  - OnboardingService : N%
  - UserService       : N%
  - (50% 미달 클래스 있으면 명시)
- Rule 3 (Controller 레이어 전체): N%  → ✅ / ❌
- Rule 4 (개별 Controller 클래스 40% 이상):
  - AuthController    : N%
  - UserController    : N%
  - (40% 미달 클래스 있으면 명시)

→ 구현 완료 확인. self-diagnose.md를 실행합니다.

---

실패가 발생한 경우에는 아래 형식으로 기록한다:

## 빌드 / 테스트 실행 결과

### 실행 명령어
- ./gradlew compileJava → ✅ BUILD SUCCESSFUL
- ./gradlew test        → ❌ BUILD FAILED

### 실패 내용
- 실패 테스트 : OnboardingServiceTest.레벨판정_SEED
- 실패 원인   : expected: SEED but was: SPROUT
- 수정 내용   : 레벨 판정 점수 기준 수정 (3점 이하 → SEED)
- 재실행 결과 : ✅ BUILD SUCCESSFUL

→ 수정 완료 후 재실행 성공 확인.
