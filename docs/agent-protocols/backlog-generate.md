# Backlog SC 생성 하네스 (BE)

## 역할
외부에서 제공된 백로그 SC를 그대로 사용하지 않는다. 프로젝트 구조와 맥락을 먼저 파악한 후
기존 코드와 충돌 없는 SC를 직접 생성하거나 수정한다.

## 트리거 조건 (자동 실행)
- 사용자가 백로그 SC 제공 / "백로그·SC·구현해줘" 언급 시
- `sc-harness.md` 실행 완료 직후

## 실행 순서
STEP 1(프로젝트 구조 파악) → 2(기존 코드 분석) → 3(SC 설계 패턴 검토) → 4(충돌 감지 및 수정) → 5(최종 SC 확정)

---

## STEP 1. 프로젝트 구조 파악

SC 검토 전 아래 파일을 순서대로 읽는다:

| 파일 | 파악 목적 |
|------|---------|
| `CLAUDE.md` | 전역 규칙 / 프로토콜 실행 순서 |
| `docs/conventions/flyway.md` | 마이그레이션 컨벤션 / 최신 버전 번호 |
| `common/exception/ErrorCode.java` | 기존 ErrorCode 전체 목록 / 신규 번호 충돌 방지 |
| `common/response/ApiResponse.java` | 공통 응답 구조 |
| `resources/application-docker.yml` | 환경 변수 / 설정 |
| `docs/api/swagger.json` (없으면 `@RequestMapping` grep) | 기존 API 경로 전체 목록 |

SC 관련 도메인 파일도 함께 읽는다: `Controller` / `Service` / `Repository` / `Entity` / `DTO`

---

## STEP 2. 기존 코드 분석

- [ ] SC에서 언급된 API가 이미 존재하는가? (존재 → 수정 / 미존재 → 신규)
- [ ] 기존 API의 응답 구조가 `ApiResponse<T>` 래퍼를 사용하는가?
- [ ] 기존 API의 인증 방식 확인 (`SecurityConfig` PUBLIC / AUTHENTICATED 설정)
- [ ] SC에서 추가 필요한 필드가 이미 엔티티에 존재하는가?
- [ ] 기존 연관 관계 / 제약 조건(NOT NULL·UNIQUE) 확인
- [ ] SC가 요구하는 ErrorCode가 이미 존재하는가?
- [ ] 인증 관련 SC인 경우: Refresh Token 저장 방식(HttpOnly Cookie / Redis / DB) 확인

---

## STEP 3. SC 설계 패턴 검토

코드베이스 실제 충돌(경로 중복·ErrorCode 중복·Flyway 버전)은 `sc-structure-check.md`에서 수행.  
이 단계에서는 설계 패턴 일치 여부만 검토한다.

**API 설계**
- [ ] 경로 컨벤션 준수: `/api/v1/{도메인}/{리소스}`
- [ ] HTTP 메서드 RESTful 원칙: 조회 GET / 생성 POST / 수정 PATCH / 삭제 DELETE
- [ ] Refresh Token: body 전달 금지 → Cookie 기반(`CookieUtils.addRefreshTokenCookie`)
- [ ] 응답 구조: `ApiResponse<T>` 래퍼 사용 / 민감 정보 body 포함 금지

**ErrorCode 설계**
- [ ] 도메인 분류 적절성: 인증 관련 → `AUTH-XXX` (USER 도메인 오분류 금지)
- [ ] 네이밍 컨벤션: `{도메인}-{순번}` 형식 (예: `AUTH-011`, `AUTH011` 금지)

**SQL / 마이그레이션 설계**
- [ ] MySQL 미지원 문법 금지: `ADD COLUMN IF NOT EXISTS` / `CREATE INDEX IF NOT EXISTS`
  → `information_schema` 조건부 패턴으로 교체 (`docs/conventions/flyway.md` 참고)

---

## STEP 4. 충돌 감지 및 SC 수정

STEP 3에서 발견된 항목을 아래 기준으로 수정한다:

| 충돌 유형 | 수정 방향 | 근거 |
|---------|---------|------|
| Refresh Token body 전달 | `@CookieValue` + `CookieUtils.addRefreshTokenCookie`로 전환 | HttpOnly 쿠키 정책 |
| 응답 body에 민감 정보 포함 | `Set-Cookie` 헤더로 전환 / body에서 제거 | `ApiResponse` 공통 구조 |
| ErrorCode 도메인 오분류 | 올바른 도메인으로 재분류 (예: `USER-XXX` → `AUTH-XXX`) | `ErrorCode.java` 도메인 그룹 |
| MySQL 미지원 SQL 문법 | `information_schema` 조건부 패턴으로 교체 | `docs/conventions/flyway.md` |

---

## STEP 5. 최종 SC 확정 및 출력

```
## 최종 Success Criteria (BE 구현 기준)

### 검토 요약
- 원본 SC 항목 수: N개 / 충돌 없음: N개 / 수정 완료: N개 / 제외(FE 관심사): N개

### 충돌 수정 내역
[수정 N] {수정 항목}
❌ 수정 전: {원본}
✅ 수정 후: {수정본}
근거: {기존 코드 / 파일명}

### 최종 SC 목록
1. {항목 1}
2. {항목 2}
...

→ 최종 SC 확정 완료. sc-structure-check.md를 실행합니다.
```
