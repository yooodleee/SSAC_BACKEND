# ADR 004: Swagger(OpenAPI) 계약 문서 자동화

**상태**: 채택됨  
**날짜**: 2026-04-19  
**담당자**: 팀 전체

---

## 컨텍스트

프론트엔드 에이전트가 백엔드 API를 신뢰할 수 있는 단일 원본으로 소비해야 한다.
수작업으로 관리하는 API 명세는 코드와 어긋나는 순간 에이전트 오작동의 원인이 된다.

---

## 결정 사항

### 1. springdoc-openapi 도입

`springdoc-openapi-starter-webmvc-ui:2.8.x`를 사용해 코드에서 OpenAPI 3.0 스펙을 자동 생성한다.

**채택 이유**: 코드가 정의(단일 원천)이고 문서는 파생물이어야 한다.
수작업 문서는 드리프트(code-doc 불일치)를 피할 수 없다.

**거절된 대안**: 수동 YAML 관리
- 코드와 문서가 분리되어 에이전트 신뢰도 하락

---

### 2. 계약 문서 제공 URL

| 용도 | URL |
|------|-----|
| 에이전트/클라이언트 소비 (JSON) | `GET /api-docs/swagger.json` |
| 개발자 UI | `GET /swagger-ui.html` |
| 형상 관리 (CI 갱신) | `docs/api/swagger.json` |

---

### 3. 에이전트 친화적 문서 작성 기준

모든 컨트롤러 메서드에 아래 어노테이션을 필수로 작성한다.

```java
@Operation(
    summary = "특정 사용자 정보 조회",
    description = """
        [호출 화면] 프로필 페이지, 설정 페이지 진입 시 호출.
        [권한 조건] 로그인한 사용자 본인 또는 ADMIN 역할만 접근 가능.
        [특이 동작] 비활성 계정은 404가 아닌 403 반환 — 존재 여부 노출 방지 목적.
                    탈퇴한 계정은 410 반환.
        """
)
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "조회 성공"),
    @ApiResponse(responseCode = "403", description = "권한 없음 또는 비활성 계정"),
    @ApiResponse(responseCode = "410", description = "탈퇴한 계정")
})
```

#### 나쁜 예 vs. 좋은 예

```
❌ 나쁜 예 (에이전트가 오해하는 문서)
  summary: Get user
  description: 유저를 가져옵니다.
  → 에이전트가 어느 화면에서 호출하는지, 403이 왜 나는지 알 수 없다.

✅ 좋은 예 (에이전트가 신뢰하는 문서)
  summary: 특정 사용자 정보 조회
  description: |
    [호출 화면] 프로필 페이지, 설정 페이지 진입 시 호출.
    [권한 조건] 로그인한 사용자 본인 또는 ADMIN 역할만 접근 가능.
    [특이 동작] 비활성 계정은 404 대신 403 반환 (의도된 설계).
                탈퇴한 계정은 410 반환.
  responses: 200, 401, 403, 410, 500 모두 명시
```

---

### 4. CI/CD 자동 배포 파이프라인

`master` 브랜치 머지 시 `.github/workflows/swagger-contract.yml`이 실행되어:

1. H2 인메모리 DB로 서버 기동 (`--spring.profiles.active=test`)
2. `GET /api-docs/swagger.json` 추출
3. `docs/api/swagger.json` 과 diff 비교 (paths + components 키 기준)
4. 변경 시: `docs/api/swagger.json` 커밋 갱신 + S3 배포
5. 변경 시: Slack 알림 전송

**필요한 GitHub Secrets**:

| Secret 키 | 설명 |
|-----------|------|
| `SLACK_WEBHOOK_URL` | Slack Incoming Webhook URL |
| `AWS_ACCESS_KEY_ID` | S3 배포용 (미설정 시 S3 배포 건너뜀) |
| `AWS_SECRET_ACCESS_KEY` | S3 배포용 |
| `AWS_REGION` | S3 리전 (예: `ap-northeast-2`) |
| `S3_SWAGGER_BUCKET` | swagger.json을 올릴 S3 버킷명 |

---

## 재검토 조건

- springdoc이 Spring Boot 4.x와 호환성 문제가 생길 경우 버전 업그레이드
- 도메인이 5개 이상으로 늘어나면 태그(tag) 기반 문서 분리 검토
- 운영 환경에서 swagger-ui 노출 여부 결정 시 `springdoc.swagger-ui.enabled` 프로파일별 관리
