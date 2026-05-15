-- content_id: contents 테이블 참조 (nullable, 레거시 진행 기록과의 호환성 유지)
SELECT COUNT(*) INTO @col_cid FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'content_progress' AND column_name = 'content_id';
SET @sql_cid = IF(COALESCE(@col_cid, 0) = 0, 'ALTER TABLE content_progress ADD COLUMN content_id BIGINT NULL', 'SELECT 1');
PREPARE stmt_cid FROM @sql_cid; EXECUTE stmt_cid; DEALLOCATE PREPARE stmt_cid;

-- category: 홈 화면 이어보기 조회 최적화용 (비정규화)
SELECT COUNT(*) INTO @col_cat FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'content_progress' AND column_name = 'category';
SET @sql_cat = IF(COALESCE(@col_cat, 0) = 0, 'ALTER TABLE content_progress ADD COLUMN category VARCHAR(50) NULL', 'SELECT 1');
PREPARE stmt_cat FROM @sql_cat; EXECUTE stmt_cat; DEALLOCATE PREPARE stmt_cat;
