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
STEP 1. compileJava 실행
STEP 2. test 실행
STEP 3. 결과 분석 및 오류 수정
STEP 4. 커버리지 검증
STEP 5. 결과 기록

---

## STEP 1. compileJava 실행

아래 명령어를 실행한다:
```
./gradlew compileJava
```

### 성공 시
→ "BUILD SUCCESSFUL" 확인
→ STEP 2로 진행

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

→ 오류 수정 완료 후 STEP 1 재실행
→ 3회 재시도 후에도 실패 시 사용자에게 보고

---

## STEP 2. test 실행

아래 명령어를 실행한다:
```
./gradlew test
```

### 성공 시
→ "BUILD SUCCESSFUL" 확인
→ 전체 테스트 통과 수 확인
→ STEP 3으로 진행

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

→ STEP 3으로 진행하여 오류를 수정한다

---

## STEP 3. 결과 분석 및 오류 수정

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
→ 모든 테스트 통과 확인 후 STEP 4로 진행

---

## STEP 4. 커버리지 검증

아래 명령어를 실행한다:
```
./gradlew jacocoTestReport jacocoTestCoverageVerification
```

### 커버리지 기준
- 서비스 레이어 Line Coverage: 70% 이상
- 측정 대상 패키지: com.ssac.*.service.*

### 성공 시
→ "BUILD SUCCESSFUL" 확인
→ STEP 5로 진행

### 실패 시 (70% 미달)
→ 커버리지 부족 클래스 / 메서드를 확인한다
  build/reports/jacoco/test/html/index.html 참고

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

→ 테스트 추가 완료 후 STEP 4 재실행
→ 70% 이상 확인 후 STEP 5로 진행

---

## STEP 5. 결과 기록

모든 단계 통과 후 아래 형식으로 결과를 출력한다:

## 빌드 / 테스트 실행 결과

### 실행 명령어
- ./gradlew compileJava → ✅ BUILD SUCCESSFUL
- ./gradlew test        → ✅ BUILD SUCCESSFUL
- ./gradlew jacocoTestCoverageVerification → ✅ PASS

### 테스트 결과
- 전체 테스트 수 : N개
- 통과           : N개
- 실패           : 0개
- 건너뜀         : N개

### 커버리지 결과
- 서비스 레이어 전체: N%
- OnboardingService : N%
- UserService       : N%

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
