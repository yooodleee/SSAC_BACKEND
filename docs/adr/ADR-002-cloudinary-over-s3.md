# ADR-002: S3 대신 Cloudinary 선택

## 맥락 (Context)
Notion 데이터베이스와 콘텐츠를 동기화할 때
콘텐츠 썸네일 이미지를 Notion 페이지의 URL로 저장했다.

Notion 이미지 URL은 AWS S3 presigned URL 기반으로
**약 1시간 후 만료**된다. 동기화 이후 프론트엔드에서
이미지를 렌더링하면 이미 만료된 URL로 인해 이미지가
깨지는 문제가 발생했다.

이를 해결하기 위한 이미지 영구 저장소 방안으로
아래 두 가지가 검토됐다:

| 대안 | 설명 |
|-----|-----|
| AWS S3 직접 업로드 | Notion URL → S3 업로드 후 영구 URL 사용 |
| Cloudinary | Notion URL → Cloudinary 업로드 후 CDN URL 사용 |

## 결정 (Decision)
**Cloudinary를 이미지 저장소로 채택한다.**

채택 이유:
- S3 대비 설정이 간결하다 (IAM 정책, 버킷 정책, CORS 불필요)
- Cloudinary SDK가 URL 직접 업로드를 지원하여 구현이 단순하다
- CDN이 기본 내장되어 별도 CloudFront 설정 불필요
- 무료 플랜으로 현재 트래픽 수준에서 충분히 운영 가능

구현 방식:
- Notion 동기화 시 `NotionImageMigrator.migrateIfNeeded(url)` 호출
- 이미 Cloudinary URL(`res.cloudinary.com` 포함)이면 건너뜀
- 마이그레이션 실패 시 원본 Notion URL 유지 (warn 로그)

```
NotionSyncService.upsertContent()
    → NotionImageMigrator.migrateIfNeeded(rawThumbnail)
        → Cloudinary.uploader().upload(url, folder="content-thumbnails")
        → secure_url 반환
```

## 결과 (Consequences)
**긍정적 영향:**
- Notion 이미지 URL 만료 문제 완전 해결
- CDN 기본 제공으로 이미지 로딩 성능 향상
- 구현 및 운영 복잡도 낮음

**부정적 영향 / 트레이드오프:**
- Cloudinary 외부 서비스 의존 추가
- 무료 플랜 스토리지/전송량 한도 존재 (트래픽 증가 시 유료 전환 필요)
- 마이그레이션 실패 시 원본 URL 유지 → 1시간 후 이미지 깨짐 가능

**향후 검토 필요 항목:**
- 트래픽 급증 시 Cloudinary 플랜 상향 또는 S3 전환 재검토

## 프로토콜 반영 필요 여부
- [ ] self-diagnose.md → 해당 없음
- [ ] sc-structure-check.md → 해당 없음
- [ ] testing.md → 해당 없음
- [ ] CLAUDE.md → 해당 없음
- [ ] flyway.md → 해당 없음

## 작성일
2026-05-30

## 작성자
에이전트 (소급 작성)
