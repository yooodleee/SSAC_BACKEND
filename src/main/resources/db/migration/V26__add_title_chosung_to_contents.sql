-- V26__add_title_chosung_to_contents.sql
-- contents 테이블에 title_chosung 컬럼 추가 (초성 검색용)

SELECT COUNT(*) INTO @col_exists
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME   = 'contents'
  AND COLUMN_NAME  = 'title_chosung';

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE contents ADD COLUMN title_chosung VARCHAR(255) NULL',
    'SELECT 1');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
