-- V1: quiz_attempts 테이블에 비회원(Guest) 지원을 위한 컬럼 및 인덱스 추가
-- 1. user_id 컬럼을 NULL 허용으로 변경 (비회원은 user_id가 없음)
ALTER TABLE quiz_attempts MODIFY user_id BIGINT NULL;

-- 2. guest_id 컬럼 추가 (UUID 저장용, 36자)
SELECT COUNT(*) INTO @col_guest_id FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'quiz_attempts' AND column_name = 'guest_id';
SET @sql_add_guest = IF(COALESCE(@col_guest_id, 0) = 0, 'ALTER TABLE quiz_attempts ADD COLUMN guest_id VARCHAR(36) AFTER user_id', 'SELECT 1');
PREPARE stmt_add_guest FROM @sql_add_guest; EXECUTE stmt_add_guest; DEALLOCATE PREPARE stmt_add_guest;

-- 3. guest_id 기반 조회를 위한 인덱스 추가
SELECT COUNT(*) INTO @idx_guest_id FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'quiz_attempts' AND index_name = 'idx_quiz_attempts_guest_id';
SET @sql_guest_id = IF(COALESCE(@idx_guest_id, 0) = 0, 'CREATE INDEX idx_quiz_attempts_guest_id ON quiz_attempts (guest_id)', 'SELECT 1');
PREPARE stmt_guest_id FROM @sql_guest_id; EXECUTE stmt_guest_id; DEALLOCATE PREPARE stmt_guest_id;
