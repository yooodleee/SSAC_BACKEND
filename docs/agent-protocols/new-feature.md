# New Feature Protocol (BE)

## 역할
신규 기능 구현 시 레이어별 올바른 구현 순서와
각 레이어에서 반드시 준수해야 할 컨벤션을 제공한다.

## 트리거 조건 (자동 실행)
- sc-structure-check.md 실행 완료 직후

## 구현 순서
STEP 1. Entity / Domain
STEP 2. Flyway Migration
STEP 3. Repository
STEP 4. Service
STEP 5. Controller
STEP 6. DTO
STEP 7. Contract 갱신
STEP 8. 테스트

> DB 변경이 없는 기능이면 STEP 1 / STEP 2를 건너뛰고 STEP 3부터 시작한다.
> 기존 엔티티에 필드 추가만 필요하면 STEP 1을 Entity 수정으로 처리하고 STEP 2로 진행한다.

---

## STEP 1. Entity / Domain

### 패키지 위치
```
src/main/java/com/ssac/ssacbackend/domain/{도메인명}/
```

### 필수 어노테이션
```java
@Entity
@Table(name = "테이블명")               // snake_case 복수형
@EntityListeners(AuditingEntityListener.class)  // 감사 필드 사용 시
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
```

### 필드 규칙
□ PK: `@Id` + `@GeneratedValue(strategy = GenerationType.IDENTITY)`
□ 생성 시각: `@CreatedDate` + `LocalDateTime createdAt`
□ 수정 시각: `@LastModifiedDate` + `LocalDateTime updatedAt`
□ 컬럼명: snake_case (`@Column(name = "user_id")`)
□ NOT NULL 컬럼: `@Column(nullable = false)`
□ DB 예약어 컬럼명 금지 (`type`, `order`, `group`, `value` 등)

### 객체 생성 패턴
정적 팩토리 메서드 또는 @Builder 중 하나를 선택한다.

```java
// 정적 팩토리 메서드 방식 (권장 — 의미 있는 이름 부여 가능)
public static User of(String email, OAuthProvider provider) {
    User user = new User();
    user.email = email;
    user.provider = provider;
    return user;
}

// @Builder 방식 (필드가 많은 경우)
@Builder
private User(String email, OAuthProvider provider) { ... }
```

### 완료 기준
```
./gradlew compileJava
→ BUILD SUCCESSFUL 확인 후 STEP 2로 진행
```

---

## STEP 2. Flyway Migration

### 파일 위치 및 네이밍
```
src/main/resources/db/migration/V{N}__{설명}.sql
```
- N: 현재 최신 버전 + 1
- 설명: 영문 소문자, 언더스코어 구분 (예: `V21__add_user_level_column.sql`)

### 최신 버전 확인 방법
```
Glob: src/main/resources/db/migration/V*.sql
→ 가장 큰 번호 확인 → 다음 번호 사용
```

### 멱등성 보장 규칙

**테이블 생성**
```sql
CREATE TABLE IF NOT EXISTS table_name ( ... );
```

**컬럼 추가** — MySQL은 IF NOT EXISTS 미지원 → information_schema 패턴 사용
```sql
SELECT COUNT(*) INTO @col_exists
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name   = 'target_table'
  AND column_name  = 'new_column';
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE target_table ADD COLUMN new_column VARCHAR(255)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
```

**인덱스 추가** — MySQL은 IF NOT EXISTS 미지원 → information_schema 패턴 사용
```sql
SELECT COUNT(*) INTO @idx_exists
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name   = 'target_table'
  AND index_name   = 'idx_target_column';
SET @sql = IF(@idx_exists = 0,
    'CREATE INDEX idx_target_column ON target_table (column_name)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
```

**데이터 삽입**
```sql
INSERT IGNORE INTO table_name (col1, col2) VALUES (val1, val2);
-- 또는
INSERT INTO table_name (col1, col2) VALUES (val1, val2)
ON DUPLICATE KEY UPDATE col1 = col1;
```

### 절대 금지 문법 (MySQL 미지원)
```sql
❌ ALTER TABLE ... ADD COLUMN IF NOT EXISTS
❌ CREATE INDEX IF NOT EXISTS
```

### 작성 후 검증
```bash
grep -rn "ADD COLUMN IF NOT EXISTS\|CREATE INDEX IF NOT EXISTS" \
  src/main/resources/db/migration/
# → 출력 없음 확인

grep -rn "CREATE TABLE " src/main/resources/db/migration/ | grep -v "IF NOT EXISTS"
# → 출력 없음 확인
```

### 완료 기준
```
./gradlew test
→ FlywayException / SchemaManagementException 없음 확인 후 STEP 3으로 진행
```

---

## STEP 3. Repository

### 패키지 위치
```
src/main/java/com/ssac/ssacbackend/repository/
```

