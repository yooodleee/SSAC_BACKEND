-- V25__add_search_keyword_table.sql
-- 검색 키워드 집계 테이블 생성

CREATE TABLE IF NOT EXISTS search_keywords (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    keyword          VARCHAR(100) NOT NULL,
    search_count     BIGINT       NOT NULL DEFAULT 1,
    last_searched_at DATETIME     NOT NULL DEFAULT NOW(),
    created_at       DATETIME     NOT NULL DEFAULT NOW()
);

-- 유니크 인덱스 (information_schema 조건부 패턴)
SELECT COUNT(*) INTO @idx_exists
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME   = 'search_keywords'
  AND INDEX_NAME   = 'uq_search_keyword';

SET @sql = IF(@idx_exists = 0,
    'CREATE UNIQUE INDEX uq_search_keyword ON search_keywords(keyword)',
    'SELECT 1');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
