-- contents 테이블에 published_at 컬럼 추가
SET @dbname = DATABASE();
SET @preparedStatement = (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @dbname
       AND TABLE_NAME   = 'contents'
       AND COLUMN_NAME  = 'published_at') > 0,
    'SELECT 1',
    'ALTER TABLE contents ADD COLUMN published_at DATETIME NULL'
  )
);
PREPARE stmt FROM @preparedStatement;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
