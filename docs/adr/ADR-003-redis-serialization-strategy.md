# ADR-003: Redis 직렬화 전략 변경

## 맥락 (Context)
콘텐츠 목록 캐싱에 `@Cacheable` + `GenericJackson2JsonRedisSerializer`를
사용하는 방식을 채택했다.

`GenericJackson2JsonRedisSerializer`는 `activateDefaultTyping(NON_FINAL)`
설정으로 비-final 타입에 `["className", {...}]` 형식의 타입 래퍼를 추가한다.

그러나 두 가지 문제가 발생했다:

1. **Java record는 final** — `ContentItemDto` record에 타입 래퍼가 붙지 않아
   `[]`가 Redis에 저장됨. 역직렬화 시 `END_ARRAY` 오류 발생.

2. **`List.toList()` 반환 타입은 `ImmutableCollections.ListN` (final)** —
   마찬가지로 타입 래퍼 없이 `[]` 저장.

결과: `/api/v1/contents` 요청마다 Redis 역직렬화 실패 →
`SERVER-001` 500 오류 → 프론트엔드가 미인증으로 오해 → 로그인 루프 발생.

해결 방안으로 아래 두 가지가 검토됐다:

| 대안 | 장점 | 단점 | 결론 |
|-----|-----|-----|-----|
| `@Cacheable` + `OBJECT_AND_NON_CONCRETE` 타입 | Spring Cache 추상화 유지 | record·불변 컬렉션 타입 래퍼 문제 근본 해결 불가 | 미채택 |
| `StringRedisTemplate` + `TypeReference` 수동 캐싱 | 타입 정보 문제 원천 차단, 직렬화 포맷 완전 제어 | 캐시 코드 직접 작성 필요 | **채택** |

## 결정 (Decision)
**`@Cacheable` 제거, `StringRedisTemplate` + `ObjectMapper.readValue(TypeReference)` 방식으로 전환한다.**

핵심 변경 사항:
- `@EnableCaching`, `CacheManager` Bean 제거 (`RedisConfig`)
- `NotionSyncService`에 `StringRedisTemplate` + static `ObjectMapper` 직접 주입
- 캐시 읽기: `stringRedisTemplate.opsForValue().get(key)` → `OBJECT_MAPPER.readValue(json, new TypeReference<List<ContentItemDto>>() {})`
- 캐시 쓰기: `OBJECT_MAPPER.writeValueAsString(result)` → `stringRedisTemplate.opsForValue().set(key, json, TTL)`
- 캐시 키 prefix: `contents:v3:` → `contents:v4:` (기존 오염 캐시 무효화)

```java
// 캐시 읽기
String cached = stringRedisTemplate.opsForValue().get(cacheKey);
return OBJECT_MAPPER.readValue(cached, new TypeReference<List<ContentItemDto>>() {});

// 캐시 쓰기
String json = OBJECT_MAPPER.writeValueAsString(result);
stringRedisTemplate.opsForValue().set(cacheKey, json, Duration.ofSeconds(3600));
```

## 결과 (Consequences)
**긍정적 영향:**
- Redis 역직렬화 실패로 인한 500 오류 완전 해소
- 직렬화 포맷 완전 제어 (타입 래퍼 없는 순수 JSON)
- 역직렬화 시 명시적 `TypeReference`로 타입 안전성 보장

**부정적 영향 / 트레이드오프:**
- Spring Cache 추상화 미사용 → 캐시 로직 서비스 코드에 직접 노출
- `CacheManager` 기반 테스트 불가 → `StringRedisTemplate` mock으로 전환 필요

**향후 검토 필요 항목:**
- 캐시 레이어가 늘어날 경우 공통 유틸 추출 검토

## 프로토콜 반영 필요 여부
- [x] self-diagnose.md → `StringRedisTemplate + TypeReference` 패턴 직렬화 점검 항목 추가 필요
- [ ] sc-structure-check.md → 해당 없음
- [x] testing.md → `StringRedisTemplate` mock 패턴 가이드 추가 검토
- [ ] CLAUDE.md → 해당 없음
- [ ] flyway.md → 해당 없음

## 작성일
2026-05-30

## 작성자
에이전트 (소급 작성)