### 기본 구조
```java
public interface XxxRepository extends JpaRepository<Xxx, Long> {

    Optional<Xxx> findByEmail(String email);

    @Query("SELECT x FROM Xxx x WHERE x.status = :status")
    List<Xxx> findAllByStatus(@Param("status") Status status);
}
```

### 규칙
□ `JpaRepository<Entity, ID>` 상속
□ 단순 조건 조회: Spring Data JPA 메서드 네이밍 활용 (`findBy`, `existsBy`, `countBy`)
□ 복잡한 쿼리: `@Query(JPQL)` 사용
□ 네이티브 SQL: 지양 — 부득이한 경우 `@Query(nativeQuery = true)` 사용
□ 추상화가 필요한 경우: `Interface → JpaImpl` 패턴 적용
    예) `TokenStore` (interface) → `JpaTokenStore` (구현체)
□ 벌크 수정 쿼리: `@Modifying` + `@Transactional` 함께 선언

### 완료 기준
```
./gradlew compileJava
→ BUILD SUCCESSFUL 확인 후 STEP 4로 진행
```

---

## STEP 4. Service

### 패키지 위치
```
src/main/java/com/ssac/ssacbackend/service/
```

### 클래스 선언
```java
@Slf4j
@Service
@RequiredArgsConstructor
public class XxxService {
    private final XxxRepository xxxRepository;
    ...
}
```

### 트랜잭션 규칙
```java
// 조회 전용 메서드
@Transactional(readOnly = true)
public XxxResponse getXxx(Long id) { ... }

// 데이터 변경 메서드
@Transactional
public XxxResponse createXxx(XxxRequest request) { ... }
```

### 예외 처리 규칙
아래 예외 클래스만 사용한다. 직접 RuntimeException을 throw하지 않는다.

| 상황 | 사용 클래스 | HTTP |
|------|-----------|------|
| 존재하지 않는 리소스 | `NotFoundException(ErrorCode.XXX)` | 404 |
| 잘못된 입력 | `BadRequestException(ErrorCode.XXX)` | 400 |
| 인증 필요 | `UnauthorizedException(ErrorCode.XXX)` | 401 |
| 권한 없음 | `ForbiddenException(ErrorCode.XXX)` | 403 |
| 중복 / 충돌 | `ConflictException(ErrorCode.XXX)` | 409 |

신규 ErrorCode 추가 기준:
- 기존 ErrorCode로 표현할 수 없는 명확한 비즈니스 오류일 때만 추가
- 추가 시 `ErrorCode.java` 주석의 도메인 구분에 맞게 그룹화
- 번호는 해당 도메인의 마지막 번호 + 1

### 로깅 규칙
```java
// 정상 흐름 — 핵심 파라미터만 기록
log.info("사용자 레벨 판정 완료: userId={}, level={}", userId, level);

// 경고 — 비즈니스 예외 (GlobalExceptionHandler가 별도 로깅하므로 Service에서 중복 로깅 금지)
// Service에서 throw만 하면 GlobalExceptionHandler가 WARN 로그를 자동 기록한다

// 민감 정보 로그 금지
❌ log.info("토큰: {}", refreshToken);
❌ log.info("이메일: {}", email);
```

### 완료 기준
```
./gradlew test
→ 서비스 레이어 신규 메서드 단위 테스트 통과
→ JaCoCo 서비스 레이어 70% 이상 (STEP 8에서 최종 확인)
```

---

## STEP 5. Controller

### 패키지 위치
```
src/main/java/com/ssac/ssacbackend/controller/
```

### 클래스 선언
```java
@Slf4j
@RestController
@RequestMapping("/api/v1/{도메인}")
@RequiredArgsConstructor
@Tag(name = "도메인명", description = "API 설명")
public class XxxController {
    private final XxxService xxxService;
}
```

### 메서드 구조
```java
@PostMapping
@Operation(summary = "요약", description = "상세 설명")
public ResponseEntity<ApiResponse<XxxResponse>> createXxx(
    @RequestBody @Valid XxxRequest request,
    Authentication authentication         // 인증 필요 시
) {
    XxxResponse response = xxxService.createXxx(request);
    return ResponseEntity.ok(ApiResponse.success(response));
}
```

### SecurityConfig 반영
신규 API 추가 시 인증 요구 수준을 반드시 확인한다.

```
공개 API (로그인 불필요):
→ SecurityConfig.PUBLIC_PATHS에 경로 추가

인증 필요 API:
→ PUBLIC_PATHS에 추가하지 않는다
→ Authentication 파라미터로 사용자 정보 추출
```

참고 파일: `src/main/java/com/ssac/ssacbackend/config/SecurityConfig.java`

### 금지 패턴
```java
❌ Controller에서 Repository 직접 주입
❌ Controller 메서드 내 비즈니스 로직 작성
❌ @Transactional을 Controller에 선언
❌ refreshToken을 response body에 포함 → CookieUtils 사용
```

### 완료 기준
```
./gradlew compileJava
→ BUILD SUCCESSFUL 확인 후 STEP 6으로 진행
```

