-- 에러 로그 영속 테이블
-- WARN 레벨: 7일 보존 / ERROR 레벨: 30일 보존
-- traceId 기반 단일 요청 전체 흐름 추적 지원
CREATE TABLE IF NOT EXISTS error_logs (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    trace_id    VARCHAR(36)  NOT NULL,
    level       VARCHAR(10)  NOT NULL,
    error_code  VARCHAR(20)  NOT NULL,
    method      VARCHAR(10),
    path        VARCHAR(255),
    user_id     VARCHAR(255),
    message     TEXT,
    stack_trace TEXT,
    created_at  DATETIME     NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_error_logs_trace_id       ON error_logs (trace_id);
CREATE INDEX IF NOT EXISTS idx_error_logs_level_created  ON error_logs (level, created_at);
