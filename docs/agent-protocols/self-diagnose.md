# Self-Diagnose Protocol (BE)

## 역할
에이전트가 "구현 완료"를 선언하기 전, 스스로 놓친 항목이 없는지
코드 레벨에서 점검한다.
testing.md가 통과했더라도 이 점검을 반드시 수행한다.

## 트리거 조건 (자동 실행)
- testing.md 실행 성공 완료 직후

## 실행 순서
STEP 1. 레이어 책임 점검
STEP 2. 인증 / 보안 점검
STEP 3. 응답 구조 점검
STEP 4. Contract 갱신 점검
STEP 5. Redis 캐싱 점검
STEP 6. 결과 출력 및 완료 판단

---

## STEP 1. 레이어 책임 점검

이번 구현에서 추가하거나 수정한 Controller / Service 파일을 대상으로 확인한다.

### 1-1. Controller 점검
□ 비즈니스 로직이 Controller 메서드 내부에 작성되어 있지 않은가?
    → 조건 분기, 계산, DB 조회 로직이 Controller에 있으면 ❌
    → 모든 로직은 Service에 위임해야 한다

□ Repository를 Controller가 직접 주입받아 사용하고 있지 않은가?
    → @Autowired / 생성자 주입으로 Repository가 Controller에 있으면 ❌

□ @Transactional이 Controller 메서드에 선언되어 있지 않은가?
    → Controller에 @Transactional이 있으면 ❌
    → @Transactional은 Service 레이어에만 선언한다

### 1-2. Service 점검
□ 조회(읽기 전용) 메서드에 @Transactional(readOnly = true)가 선언되어 있는가?
    → SELECT만 수행하는 메서드에 readOnly = true 없으면 ⚠️

□ 데이터 변경(insert / update / delete) 메서드에 @Transactional이 선언되어 있는가?
    → 변경 메서드에 @Transactional 없으면 ❌

□ 외부 API 호출이 Service에서 이루어지는가?
    → Controller에서 외부 API(카카오, 네이버 등)를 직접 호출하면 ❌

---

## STEP 2. 인증 / 보안 점검

### 2-1. SecurityConfig 반영 점검
이번 구현에서 추가한 신규 API 경로를 SecurityConfig.PUBLIC_PATHS와 대조한다.

□ 인증이 필요한 신규 API가 PUBLIC_PATHS에 잘못 등록되어 있지 않은가?
    → 인증 필요 API가 PUBLIC_PATHS에 있으면 ❌

□ 공개 접근이 허용되어야 하는 신규 API가 PUBLIC_PATHS에 누락되어 있지 않은가?
    → 공개 API인데 PUBLIC_PATHS에 없으면 ❌

참고 — 현재 PUBLIC_PATHS 위치:
    src/main/java/com/ssac/ssacbackend/config/SecurityConfig.java

### 2-2. 민감 정보 노출 점검
□ refreshToken이 response body에 반환되고 있지 않은가?
    → Refresh Token은 HttpOnly 쿠키로만 전달한다
    → CookieUtils.addRefreshTokenCookie() 사용 여부 확인

□ 비밀번호 / 토큰 값 / 개인정보가 log.info / log.warn 등 로그에 기록되지 않는가?
    → 로그에 token=, password=, email= 형태로 값이 찍히면 ❌

### 2-3. 입력 검증 점검
□ 외부 입력을 받는 @RequestBody 파라미터에 @Valid가 선언되어 있는가?
    → @RequestBody XxxRequest request 에 @Valid 없으면 ⚠️

□ Request DTO record 필드에 Bean Validation 어노테이션이 적용되어 있는가?
    → 필수 입력 필드에 @NotBlank / @NotNull 없으면 ⚠️

---

## STEP 3. 응답 구조 점검

### 3-1. 응답 래퍼 일관성
□ 신규 API의 응답이 ApiResponse<T> 래퍼를 사용하는가?
    → ResponseEntity<ApiResponse<XxxResponse>> 형식 준수
    → ApiResponse.success(data) 사용 확인

□ 성공 응답에 ErrorResponse를 반환하고 있지 않은가?
    → 성공 케이스에 ErrorResponse가 쓰이면 ❌

### 3-2. 예외 처리 일관성
□ 직접 RuntimeException / IllegalArgumentException을 throw하고 있지 않은가?
    → 프로젝트 예외 클래스만 사용한다:
        404 → NotFoundException
        400 → BadRequestException
        401 → UnauthorizedException
        403 → ForbiddenException
        409 → ConflictException
    → 위 클래스 외의 예외를 직접 throw하면 ❌

