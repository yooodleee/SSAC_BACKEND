# New Feature Protocol (BE)

## 역할
신규 기능 구현 시 레이어별 구현 순서와 컨벤션을 제공한다.  
트리거: `sc-structure-check.md` 완료 직후 자동 실행

## 구현 순서
STEP 1(Entity) → 2(Migration) → 3(Repository) → 4(Service) → 5(Controller) → 6(DTO) → 7(Contract) → 8(테스트)

> DB 변경 없음: STEP 1/2 건너뜀. 필드 추가만: STEP 1을 Entity 수정으로 처리 후 STEP 2 진행.

---

## STEP 1. Entity / Domain
패키지: `src/main/java/com/ssac/ssacbackend/domain/{도메인명}/`

필수 어노테이션: `@Entity` `@Table(name="snake_case_복수형")` `@Getter` `@NoArgsConstructor(access = PROTECTED)`  
감사 필드 사용 시: `@EntityListeners(AuditingEntityListener.class)`

필드 규칙:
- PK: `@Id` + `@GeneratedValue(strategy = IDENTITY)`
- 생성/수정: `@CreatedDate` / `@LastModifiedDate` + `LocalDateTime`
- 컬럼명: snake_case (`@Column(name = "user_id")`) / NOT NULL: `nullable = false`
- DB 예약어 컬럼명 금지: `type`, `order`, `group`, `value`

객체 생성: 정적 팩토리 메서드(`of()`) 또는 `@Builder` 중 하나 선택  
완료 기준: `./gradlew compileJava` → BUILD SUCCESSFUL

---

## STEP 2. Flyway Migration
파일: `src/main/resources/db/migration/V{N}__{설명}.sql`  
버전 확인: `Glob: src/main/resources/db/migration/V*.sql` → 최대 번호 + 1

**금지 문법 (MySQL 미지원):** `ADD COLUMN IF NOT EXISTS` / `CREATE INDEX IF NOT EXISTS`

```sql
-- 테이블 생성
CREATE TABLE IF NOT EXISTS table_name (...);

-- 컬럼 추가 (information_schema 조건부 패턴)
SELECT COUNT(*) INTO @col_exists FROM information_schema.columns
WHERE table_schema=DATABASE() AND table_name='t' AND column_name='col';
SET @sql=IF(@col_exists=0,'ALTER TABLE t ADD COLUMN col VARCHAR(255)','SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 인덱스 추가 (information_schema 조건부 패턴)
SELECT COUNT(*) INTO @idx_exists FROM information_schema.statistics
WHERE table_schema=DATABASE() AND table_name='t' AND index_name='idx_col';
SET @sql=IF(@idx_exists=0,'CREATE INDEX idx_col ON t(col)','SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 데이터 삽입
INSERT IGNORE INTO table_name (col1) VALUES (val1);
```

검증: `grep -rn "ADD COLUMN IF NOT EXISTS\|CREATE INDEX IF NOT EXISTS" src/main/resources/db/migration/` → 출력 없음 확인  
완료 기준: `./gradlew test` → FlywayException 없음

---

## STEP 3. Repository
패키지: `src/main/java/com/ssac/ssacbackend/repository/`

- `JpaRepository<Entity, ID>` 상속
- 단순 조회: Spring Data JPA 네이밍 (`findBy`, `existsBy`, `countBy`)
- 복잡 쿼리: `@Query(JPQL)` 사용 / 네이티브 SQL 지양
- 벌크 수정: `@Modifying` + `@Transactional` 함께 선언

---

## STEP 4. Service
패키지: `src/main/java/com/ssac/ssacbackend/service/`  
클래스 선언: `@Slf4j @Service @RequiredArgsConstructor`

트랜잭션: 조회 → `@Transactional(readOnly = true)` / 변경 → `@Transactional`

예외 처리 (직접 `RuntimeException` throw 금지):

| 상황 | 클래스 | HTTP |
|------|--------|------|
| 리소스 없음 | `NotFoundException(ErrorCode.XXX)` | 404 |
| 잘못된 입력 | `BadRequestException(ErrorCode.XXX)` | 400 |
| 인증 필요 | `UnauthorizedException(ErrorCode.XXX)` | 401 |
| 권한 없음 | `ForbiddenException(ErrorCode.XXX)` | 403 |
| 중복/충돌 | `ConflictException(ErrorCode.XXX)` | 409 |

신규 ErrorCode: 기존으로 표현 불가한 경우에만 추가 / 도메인 그룹에 배치 / 번호 = 마지막 번호 + 1  
로깅: 핵심 파라미터만 기록 / 민감 정보(토큰·이메일) 금지 / GlobalExceptionHandler가 WARN 자동 기록하므로 Service에서 비즈니스 예외 중복 로깅 금지  
완료 기준: `./gradlew test` → 신규 메서드 단위 테스트 통과

