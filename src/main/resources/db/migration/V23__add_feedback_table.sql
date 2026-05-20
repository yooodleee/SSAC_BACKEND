-- V23__add_feedback_table.sql
-- 개발팀 문의 피드백 테이블 생성

CREATE TABLE IF NOT EXISTS feedbacks (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT NULL,
    message    TEXT NOT NULL,
    page_url   VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT NOW()
);
