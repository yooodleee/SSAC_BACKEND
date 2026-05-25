-- V28__notion_content_migration.sql
-- contents 테이블을 Notion 연동 구조로 전환한다.
-- 카테고리·도메인은 ElementCollection 테이블(content_categories, content_domains)로 분리한다.

-- 1. title: NOT NULL 제거 및 길이 확장
ALTER TABLE contents MODIFY COLUMN title VARCHAR(500) NULL;

-- 2. 기존 인덱스 제거
SELECT COUNT(*) INTO @idx_exists
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name   = 'contents'
  AND index_name   = 'idx_contents_category_difficulty';
SET @sql = IF(@idx_exists > 0,
    'DROP INDEX idx_contents_category_difficulty ON contents',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3. 불필요 컬럼 제거
SELECT COUNT(*) INTO @col_exists
FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'contents' AND column_name = 'category';
SET @sql = IF(@col_exists > 0, 'ALTER TABLE contents DROP COLUMN category', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @col_exists
FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'contents' AND column_name = 'estimated_minutes';
SET @sql = IF(@col_exists > 0, 'ALTER TABLE contents DROP COLUMN estimated_minutes', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @col_exists
FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'contents' AND column_name = 'view_count';
SET @sql = IF(@col_exists > 0, 'ALTER TABLE contents DROP COLUMN view_count', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @col_exists
FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'contents' AND column_name = 'title_chosung';
SET @sql = IF(@col_exists > 0, 'ALTER TABLE contents DROP COLUMN title_chosung', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4. Notion 연동 컬럼 추가
SELECT COUNT(*) INTO @col_exists
FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'contents' AND column_name = 'notion_page_id';
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE contents ADD COLUMN notion_page_id VARCHAR(100) NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @col_exists
FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'contents' AND column_name = 'notion_database_id';
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE contents ADD COLUMN notion_database_id VARCHAR(100) NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @col_exists
FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'contents' AND column_name = 'thumbnail_url';
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE contents ADD COLUMN thumbnail_url VARCHAR(1000) NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @col_exists
FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'contents' AND column_name = 'is_published';
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE contents ADD COLUMN is_published BOOLEAN NOT NULL DEFAULT FALSE',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @col_exists
FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'contents' AND column_name = 'notion_created_at';
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE contents ADD COLUMN notion_created_at DATETIME NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @col_exists
FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'contents' AND column_name = 'notion_last_edited_at';
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE contents ADD COLUMN notion_last_edited_at DATETIME NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @col_exists
FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'contents' AND column_name = 'synced_at';
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE contents ADD COLUMN synced_at DATETIME NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @col_exists
FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'contents' AND column_name = 'updated_at';
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE contents ADD COLUMN updated_at DATETIME NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 5. notion_page_id 유니크 인덱스
SELECT COUNT(*) INTO @idx_exists
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name   = 'contents'
  AND index_name   = 'uk_contents_notion_page_id';
SET @sql = IF(@idx_exists = 0,
    'CREATE UNIQUE INDEX uk_contents_notion_page_id ON contents (notion_page_id)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 6. is_published 인덱스
SELECT COUNT(*) INTO @idx_exists
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name   = 'contents'
  AND index_name   = 'idx_contents_is_published';
SET @sql = IF(@idx_exists = 0,
    'CREATE INDEX idx_contents_is_published ON contents (is_published)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 7. content_categories (카테고리 ElementCollection)
CREATE TABLE IF NOT EXISTS content_categories (
    content_id BIGINT      NOT NULL,
    category   VARCHAR(50) NOT NULL,
    FOREIGN KEY (content_id) REFERENCES contents (id) ON DELETE CASCADE
);

-- 8. content_domains (도메인 ElementCollection)
CREATE TABLE IF NOT EXISTS content_domains (
    content_id BIGINT      NOT NULL,
    domain     VARCHAR(50) NOT NULL,
    FOREIGN KEY (content_id) REFERENCES contents (id) ON DELETE CASCADE
);
