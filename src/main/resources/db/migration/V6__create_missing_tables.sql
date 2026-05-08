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

CREATE INDEX IF NOT EXISTS idx_content_progress_user_id
    ON content_progress (user_id);

CREATE INDEX IF NOT EXISTS idx_content_progress_updated_at
    ON content_progress (updated_at);

-- ── notifications ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS notifications (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    user_id    BIGINT       NOT NULL,
    message    VARCHAR(500) NOT NULL,
    is_read    TINYINT(1)   NOT NULL DEFAULT 0,
    created_at DATETIME     NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_notifications_user_id
    ON notifications (user_id);

CREATE INDEX IF NOT EXISTS idx_notifications_created_at
    ON notifications (created_at);

-- ── news_views ────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS news_views (
    id        BIGINT   NOT NULL AUTO_INCREMENT,
    news_id   BIGINT   NOT NULL,
    viewed_at DATETIME NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_news_views_news_id_viewed_at
    ON news_views (news_id, viewed_at);

-- ── migration_failures ────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS migration_failures (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    guest_id      VARCHAR(36)  NOT NULL,
    user_id       BIGINT       NOT NULL,
    error_message TEXT,
    created_at    DATETIME     NOT NULL,
    PRIMARY KEY (id)
);
