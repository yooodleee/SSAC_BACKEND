-- onboarding_score: 온보딩 테스트 총점 (건너뛰기 시 0)
SELECT COUNT(*) INTO @col_score FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'onboarding_score';
SET @sql_score = IF(COALESCE(@col_score, 0) = 0, 'ALTER TABLE users ADD COLUMN onboarding_score INT NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE stmt_score FROM @sql_score; EXECUTE stmt_score; DEALLOCATE PREPARE stmt_score;

-- onboarding_skipped: 온보딩 건너뛰기 여부
SELECT COUNT(*) INTO @col_skip FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'onboarding_skipped';
SET @sql_skip = IF(COALESCE(@col_skip, 0) = 0, 'ALTER TABLE users ADD COLUMN onboarding_skipped TINYINT(1) NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE stmt_skip FROM @sql_skip; EXECUTE stmt_skip; DEALLOCATE PREPARE stmt_skip;