---

## STEP 6. DTO

### 패키지 위치
```
src/main/java/com/ssac/ssacbackend/dto/request/   ← 요청 DTO
src/main/java/com/ssac/ssacbackend/dto/response/  ← 응답 DTO
```

### Request DTO
```java
public record XxxRequest(

    @NotBlank(message = "이름은 필수입니다.")
    String name,

    @NotNull(message = "타입은 필수입니다.")
    XxxType type

) {}
```

### Response DTO
```java
public record XxxResponse(

    Long id,
    String name,

    @JsonProperty("isCompleted")   // boolean 필드명 명시가 필요한 경우
    boolean isCompleted

) {}
```

### 규칙
□ Java `record` 타입 사용 — 일반 class 사용 금지
□ Request DTO: 모든 필수 입력 필드에 Bean Validation 어노테이션 적용
□ Response DTO: Entity를 직접 필드로 포함하지 않는다 (ID / primitive 값만 포함)
□ 내부 통신용 DTO (`TokenPair` 등)는 `dto/` 루트에 위치

### 완료 기준
```
./gradlew compileJava
→ BUILD SUCCESSFUL 확인 후 STEP 7로 진행
```

---

## STEP 7. Contract 갱신

**이 단계는 테스트 작성 전에 수행한다.**
Contract를 먼저 확정해야 테스트 케이스의 기대값이 명확해진다.

### 7-1. API 스펙 갱신
신규 또는 변경된 API가 있는 경우 `contract/api-spec.yaml`을 갱신한다.

갱신 항목:
```yaml
paths:
  /api/v1/{도메인}/{리소스}:
    post:
      summary: API 요약
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/XxxRequest'
      responses:
        '200':
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ApiResponse_XxxResponse'
```

### 7-2. ErrorCode 계약 갱신
신규 ErrorCode가 추가된 경우 `contract/error-contract.yml`을 갱신한다.

참고: `ErrorCode.java` 클래스 주석 — "코드 추가/수정 후 contract/error-contract.yml도 함께 갱신해야 한다"

### 완료 기준
```
API 변경 없음: 이 단계 건너뜀 → STEP 8로 진행
API 변경 있음: contract 파일 갱신 확인 후 STEP 8로 진행
```

---

## STEP 8. 테스트

### Service 단위 테스트
```java
@ExtendWith(MockitoExtension.class)
class XxxServiceTest {

    @Mock private XxxRepository xxxRepository;
    @InjectMocks private XxxService xxxService;

    @Test
    @DisplayName("정상 케이스 — 한국어로 작성")
    void 정상_케이스() {
        // given
        given(xxxRepository.findById(1L)).willReturn(Optional.of(mockXxx()));

        // when
        XxxResponse response = xxxService.getXxx(1L);

        // then
        assertThat(response.id()).isEqualTo(1L);
    }
}
```

### Controller 단위 테스트
MockMvc 대신 Controller를 직접 생성하여 호출한다.

```java
class XxxControllerTest {

    private XxxService xxxService;
    private XxxController controller;

    @BeforeEach
    void setUp() {
        xxxService = mock(XxxService.class);
        controller = new XxxController(xxxService);
    }

    @Test
    void 정상_케이스() {
        given(xxxService.getXxx(1L)).willReturn(new XxxResponse(1L, "name"));

        ResponseEntity<ApiResponse<XxxResponse>> result = controller.getXxx(1L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
```

### 통합 테스트
인증/인가가 핵심인 API는 `RbacIntegrationTest` 패턴을 참고한다.

```java
@SpringBootTest
@AutoConfigureMockMvc
class XxxIntegrationTest {
    @Autowired MockMvc mockMvc;

    @Test
    void 비인증_요청_401() throws Exception {
        mockMvc.perform(get("/api/v1/xxx/resource"))
            .andExpect(status().isUnauthorized());
    }
}
```

### 테스트 커버리지 기준
- 서비스 레이어 Line Coverage: **70% 이상**
- 측정 대상: `com.ssac.*.service.*`

### 테스트 완료 후 실행
```
testing.md 전체 실행
→ checkstyle → compileJava → test → jacocoTestCoverageVerification
→ 모두 통과 후 self-diagnose.md 실행
```

---

## 구현 완료 체크리스트

모든 STEP 완료 후 아래 항목을 최종 확인한다:

```
□ STEP 1. Entity    — compileJava 통과
□ STEP 2. Migration — FlywayException 없음
□ STEP 3. Repository — compileJava 통과
□ STEP 4. Service   — 단위 테스트 통과
□ STEP 5. Controller — compileJava 통과
□ STEP 6. DTO       — compileJava 통과
□ STEP 7. Contract  — api-spec.yaml / error-contract.yml 갱신 완료 (해당 시)
□ STEP 8. 테스트    — testing.md 전체 통과 + JaCoCo 70% 이상

→ 전체 완료 후 self-diagnose.md 실행
```
