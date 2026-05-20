-- V24__add_admin_tables.sql
-- 관리자 기능: admin_codes 테이블 생성 + feedbacks.status 컬럼 추가

-- 관리자 사전 발급 코드 테이블
CREATE TABLE IF NOT EXISTS admin_codes (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    code_hash     VARCHAR(255) NOT NULL,
    admin_user_id BIGINT NULL,
    used          BOOLEAN DEFAULT FALSE,
    expires_at    DATETIME NULL,
    created_at    DATETIME NOT NULL DEFAULT NOW()
);

-- users 테이블 role 컬럼 조건부 추가 (이미 존재하면 무시)
SET @dbname = DATABASE();
SET @preparedStatement = (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @dbname
           AND TABLE_NAME   = 'users'
           AND COLUMN_NAME  = 'role') > 0,
        'SELECT 1',
        "ALTER TABLE users ADD COLUMN role VARCHAR(20) DEFAULT 'USER' NOT NULL"
    )
);
PREPARE stmt FROM @preparedStatement;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- feedbacks 테이블 status 컬럼 조건부 추가
SET @preparedStatement = (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @dbname
           AND TABLE_NAME   = 'feedbacks'
           AND COLUMN_NAME  = 'status') > 0,
        'SELECT 1',
        "ALTER TABLE feedbacks ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PENDING'"
    )
);
PREPARE stmt FROM @preparedStatement;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
