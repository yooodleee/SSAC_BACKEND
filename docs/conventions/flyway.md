# Flyway 마이그레이션 컨벤션

## ⚠️ MySQL / MariaDB 문법 호환성 주의

아래 문법은 MariaDB 전용이며 MySQL에서 미지원됩니다:
❌ ALTER TABLE ... ADD COLUMN IF NOT EXISTS
❌ CREATE INDEX IF NOT EXISTS

MySQL에서는 반드시 아래 조건부 패턴을 사용합니다.

## 파일 네이밍 규칙
V{순번}__{설명}.sql
→ 순번은 반드시 현재 최신 버전 + 1
→ 순번 건너뛰기 금지

## 멱등성 보장 패턴 (MySQL 호환)

### 테이블 생성 (IF NOT EXISTS 지원)
```sql
CREATE TABLE IF NOT EXISTS {table_name} (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  created_at DATETIME NOT NULL DEFAULT NOW(),
  updated_at DATETIME NOT NULL DEFAULT NOW()
);
```

### 컬럼 추가 (조건부 패턴 필수)
```sql
SET @dbname = DATABASE();
SET @preparedStatement = (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @dbname
       AND TABLE_NAME   = '{table_name}'
       AND COLUMN_NAME  = '{column_name}') > 0,
    'SELECT 1',
    'ALTER TABLE {table_name} ADD COLUMN {column_name} {type}'
  )
);
PREPARE stmt FROM @preparedStatement;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
```

### 인덱스 생성 (조건부 패턴 필수)
```sql
SET @dbname = DATABASE();
SET @preparedStatement = (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = @dbname
       AND TABLE_NAME   = '{table_name}'
       AND INDEX_NAME   = '{index_name}') > 0,
    'SELECT 1',
    'CREATE INDEX {index_name} ON {table_name}({column_name})'
  )
);
PREPARE stmt FROM @preparedStatement;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
```

### 유니크 제약 추가 (조건부 패턴 필수)
```sql
SET @dbname = DATABASE();
SET @constraintname = 'uq_{table_name}_{column_name}';
SET @tablename = '{table_name}';
SET @preparedStatement = (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
     WHERE TABLE_SCHEMA = @dbname
       AND TABLE_NAME   = @tablename
       AND CONSTRAINT_NAME = @constraintname) > 0,
    'SELECT 1',
    'ALTER TABLE {table_name} ADD CONSTRAINT uq_{table_name}_{column_name} UNIQUE ({column_name})'
  )
);
PREPARE stmt FROM @preparedStatement;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
```

## DB 예약어 컬럼명 금지
❌ type / order / group → ✅ user_type / question_order / user_group

## 데이터 삽입 멱등성
```sql
INSERT IGNORE INTO {table_name} VALUES (...);
-- 또는
INSERT INTO {table_name} (...) VALUES (...)
  ON DUPLICATE KEY UPDATE updated_at = NOW();
```
