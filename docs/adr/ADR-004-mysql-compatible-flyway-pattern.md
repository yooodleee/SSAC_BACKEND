# ADR-004: MySQL 호환 Flyway 조건부 패턴 채택

## 맥락 (Context)
Flyway 마이그레이션 스크립트에서 멱등성(idempotency)을 보장하기 위해
컬럼/인덱스 추가 시 `IF NOT EXISTS` 조건을 사용하려 했다.

그러나 아래 문법은 **MariaDB 전용**으로 MySQL에서는 지원하지 않는다:
- `ALTER TABLE ... ADD COLUMN IF NOT EXISTS`
- `CREATE INDEX IF NOT EXISTS`

Railway 배포 환경은 MySQL을 사용하므로 해당 문법 사용 시
마이그레이션 실행 단계에서 SQL 구문 오류가 발생한다.

해결 방안으로 아래 두 가지가 검토됐다:

| 대안 | 장점 | 단점 | 결론 |
|-----|-----|-----|-----|
| MariaDB 전환 | `IF NOT EXISTS` 그대로 사용 가능 | 운영 DB 변경 비용, 호환성 리스크 | 미채택 |
| `information_schema` 조건부 패턴 | MySQL 완전 호환, 멱등성 보장 | 스크립트 복잡도 증가 | **채택** |

## 결정 (Decision)
**MySQL 호환 `information_schema` 조건부 패턴을 Flyway 컨벤션으로 확정한다.**

### 컬럼 추가 패턴
```sql
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = '{테이블명}'
      AND COLUMN_NAME  = '{컬럼명}'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE {테이블명} ADD COLUMN {컬럼명} {타입}',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
```

### 인덱스 추가 패턴
```sql
SET @idx_exists = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = '{테이블명}'
      AND INDEX_NAME   = '{인덱스명}'
);
SET @sql = IF(@idx_exists = 0,
    'CREATE INDEX {인덱스명} ON {테이블명}({컬럼명})',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
```

### 허용 패턴
- `CREATE TABLE IF NOT EXISTS` — MySQL 지원, 그대로 사용
- `INSERT IGNORE` / `ON DUPLICATE KEY UPDATE` — MySQL 지원, 그대로 사용

### 검증 명령어
```bash
grep -rn "ADD COLUMN IF NOT EXISTS\|CREATE INDEX IF NOT EXISTS" \
  src/main/resources/db/migration/
# → 출력 없음 확인
```

## 결과 (Consequences)
**긍정적 영향:**
- MySQL / MariaDB 모두 호환되는 멱등성 마이그레이션 보장
- 재실행 시 오류 없이 안전하게 통과

**부정적 영향 / 트레이드오프:**
- `information_schema` 패턴이 MariaDB `IF NOT EXISTS`보다 코드량이 많음
- 개발자가 패턴을 모를 경우 MariaDB 전용 문법을 실수로 사용할 위험

**향후 검토 필요 항목:**
- `docs/conventions/flyway.md` 템플릿 현행화 유지

## 프로토콜 반영 필요 여부
- [ ] self-diagnose.md → 해당 없음
- [x] sc-structure-check.md → MySQL 미지원 문법 점검 항목 추가 (완료)
- [ ] testing.md → 해당 없음
- [x] CLAUDE.md → Flyway 마이그레이션 작성 규칙 섹션에 금지 문법 명시 (완료)
- [x] flyway.md → 조건부 패턴 템플릿 현행화 (완료)

## 작성일
2026-05-30

## 작성자
에이전트 (소급 작성)
