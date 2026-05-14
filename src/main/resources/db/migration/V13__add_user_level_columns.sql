-- IF NOT EXISTS: 컬럼이 이미 존재해도 오류 없이 통과 (멱등성 보장)
-- 배경: ddl-auto=update 긴급 배포 후 동일 컬럼을 Flyway가 재추가 시도하면
--       'Duplicate column name' 오류로 FAILED 상태가 되어 이후 모든 배포가 차단됨
SELECT COUNT(*) INTO @col_lsa FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'level_set_at';
SET @sql_lsa = IF(COALESCE(@col_lsa, 0) = 0, 'ALTER TABLE users ADD COLUMN level_set_at DATETIME NULL', 'SELECT 1');
PREPARE stmt_lsa FROM @sql_lsa; EXECUTE stmt_lsa; DEALLOCATE PREPARE stmt_lsa;

SELECT COUNT(*) INTO @col_onb FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'onboarding_completed';
SET @sql_onb = IF(COALESCE(@col_onb, 0) = 0, 'ALTER TABLE users ADD COLUMN onboarding_completed TINYINT(1) NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE stmt_onb FROM @sql_onb; EXECUTE stmt_onb; DEALLOCATE PREPARE stmt_onb;

-- 기존에 level이 설정된 사용자는 온보딩 완료로 간주
UPDATE users SET onboarding_completed = 1 WHERE level IS NOT NULL AND onboarding_completed = 0;
