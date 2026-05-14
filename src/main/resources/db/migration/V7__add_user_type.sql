-- 사용자 유형(userType), 유형 설정 일시(userTypeSetAt), 레벨(level) 컬럼 추가
SELECT COUNT(*) INTO @col_utype FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'user_type';
SET @sql_utype = IF(COALESCE(@col_utype, 0) = 0, 'ALTER TABLE users ADD COLUMN user_type VARCHAR(20) NULL', 'SELECT 1');
PREPARE stmt_utype FROM @sql_utype; EXECUTE stmt_utype; DEALLOCATE PREPARE stmt_utype;

SELECT COUNT(*) INTO @col_uts FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'user_type_set_at';
SET @sql_uts = IF(COALESCE(@col_uts, 0) = 0, 'ALTER TABLE users ADD COLUMN user_type_set_at DATETIME NULL', 'SELECT 1');
PREPARE stmt_uts FROM @sql_uts; EXECUTE stmt_uts; DEALLOCATE PREPARE stmt_uts;

SELECT COUNT(*) INTO @col_lvl FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'level';
SET @sql_lvl = IF(COALESCE(@col_lvl, 0) = 0, 'ALTER TABLE users ADD COLUMN level VARCHAR(20) NULL', 'SELECT 1');
PREPARE stmt_lvl FROM @sql_lvl; EXECUTE stmt_lvl; DEALLOCATE PREPARE stmt_lvl;
