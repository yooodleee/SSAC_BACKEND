-- category: 퀴즈 카테고리 (nullable)
SELECT COUNT(*) INTO @col_qcat FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'quizzes' AND column_name = 'category';
SET @sql_qcat = IF(COALESCE(@col_qcat, 0) = 0, 'ALTER TABLE quizzes ADD COLUMN category VARCHAR(50) NULL', 'SELECT 1');
PREPARE stmt_qcat FROM @sql_qcat; EXECUTE stmt_qcat; DEALLOCATE PREPARE stmt_qcat;

-- difficulty: 퀴즈 난이도 (SEED/SPROUT/TREE, nullable)
SELECT COUNT(*) INTO @col_qdiff FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'quizzes' AND column_name = 'difficulty';
SET @sql_qdiff = IF(COALESCE(@col_qdiff, 0) = 0, 'ALTER TABLE quizzes ADD COLUMN difficulty VARCHAR(20) NULL', 'SELECT 1');
PREPARE stmt_qdiff FROM @sql_qdiff; EXECUTE stmt_qdiff; DEALLOCATE PREPARE stmt_qdiff;