---

## STEP 5. Controller
패키지: `src/main/java/com/ssac/ssacbackend/controller/`  
클래스 선언: `@Slf4j @RestController @RequestMapping("/api/v1/{도메인}") @RequiredArgsConstructor @Tag(...)`

SecurityConfig 반영 (신규 API 추가 시 필수):
- 공개 API → `SecurityConfig.PUBLIC_PATHS`에 경로 추가
- 인증 필요 API → PUBLIC_PATHS에 추가하지 않음 / `Authentication` 파라미터로 사용자 추출

금지: Repository 직접 주입 / 비즈니스 로직 작성 / `@Transactional` 선언 / refreshToken을 response body에 포함 (→ CookieUtils 사용)  
완료 기준: `./gradlew compileJava` → BUILD SUCCESSFUL

---

## STEP 6. DTO
위치: `dto/request/` (요청) / `dto/response/` (응답) / `dto/` 루트 (내부 통신용)

- Java `record` 타입 사용 (일반 class 금지)
- Request: 필수 필드에 Bean Validation 적용 (`@NotBlank`, `@NotNull` 등)
- Response: Entity 직접 포함 금지 (ID / primitive 값만)
- boolean 필드 직렬화 명시 필요 시: `@JsonProperty("isCompleted")`

완료 기준: `./gradlew compileJava` → BUILD SUCCESSFUL

---

## STEP 7. Contract 갱신
API / ErrorCode 변경 없으면 건너뜀 → STEP 8로 진행.

- **API 변경 시**: Controller에 `@Operation(summary, description)` + `@ApiResponses` 작성
  - description 형식: `[호출 화면]` / `[권한 조건]` / `[특이 동작]`
  - `docs/api/swagger.json`은 CI 자동 생성 — 직접 편집 금지 (ADR-004)
- **ErrorCode 추가 시**: `contract-sync.md` STEP 1 절차에 따라 `contract/error-contract.yml` 갱신

---

## STEP 8. 테스트

### Service 단위 테스트
```java
@ExtendWith(MockitoExtension.class)
class XxxServiceTest {
    @Mock XxxRepository xxxRepository;
    @InjectMocks XxxService xxxService;

    @Test @DisplayName("한국어 설명")
    void 메서드명_한국어() {
        given(xxxRepository.findById(1L)).willReturn(Optional.of(mock));
        XxxResponse res = xxxService.getXxx(1L);
        assertThat(res.id()).isEqualTo(1L);
    }
}
```

### Controller 단위 테스트 (신규 Controller마다 의무)
- 반드시 포함: 정상 케이스(상태코드 검증) + 서비스 호출 `verify`
- `CookieProperties` 의존 Controller: `@InjectMocks` 대신 생성자 직접 호출 (`new XxxController(service, new CookieProperties())`)

```java
@ExtendWith(MockitoExtension.class)
class XxxControllerTest {
    @Mock XxxService xxxService;
    @InjectMocks XxxController controller;

    @Test @DisplayName("정상 케이스 — 200 반환")
    void 정상_케이스() {
        Authentication auth = mock(Authentication.class);
        given(auth.getName()).willReturn("user@test.com");
        given(xxxService.getXxx("user@test.com")).willReturn(null);

        ResponseEntity<?> result = controller.getXxx(auth);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(xxxService).getXxx("user@test.com");
    }
}
```

인증/인가 핵심 API: `RbacIntegrationTest` 패턴 참고 (`@SpringBootTest` + `@AutoConfigureMockMvc`)

### 커버리지 기준

| 대상 | 기준 |
|------|------|
| 서비스 레이어 전체 | Line ≥ 70% |
| 개별 서비스 클래스 | Line ≥ 50% |
| Controller 레이어 전체 | Line ≥ 60% |
| 개별 Controller 클래스 | Line ≥ 40% |

완료 기준: `testing.md` 전체 실행 → 모두 통과 후 `self-diagnose.md` 실행

---

## 구현 완료 체크리스트

| STEP | 완료 기준 |
|------|---------|
| 1. Entity | compileJava BUILD SUCCESSFUL |
| 2. Migration | FlywayException 없음 |
| 3. Repository | compileJava BUILD SUCCESSFUL |
| 4. Service | 단위 테스트 통과 |
| 5. Controller | compileJava BUILD SUCCESSFUL |
| 6. DTO | compileJava BUILD SUCCESSFUL |
| 7. Contract | 변경 시 contract-sync.md 완료 |
| 8. 테스트 | testing.md 전체 통과 + JaCoCo ≥ 70% |

→ 전체 완료 후 `self-diagnose.md` 실행
