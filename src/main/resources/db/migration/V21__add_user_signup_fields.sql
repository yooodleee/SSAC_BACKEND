-- V21__add_user_signup_fields.sql
-- 회원 가입 시 수집하는 개인 정보 컬럼 추가 (MySQL 호환 information_schema 패턴)

-- name 컬럼
SELECT COUNT(*) INTO @col_exists
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name   = 'users'
  AND column_name  = 'name';
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE users ADD COLUMN name VARCHAR(20) NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- birth_date 컬럼
SELECT COUNT(*) INTO @col_exists
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name   = 'users'
  AND column_name  = 'birth_date';
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE users ADD COLUMN birth_date DATE NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- phone 컬럼
SELECT COUNT(*) INTO @col_exists
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name   = 'users'
  AND column_name  = 'phone';
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE users ADD COLUMN phone VARCHAR(20) NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- gender 컬럼
SELECT COUNT(*) INTO @col_exists
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name   = 'users'
  AND column_name  = 'gender';
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE users ADD COLUMN gender VARCHAR(10) NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- phone UNIQUE INDEX (1인 1계정 정책)
SELECT COUNT(*) INTO @idx_exists
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name   = 'users'
  AND index_name   = 'idx_users_phone';
SET @sql = IF(@idx_exists = 0,
    'CREATE UNIQUE INDEX idx_users_phone ON users (phone)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
