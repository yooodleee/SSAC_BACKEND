-- V8(SET+PREPARE 방식) 이 level 컬럼을 추가하지 못한 경우를 보정한다.
--
-- 현재 production DB 상태: Hibernate 검증 결과 level 컬럼 부재 확인.
-- 단순 ALTER TABLE을 사용한다.
--   - level 컬럼이 없는 경우(정상): 컬럼 추가 성공 → Hibernate 검증 통과
--   - level 컬럼이 있는 경우(V7 또는 V8이 이미 추가): "Duplicate column name" 오류 발생
--     → ignore-migration-patterns: "*:failed" 로 건너뜀 → Hibernate 검증은 정상 통과
SELECT COUNT(*) INTO @col_lvl9 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'level';
SET @sql_lvl9 = IF(COALESCE(@col_lvl9, 0) = 0, 'ALTER TABLE users ADD COLUMN level VARCHAR(20) NULL', 'SELECT 1');
PREPARE stmt_lvl9 FROM @sql_lvl9; EXECUTE stmt_lvl9; DEALLOCATE PREPARE stmt_lvl9;
