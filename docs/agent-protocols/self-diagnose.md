# Self-Diagnose Protocol (BE)

## 역할
"구현 완료" 선언 전 코드 레벨 자가 점검. testing.md 통과 후에도 반드시 실행한다.  
트리거: `testing.md` 실행 성공 완료 직후 자동 실행

## 실행 순서
STEP 1(레이어 책임) → 2(인증/보안) → 3(응답 구조) → 4(Contract 갱신) → 5(Redis 캐싱) → 6(결과 출력)

---

## STEP 1. 레이어 책임 점검

**Controller 점검**
- [ ] 비즈니스 로직(조건 분기·계산·DB 조회)이 Controller 내부에 없는가
- [ ] Controller가 Repository를 직접 주입받지 않는가
- [ ] `@Transactional`이 Controller에 선언되어 있지 않은가 (Service 레이어에만 허용)

**Service 점검**
- [ ] 조회(읽기 전용) 메서드에 `@Transactional(readOnly = true)` 선언 여부
- [ ] 데이터 변경 메서드에 `@Transactional` 선언 여부
- [ ] 외부 API 호출(카카오·네이버)이 Service에서 이루어지는가 (Controller 직접 호출 금지)

---

## STEP 2. 인증 / 보안 점검

**SecurityConfig 반영**
- [ ] 인증 필요 신규 API가 `SecurityConfig.PUBLIC_PATHS`에 잘못 등록되어 있지 않은가
- [ ] 공개 접근 허용 신규 API가 `PUBLIC_PATHS`에 누락되어 있지 않은가

**민감 정보 노출**
- [ ] refreshToken이 response body에 반환되지 않는가 (HttpOnly 쿠키 전용 — `CookieUtils.addRefreshTokenCookie()`)
- [ ] 비밀번호·토큰·개인정보가 로그(`log.info` 등)에 기록되지 않는가

**입력 검증**
- [ ] `@RequestBody` 파라미터에 `@Valid` 선언 여부
- [ ] Request DTO 필수 필드에 `@NotBlank` / `@NotNull` 등 Bean Validation 적용 여부

---

## STEP 3. 응답 구조 점검

**응답 래퍼 일관성**
- [ ] 신규 API 응답이 `ResponseEntity<ApiResponse<T>>` 형식을 사용하는가
- [ ] `ApiResponse.success(data)` 사용 확인 (성공 케이스에 `ErrorResponse` 사용 금지)

**예외 처리 일관성**
- [ ] `RuntimeException` / `IllegalArgumentException` 직접 throw 금지 — 프로젝트 예외 클래스만 사용
  - 404 → `NotFoundException` / 400 → `BadRequestException` / 401 → `UnauthorizedException`
  - 403 → `ForbiddenException` / 409 → `ConflictException`
- [ ] 예외 생성 시 `ErrorCode` 전달 여부 (`new NotFoundException(ErrorCode.XXX)` 형식)
- [ ] 신규 예외 클래스 정의 시 `GlobalExceptionHandler`에 핸들러 추가 여부

---

## STEP 4. Contract 갱신 점검

- [ ] 신규/변경 API 있음 → Controller에 `@Operation` / `@ApiResponses` 작성 여부  
  (`docs/api/swagger.json`은 CI 자동 생성 — 직접 편집 금지, ADR-004)
- [ ] 신규 `ErrorCode` 있음 → `contract/error-contract.yml` 갱신 여부  
  변경 없음 → 건너뜀 ✅

---

## STEP 5. Redis 캐싱 점검
캐싱 구현이 없으면 건너뜀 ✅

**CACHE-1. 직렬화 방식**
- [ ] `@Cacheable` + `GenericJackson2JsonRedisSerializer` 조합 사용 금지 (ADR-003)
- [ ] `StringRedisTemplate` 수동 캐싱 사용 여부 / `RedisConfig` Bean에 `StringRedisSerializer` 설정 확인

