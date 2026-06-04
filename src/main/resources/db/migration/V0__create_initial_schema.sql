-- V0: Flyway 도입 이전에 JPA ddl-auto=create 로 생성된 초기 스키마
-- 신규 MySQL 인스턴스(CI, 로컬 테스트 컨테이너)에서 V1 이후 마이그레이션이 실행될 수 있도록
-- 원본 테이블을 IF NOT EXISTS 패턴으로 생성한다.
-- 기존 Production DB에는 이미 테이블이 존재하므로 IF NOT EXISTS 로 무시된다.

-- ── users ─────────────────────────────────────────────────────────────────────
-- V3: service_term_agreed_at/privacy_term_agreed_at/age_verification_agreed_at/marketing_term_agreed_at 추가
-- V7-V10: user_type/user_type_set_at/level 추가
-- V13: level_set_at/onboarding_completed 추가
-- V15: onboarding_score/onboarding_skipped 추가
-- V20: last_visited_at 추가
-- V21: name/birth_date/phone/gender 추가
-- V22: nickname_explicitly_set/deleted_at 추가
CREATE TABLE IF NOT EXISTS users (
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    email                VARCHAR(100) NOT NULL,
    password             VARCHAR(255) NULL,
    nickname             VARCHAR(20)  NOT NULL,
    provider             VARCHAR(20)  NULL,
    provider_id          VARCHAR(100) NULL,
    role                 VARCHAR(10)  NOT NULL,
    created_at           DATETIME     NOT NULL,
    updated_at           DATETIME     NOT NULL,
    invalidated_before   DATETIME     NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email    (email),
    UNIQUE KEY uk_users_nickname (nickname)
);

-- ── quizzes ───────────────────────────────────────────────────────────────────
-- V18: category/difficulty 추가
CREATE TABLE IF NOT EXISTS quizzes (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    title           VARCHAR(200) NOT NULL,
    description     VARCHAR(500) NULL,
    max_score       INT          NOT NULL,
    total_questions INT          NOT NULL,
    created_at      DATETIME     NOT NULL,
    PRIMARY KEY (id)
);

-- ── news ──────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS news (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    title        VARCHAR(300)  NOT NULL,
    summary      VARCHAR(1000) NOT NULL,
    view_count   INT           NOT NULL,
    published_at DATETIME      NOT NULL,
    PRIMARY KEY (id)
);

-- ── social_accounts ───────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS social_accounts (
    id               BIGINT      NOT NULL AUTO_INCREMENT,
    provider         VARCHAR(20) NOT NULL,
    provider_user_id VARCHAR(100) NOT NULL,
    user_id          BIGINT      NOT NULL,
    created_at       DATETIME    NOT NULL,
    PRIMARY KEY (id)
);

-- ── refresh_tokens ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    user_id     BIGINT      NOT NULL,
    token_hash  VARCHAR(64) NOT NULL,
    expires_at  DATETIME    NOT NULL,
    revoked     TINYINT(1)  NOT NULL,
    created_at  DATETIME    NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_refresh_tokens_token_hash (token_hash)
);

-- ── quiz_attempts ─────────────────────────────────────────────────────────────
-- V1: user_id 를 NULL 허용으로 변경, guest_id 컬럼 및 인덱스 추가
CREATE TABLE IF NOT EXISTS quiz_attempts (
    id           BIGINT   NOT NULL AUTO_INCREMENT,
    user_id      BIGINT   NOT NULL,
    quiz_id      BIGINT   NOT NULL,
    earned_score INT      NOT NULL,
    correct_count INT     NOT NULL,
    attempted_at DATETIME NOT NULL,
    PRIMARY KEY (id)
);

SELECT COUNT(*) INTO @idx_qa_uid FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'quiz_attempts' AND index_name = 'idx_quiz_attempts_user_id';
SET @sql_qa_uid = IF(COALESCE(@idx_qa_uid, 0) = 0, 'CREATE INDEX idx_quiz_attempts_user_id ON quiz_attempts (user_id)', 'SELECT 1');
PREPARE stmt_qa_uid FROM @sql_qa_uid; EXECUTE stmt_qa_uid; DEALLOCATE PREPARE stmt_qa_uid;

SELECT COUNT(*) INTO @idx_qa_at FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'quiz_attempts' AND index_name = 'idx_quiz_attempts_attempted_at';
SET @sql_qa_at = IF(COALESCE(@idx_qa_at, 0) = 0, 'CREATE INDEX idx_quiz_attempts_attempted_at ON quiz_attempts (attempted_at)', 'SELECT 1');
PREPARE stmt_qa_at FROM @sql_qa_at; EXECUTE stmt_qa_at; DEALLOCATE PREPARE stmt_qa_at;

-- ── questions ─────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS questions (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    quiz_id        BIGINT      NOT NULL,
    content        TEXT        NOT NULL,
    correct_answer VARCHAR(200) NOT NULL,
    points         INT         NOT NULL,
    question_order INT         NOT NULL,
    PRIMARY KEY (id)
);
