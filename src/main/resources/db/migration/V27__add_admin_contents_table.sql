-- 관리자 콘텐츠 관리 테이블
-- 학습 콘텐츠(contents)와 별도로 관리자가 생성/편집/게시하는 CMS 콘텐츠

CREATE TABLE IF NOT EXISTS admin_contents (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    title         VARCHAR(500) NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'NOT_STARTED',
    is_completed  BOOLEAN      NOT NULL DEFAULT FALSE,
    thumbnail_url VARCHAR(1000) NULL,
    body          LONGTEXT     NULL,
    author_id     BIGINT       NOT NULL,
    published_at  DATETIME     NULL,
    created_at    DATETIME     NOT NULL,
    updated_at    DATETIME     NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (author_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS admin_content_categories (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    content_id BIGINT      NOT NULL,
    category   VARCHAR(50) NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (content_id) REFERENCES admin_contents(id)
);

CREATE TABLE IF NOT EXISTS admin_content_domains (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    content_id BIGINT      NOT NULL,
    domain     VARCHAR(50) NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (content_id) REFERENCES admin_contents(id)
);

SELECT COUNT(*) INTO @idx_admin_contents_created
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name = 'admin_contents'
  AND index_name = 'idx_admin_contents_created_at';

SET @sql_idx = IF(COALESCE(@idx_admin_contents_created, 0) = 0,
    'CREATE INDEX idx_admin_contents_created_at ON admin_contents (created_at DESC)',
    'SELECT 1');
PREPARE stmt FROM @sql_idx;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
