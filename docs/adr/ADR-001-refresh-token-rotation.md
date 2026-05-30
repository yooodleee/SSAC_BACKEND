# ADR-001: Refresh Token Rotation 정책 적용

## 맥락 (Context)
초기 인증 구현에서 Access Token만 발급하는 방식을 사용했으나,
Access Token 탈취 시 만료 전까지 무효화가 불가능한 보안 취약점이 존재했다.

토큰 갱신 방식으로 아래 두 가지가 검토됐다:
- Access Token 단독 발급 (만료 시 재로그인)
- HttpOnly Cookie 기반 Refresh Token + Rotation 전략

Refresh Token을 도입할 경우 탈취 감지와 무효화가 가능하므로
장기 세션 보안을 보장할 수 있다.

## 결정 (Decision)
**HttpOnly Cookie + Refresh Token Rotation(RTR) 전략을 채택한다.**

- Refresh Token은 HttpOnly, Secure 쿠키로 전송하여 JS 접근을 차단한다.
- 재발급 시 기존 Refresh Token을 무효화하고 새 토큰을 발급한다 (RTR).
- 토큰 저장소는 `TokenStore` 인터페이스로 추상화하여
  JPA → Redis 전환 시 Bean 교체만으로 동작하도록 설계한다.
- `TokenService`는 `TokenStore`에만 의존하므로 저장소 변경에 영향받지 않는다.

```
TokenService → TokenStore (interface)
                   ↑
            JpaTokenStore (현재)
```

## 결과 (Consequences)
**긍정적 영향:**
- Refresh Token 탈취 감지 가능 (RTR: 재사용 시 전체 세션 무효화)
- HttpOnly 쿠키로 XSS 기반 토큰 탈취 차단
- TokenStore 추상화로 Redis 전환 비용 최소화

**부정적 영향 / 트레이드오프:**
- 모든 재발급 요청에서 DB 쓰기 발생 (JPA 사용 시)
- Redis 전환 전까지는 수평 확장 시 세션 공유 문제 발생 가능

**향후 검토 필요 항목:**
- 트래픽 증가 시 JpaTokenStore → Redis 기반 TokenStore 전환

## 프로토콜 반영 필요 여부
- [x] self-diagnose.md → HttpOnly 쿠키 설정 점검 항목 추가 검토
- [ ] sc-structure-check.md → 해당 없음
- [ ] testing.md → 해당 없음
- [ ] CLAUDE.md → 해당 없음
- [ ] flyway.md → 해당 없음

## 작성일
2026-05-30

## 작성자
에이전트 (소급 작성)
