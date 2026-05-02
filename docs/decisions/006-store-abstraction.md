# ADR 006 — 저장소 추상화(Store Interface) 원칙

> 작성일: 2026-05-02  
> 상태: 채택(Accepted)

---

## 맥락

- Redis 도입 전 MySQL JPA 기반 구현이 Service에 직접 결합되어 있었다.
- 저장 기술을 교체할 때 Service 코드까지 수정해야 하는 구조적 문제가 있었다.
- Actuator + Redis 의존성이 추가된 시점에 전환 준비를 위한 추상화가 필요했다.

---

## 결정

신규 서비스 클래스 추가 시 JPA Repository를 `@Service` 클래스에서 **직접 의존하지 않는다**.  
저장소 인터페이스(Store Interface)를 통해서만 접근하며, 구현체는 Bean으로 교체한다.

```
// 금지 패턴
@Service
public class XxxService {
    private final XxxRepository repository;  // JPA 직접 의존 금지
}

// 권장 패턴
@Service
public class XxxService {
    private final XxxStore store;  // 인터페이스 의존
}
```

---

## 적용 대상

| Store 인터페이스 | 현재 구현체 | 전환 예정 구현체 | 관련 서비스 |
|---|---|---|---|
| `TokenStore` | `JpaTokenStore` | `RedisTokenStore` | `TokenService` |
| `ViewCountStore` | `MysqlViewCountStore` | `RedisViewCountStore` | `NewsService` |
| `RateLimitStore` | `InMemoryRateLimitStore` | `RedisRateLimitStore` | `RateLimitingFilter` |

---

## 알려진 문제 — KNOWN-LIMIT-001: InMemoryRateLimitStore 다중 인스턴스 한계

### 문제

`InMemoryRateLimitStore`는 JVM 프로세스 내 ConcurrentHashMap으로 카운터를 관리한다.  
로드밸런서 환경에서 2개 이상의 인스턴스가 운영될 경우 각 인스턴스가 독립 카운터를 유지하므로  
실제 허용 요청 수가 `MAX_REQUESTS × 인스턴스 수`가 된다.

### 영향 범위

- 단일 인스턴스(현재 Docker Compose 환경): **정상 동작**
- 다중 인스턴스(수평 확장 시): Rate Limiting 정확도 저하

### 해결 방향 (전환 시 구현)

```java
// RedisRateLimitStore 예시
@Component
public class RedisRateLimitStore implements RateLimitStore {
    private final StringRedisTemplate redisTemplate;

    @Override
    public long increment(String key, long windowMs) {
        String redisKey = "rate_limit:" + key;
        Long count = redisTemplate.opsForValue().increment(redisKey);
        if (count != null && count == 1L) {
            redisTemplate.expire(redisKey, Duration.ofMillis(windowMs));
        }
        return count != null ? count : 1L;
    }
}
```

전환 절차: `InMemoryRateLimitStore` Bean에 `@ConditionalOnProperty` 또는 `@Primary` 조건 추가 후  
`RedisRateLimitStore`를 활성화한다. `RateLimitingFilter` 수정 불필요.

---

## RedisViewCountStore 전환 전략 (예정)

### 동작 패턴

```
1. 조회 발생 → Redis INCR viewcount:{newsId}
2. 스케줄러 (5분 간격) → Redis 카운터를 DB에 일괄 flush
3. flush 완료 후 Redis 카운터 초기화
```

### flush 장애 대응

```
flush 전:
  1. 카운터 값 읽기: GETSET backup:viewcount:{newsId} 0
  2. 백업 키 저장 확인

DB 반영:
  UPDATE news SET view_count = view_count + {backup_value} WHERE id = {newsId}

장애 복구 시:
  - 애플리케이션 시작 시 backup:viewcount:* 키 존재 여부 확인
  - 존재하면 DB 재반영 후 백업 키 삭제
```

> 데이터 유실 위험: flush 시작 후 DB 반영 완료 전 서버 장애 발생 시  
> 백업 키가 남아 있으므로 재시작 후 자동 복구된다.

---

## 추상화 원칙 — Repository 직접 의존 허용 예외

Store 구현체 클래스(`JpaTokenStore`, `MysqlViewCountStore` 등)는  
`service` 패키지 내에 위치하며 JPA Repository에 직접 의존할 수 있다.  
이 클래스들은 `@Service`가 아닌 `@Component`로 표시하여 명확히 구분한다.

| 클래스 유형 | Repository 직접 의존 | 설명 |
|---|---|---|
| `@Service` | **금지** | Store 인터페이스만 사용 |
| `@Component` (Store 구현체) | **허용** | JPA Repository 위임 역할 |

---

## 결과

- `TokenService`, `NewsService`에서 JPA Repository 직접 의존 제거 완료
- Redis 전환 시 Bean 교체만으로 Service 무수정 전환 가능
- `RateLimitingFilter`의 다중 인스턴스 문제가 문서화되어 추적 가능