**CACHE-2. 수동 캐싱 구현**
- [ ] 캐시 키 형식: `domain:type:{id}` 패턴 준수 (예: `contents:list:{category}`, `content:detail:{id}`)
- [ ] 직렬화(`ObjectMapper.writeValueAsString`) ↔ 역직렬화(`readValue`) 대칭 확인
- [ ] Redis 장애 시 fallback 처리 여부 — 아래 패턴 미적용 시 ❌

```java
try {
    String cached = redisTemplate.opsForValue().get(key);
    if (cached != null) return deserialize(cached);
} catch (Exception e) {
    log.warn("Redis 조회 실패, DB 직접 조회: {}", e.getMessage());
}
return repository.findById(id); // fallback
```

**CACHE-3. TTL 설정**
- [ ] 모든 캐시에 TTL 명시 (`set(key, value)` 단독 사용 금지 — 메모리 누수)
- [ ] TTL 기준: 콘텐츠 목록/상세 1시간, 사용자 프로필 30분, 온보딩 24시간
- [ ] TTL 상수 `CacheTtl` 클래스에서 중앙 관리 여부

**CACHE-4. 캐시 무효화**
- [ ] 데이터 변경 시 관련 캐시 무효화 확인 (Notion 동기화 → `contents:*` 전체 삭제)
- [ ] 무효화 로직이 Service 레이어에서 처리되는가

**CACHE-5. 환경 분리**
- [ ] 로컬: `application-local.yml` → `spring.cache.type: none`
- [ ] 운영: `SPRING_REDIS_HOST/PORT/PASSWORD` 환경 변수로 관리 (Railway Variables)

---

## STEP 6. 결과 출력 및 완료 판단

점검 결과를 아래 형식으로 출력한다:
```
[자가 점검 결과]
STEP 1 레이어 책임 : ✅/❌ {설명}
STEP 2 인증/보안   : ✅/❌/⚠️ {설명}
STEP 3 응답 구조   : ✅/❌ {설명}
STEP 4 Contract    : ✅/❌/해당없음
STEP 5 Redis 캐싱  : ✅/❌/⚠️/해당없음
```

**완료 판단 규칙**
- **❌ 존재**: 완료 선언 금지 → 즉시 수정 → `testing.md` → `self-diagnose.md` 재실행 → 모든 ❌ 해소 후 완료 선언
- **✅/⚠️ 만 존재**: "자가 점검 완료. 구현을 완료합니다." 선언 → Slack 보고
- **⚠️ 단독**: 의도적 설계인 경우 근거를 출력한 후 완료 선언 가능 / 판단 불확실 시 사용자 확인 후 선언

---

## STEP 7. Sentry 연동 점검
Sentry 관련 코드 변경이 없으면 건너뜀 ✅

**SENTRY-1. 환경 설정**
- [ ] `application.properties`: `sentry.enabled=false` 존재 여부 (로컬 비활성화)
- [ ] `application-prod.yml`: `sentry.enabled=true` / `dsn: ${SENTRY_DSN}` 설정 여부
- [ ] `send-default-pii: false` 설정 여부 (PII 자동 수집 차단)

**SENTRY-2. DSN 보안**
- [ ] `grep -r "ingest.sentry.io" src/` → 결과 없음 확인 (하드코딩 금지)
- [ ] SENTRY_DSN이 코드 / 로그 / 응답 Body에 노출되지 않는가

**SENTRY-3. 예외 필터링**
- [ ] 4xx 예외 (`BusinessException` 하위)가 `SentryConfig.beforeSendCallback()`에서 필터링되는가
- [ ] 5xx 및 예상치 못한 런타임 예외만 Sentry에 전송되는가

**SENTRY-4. MDC 태그**
- [ ] `SentryConfig.mdcEventProcessor()`가 `trace_id` / `user_id` / `http_method` / `request_path` 태그를 추가하는가
- [ ] 비로그인 요청 시 `user_id` 태그 누락 허용 / 나머지 태그 정상 포함 여부