□ 예외 생성 시 ErrorCode가 올바르게 전달되고 있는가?
    → new NotFoundException(ErrorCode.USER_NOT_FOUND) 형식 확인
    → ErrorCode 없이 문자열만 throw하면 ❌

□ GlobalExceptionHandler에 등록되지 않은 예외 타입을 새로 정의한 경우,
  핸들러에 추가했는가?
    → 신규 예외 클래스 정의 + GlobalExceptionHandler 누락이면 ❌

---

## STEP 4. Contract 갱신 점검

### 4-1. API 스펙 갱신
□ 이번 구현에서 신규 API 또는 기존 API 수정이 있었는가?
    → 있음: 해당 Controller 메서드에 @Operation / @ApiResponses 어노테이션이 작성되었는가?
            (docs/api/swagger.json은 CI 자동 생성 — 직접 편집 금지, ADR 004)
    → 없음: 이 항목 건너뜀 ✅

### 4-2. ErrorCode 계약 갱신
□ 이번 구현에서 신규 ErrorCode가 추가되었는가?
    → 있음: contract/error-contract.yml 갱신 여부 확인
    → ErrorCode.java 주석에 명시된 갱신 의무 준수
    → 없음: 이 항목 건너뜀 ✅

---

## STEP 5. Redis 캐싱 점검

### 배경
@Cacheable + GenericJackson2JsonRedisSerializer 조합은
타입 정보를 JSON에 포함하여 역직렬화 오류를
유발할 수 있다. (ADR-003 참고)
캐싱 구현 완료 후 반드시 아래 항목을 점검한다.

---

### CACHE-1. 직렬화 방식 점검

□ @Cacheable 어노테이션을 사용하고 있는가?
  → 사용 중: 아래 직렬화 설정을 반드시 확인
  → 미사용: CACHE-2로 건너뜀

□ RedisTemplate 직렬화 설정이 아래 중 하나인가?
  ✅ 권장: StringRedisTemplate 수동 캐싱
           (ObjectMapper로 직접 직렬화)
  ✅ 권장: RedisTemplate<String, String>
           (값을 String으로 직렬화)
  ❌ 주의: GenericJackson2JsonRedisSerializer
           → 타입 정보 포함으로 역직렬화 오류 위험
           → 반드시 StringRedisTemplate으로 전환

□ 직렬화 설정이 RedisConfig Bean에 명시되어 있는가?
```java
    // ✅ 권장 설정
    @Bean
    public RedisTemplate<String, String> redisTemplate(
        RedisConnectionFactory factory
    ) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }
```

---

### CACHE-2. 수동 캐싱 구현 점검

StringRedisTemplate 수동 캐싱 사용 시
아래 항목을 점검한다:

□ 캐시 키 형식이 일관성 있게 정의되어 있는가?
```java
    // ✅ 권장 키 형식
    "contents:list:{category}:{page}"
    "content:detail:{id}"
    "user:profile:{userId}"

    // ❌ 비권장
    "list"  // 너무 짧아 충돌 위험
    "1"     // ID만 사용 시 도메인 구분 불가
```

□ 직렬화 / 역직렬화 로직이 대칭적인가?
```java
    // 저장 (직렬화)
    String json = objectMapper.writeValueAsString(data);
    redisTemplate.opsForValue()
        .set(key, json, ttl, TimeUnit.SECONDS);

    // 조회 (역직렬화)
    String json = redisTemplate.opsForValue().get(key);
    if (json == null) return null;
    return objectMapper.readValue(json, TargetClass.class);
```

□ Redis 장애 시 fallback 처리가 있는가?
```java
    // ✅ 권장
    try {
        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null) return deserialize(cached);
    } catch (Exception e) {
        log.warn("Redis 조회 실패, DB 직접 조회: {}", e.getMessage());
    }
    // Redis 실패 시 DB 직접 조회
    return repository.findById(id);
```

---

### CACHE-3. TTL 설정 점검

□ 모든 캐시 항목에 TTL이 명시적으로 설정되어 있는가?
```java
    // ✅ TTL 명시
    redisTemplate.opsForValue()
        .set(key, value, 3600, TimeUnit.SECONDS);

    // ❌ TTL 미설정 (영구 저장 → 메모리 누수)
    redisTemplate.opsForValue().set(key, value);
```

□ TTL이 비즈니스 요구사항과 일치하는가?
```
콘텐츠 목록   : 1시간 (Notion 동기화 주기와 일치)
콘텐츠 상세   : 1시간
사용자 프로필 : 30분
온보딩 결과   : 24시간
```

