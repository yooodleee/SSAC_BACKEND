CREATE TABLE IF NOT EXISTS contents (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    title             VARCHAR(200) NOT NULL,
    category          VARCHAR(50),
    difficulty        VARCHAR(20),
    estimated_minutes INT          NOT NULL DEFAULT 0,
    view_count        BIGINT       NOT NULL DEFAULT 0,
    created_at        DATETIME     NOT NULL,
    PRIMARY KEY (id)
);

SELECT COUNT(*) INTO @idx_contents_cat FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'contents' AND index_name = 'idx_contents_category_difficulty';
SET @sql_idx_cat = IF(COALESCE(@idx_contents_cat, 0) = 0, 'CREATE INDEX idx_contents_category_difficulty ON contents (category, difficulty)', 'SELECT 1');
PREPARE stmt_idx_cat FROM @sql_idx_cat; EXECUTE stmt_idx_cat; DEALLOCATE PREPARE stmt_idx_cat;
