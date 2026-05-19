-- V22__add_content_view_history_and_user_fields.sql
-- 콘텐츠 조회 이력 테이블 추가 및 users 테이블 컬럼 추가

-- content_view_histories 테이블 생성
CREATE TABLE IF NOT EXISTS content_view_histories (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT       NOT NULL,
    content_id   BIGINT       NOT NULL,
    is_completed BOOLEAN      NOT NULL DEFAULT FALSE,
    viewed_at    DATETIME     NOT NULL DEFAULT NOW(),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- content_view_histories.user_id 인덱스
SELECT COUNT(*) INTO @idx_exists
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name   = 'content_view_histories'
  AND index_name   = 'idx_content_view_user_id';
SET @sql = IF(@idx_exists = 0,
    'CREATE INDEX idx_content_view_user_id ON content_view_histories(user_id)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- nickname_explicitly_set 컬럼 추가
SELECT COUNT(*) INTO @col_exists
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name   = 'users'
  AND column_name  = 'nickname_explicitly_set';
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE users ADD COLUMN nickname_explicitly_set BOOLEAN NOT NULL DEFAULT FALSE',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- deleted_at 컬럼 추가
SELECT COUNT(*) INTO @col_exists
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name   = 'users'
  AND column_name  = 'deleted_at';
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE users ADD COLUMN deleted_at DATETIME NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