□ 캐시 TTL 상수가 중앙 관리되고 있는가?
```java
    // ✅ 권장
    public class CacheTtl {
        public static final long CONTENTS = 3600L;
        public static final long USER     = 1800L;
    }
```

---

### CACHE-4. 캐시 무효화 정책 점검

□ 데이터 변경 시 관련 캐시가 무효화되는가?
```
예) Notion 동기화 완료
→ contents:list:* 전체 무효화
→ content:detail:{id} 개별 무효화
```

□ 캐시 무효화 로직이 Service 레이어에서 처리되고 있는가?
```java
    // ✅ 동기화 완료 후 캐시 무효화
    public SyncResult sync() {
        syncPublishedContents();
        Set<String> keys = redisTemplate.keys("contents:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
```

□ 캐시 무효화 범위가 최소한으로 설정되어 있는가?
```
✅ 변경된 항목만 무효화
⚠️ 전체 무효화는 부하 주의
```

---

### CACHE-5. 로컬 / 운영 환경 분리 점검

□ 로컬 개발 환경에서 Redis 연결이 없을 때 오류 없이 동작하는가?
```yaml
    # application-local.yml
    spring:
      cache:
        type: none
```

□ 운영 환경 Redis URL이 환경 변수로 관리되고 있는가?
```bash
    # Railway 환경 변수
    SPRING_REDIS_HOST=${Redis.REDISHOST}
    SPRING_REDIS_PORT=${Redis.REDISPORT}
    SPRING_REDIS_PASSWORD=${Redis.REDISPASSWORD}
```

□ Redis 공개 Proxy URL 확인 방법을 알고 있는가?
```bash
    # Railway CLI로 확인
    railway variables --service Redis
    → REDIS_PUBLIC_URL 값 확인
```

---

### CACHE 점검 완료 출력 형식

아래 형식으로 캐싱 점검 결과를 출력한다:

```
## Redis 캐싱 자가 진단 결과

### CACHE-1. 직렬화 방식
✅/❌ {직렬화 방식 및 설정 확인 결과}

### CACHE-2. 수동 캐싱 구현
✅/❌/⚠️ {캐시 키 형식 / 대칭성 / fallback 확인 결과}

### CACHE-3. TTL 설정
✅/❌ {TTL 명시 및 비즈니스 요구사항 일치 여부}

### CACHE-4. 캐시 무효화
✅/⚠️ {무효화 로직 및 범위 확인 결과}

### CACHE-5. 환경 분리
✅/⚠️ {로컬 / 운영 환경 분리 확인 결과}

→ 캐싱 점검 완료.
```

---

## STEP 6. 결과 출력 및 완료 판단

아래 형식으로 점검 결과를 출력한다:

```
[자가 점검 결과]
STEP 1 레이어 책임 : ✅ 이상 없음
                   / ❌ {위반 항목 설명}
STEP 2 인증 / 보안 : ✅ 이상 없음
                   / ❌ {위반 항목 설명}
                   / ⚠️ {주의 항목 설명}
STEP 3 응답 구조   : ✅ 이상 없음
                   / ❌ {위반 항목 설명}
STEP 4 Contract    : ✅ 이상 없음 (또는 해당 없음)
                   / ❌ @Operation 누락 또는 error-contract.yml 미갱신
STEP 5 Redis 캐싱  : ✅ 이상 없음 (또는 해당 없음)
                   / ❌ {CACHE-1~5 위반 항목 설명}
                   / ⚠️ {주의 항목 설명}
```

### 완료 판단 규칙

**모든 항목 ✅ 또는 ⚠️ 만 존재하는 경우:**
→ "자가 점검 완료. 구현을 완료합니다." 선언
→ CLAUDE.md의 Slack 보고 규칙에 따라 작업 결과를 보고한다

**❌ 항목이 1개 이상인 경우:**
→ "구현 완료"를 선언하지 않는다
→ ❌ 항목을 즉시 수정한다
→ 수정 완료 후 testing.md → self-diagnose.md 순서로 재실행한다
→ 모든 ❌ 해소 후에만 완료를 선언한다

**⚠️ 항목만 존재하는 경우:**
→ 주의 항목 내용과 판단 근거를 사용자에게 출력한다
→ 사용자 확인 없이 완료를 선언해도 되는 경우:
    - Bean Validation 어노테이션 누락이 의도적인 설계인 경우
    - readOnly = true 누락이 쓰기 작업과 함께 묶인 트랜잭션인 경우
→ 판단이 불확실한 경우 사용자에게 확인 후 완료를 선언한다
