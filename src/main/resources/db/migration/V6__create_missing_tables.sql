-- V6: 초기 스키마 생성 이후 추가된 엔티티 테이블 일괄 생성
-- 이미 존재하는 테이블은 IF NOT EXISTS로 무시한다.

-- ── content_progress ─────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS content_progress (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    user_id       BIGINT       NOT NULL,
    title         VARCHAR(200) NOT NULL,
    last_position VARCHAR(100) NOT NULL,
    progress_rate INT          NOT NULL,
    created_at    DATETIME     NOT NULL,
    updated_at    DATETIME     NOT NULL,
    PRIMARY KEY (id)
);

SELECT COUNT(*) INTO @idx_cp1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'content_progress' AND index_name = 'idx_content_progress_user_id';
SET @sql_cp1 = IF(COALESCE(@idx_cp1, 0) = 0, 'CREATE INDEX idx_content_progress_user_id ON content_progress (user_id)', 'SELECT 1');
PREPARE stmt_cp1 FROM @sql_cp1; EXECUTE stmt_cp1; DEALLOCATE PREPARE stmt_cp1;

SELECT COUNT(*) INTO @idx_cp2 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'content_progress' AND index_name = 'idx_content_progress_updated_at';
SET @sql_cp2 = IF(COALESCE(@idx_cp2, 0) = 0, 'CREATE INDEX idx_content_progress_updated_at ON content_progress (updated_at)', 'SELECT 1');
PREPARE stmt_cp2 FROM @sql_cp2; EXECUTE stmt_cp2; DEALLOCATE PREPARE stmt_cp2;

-- ── notifications ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS notifications (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    user_id    BIGINT       NOT NULL,
    message    VARCHAR(500) NOT NULL,
    is_read    TINYINT(1)   NOT NULL DEFAULT 0,
    created_at DATETIME     NOT NULL,
    PRIMARY KEY (id)
);

SELECT COUNT(*) INTO @idx_noti1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'notifications' AND index_name = 'idx_notifications_user_id';
SET @sql_noti1 = IF(COALESCE(@idx_noti1, 0) = 0, 'CREATE INDEX idx_notifications_user_id ON notifications (user_id)', 'SELECT 1');
PREPARE stmt_noti1 FROM @sql_noti1; EXECUTE stmt_noti1; DEALLOCATE PREPARE stmt_noti1;

SELECT COUNT(*) INTO @idx_noti2 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'notifications' AND index_name = 'idx_notifications_created_at';
SET @sql_noti2 = IF(COALESCE(@idx_noti2, 0) = 0, 'CREATE INDEX idx_notifications_created_at ON notifications (created_at)', 'SELECT 1');
PREPARE stmt_noti2 FROM @sql_noti2; EXECUTE stmt_noti2; DEALLOCATE PREPARE stmt_noti2;

-- ── news_views ────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS news_views (
    id        BIGINT   NOT NULL AUTO_INCREMENT,
    news_id   BIGINT   NOT NULL,
    viewed_at DATETIME NOT NULL,
    PRIMARY KEY (id)
);

SELECT COUNT(*) INTO @idx_nv1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'news_views' AND index_name = 'idx_news_views_news_id_viewed_at';
SET @sql_nv1 = IF(COALESCE(@idx_nv1, 0) = 0, 'CREATE INDEX idx_news_views_news_id_viewed_at ON news_views (news_id, viewed_at)', 'SELECT 1');
PREPARE stmt_nv1 FROM @sql_nv1; EXECUTE stmt_nv1; DEALLOCATE PREPARE stmt_nv1;

-- ── migration_failures ────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS migration_failures (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    guest_id      VARCHAR(36)  NOT NULL,
    user_id       BIGINT       NOT NULL,
    error_message TEXT,
    created_at    DATETIME     NOT NULL,
    PRIMARY KEY (id)
);
