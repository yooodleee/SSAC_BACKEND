-- 약관 동의 일시 컬럼 추가
-- serviceTerm, privacyTerm, ageVerification: 필수 동의 항목 (NOT NULL이 아님 - 기존 레코드 고려)
-- marketingTerm: 선택 동의 항목 (NULL 허용)
SELECT COUNT(*) INTO @col_svc FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'service_term_agreed_at';
SET @sql_svc = IF(COALESCE(@col_svc, 0) = 0, 'ALTER TABLE users ADD COLUMN service_term_agreed_at DATETIME(6) NULL AFTER invalidated_before', 'SELECT 1');
PREPARE stmt_svc FROM @sql_svc; EXECUTE stmt_svc; DEALLOCATE PREPARE stmt_svc;

SELECT COUNT(*) INTO @col_prv FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'privacy_term_agreed_at';
SET @sql_prv = IF(COALESCE(@col_prv, 0) = 0, 'ALTER TABLE users ADD COLUMN privacy_term_agreed_at DATETIME(6) NULL AFTER service_term_agreed_at', 'SELECT 1');
PREPARE stmt_prv FROM @sql_prv; EXECUTE stmt_prv; DEALLOCATE PREPARE stmt_prv;

SELECT COUNT(*) INTO @col_age FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'age_verification_agreed_at';
SET @sql_age = IF(COALESCE(@col_age, 0) = 0, 'ALTER TABLE users ADD COLUMN age_verification_agreed_at DATETIME(6) NULL AFTER privacy_term_agreed_at', 'SELECT 1');
PREPARE stmt_age FROM @sql_age; EXECUTE stmt_age; DEALLOCATE PREPARE stmt_age;

SELECT COUNT(*) INTO @col_mkt FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'marketing_term_agreed_at';
SET @sql_mkt = IF(COALESCE(@col_mkt, 0) = 0, 'ALTER TABLE users ADD COLUMN marketing_term_agreed_at DATETIME(6) NULL AFTER age_verification_agreed_at', 'SELECT 1');
PREPARE stmt_mkt FROM @sql_mkt; EXECUTE stmt_mkt; DEALLOCATE PREPARE stmt_mkt;
