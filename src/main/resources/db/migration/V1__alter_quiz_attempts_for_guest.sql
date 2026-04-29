-- V1: quiz_attempts 테이블에 비회원(Guest) 지원을 위한 컬럼 및 인덱스 추가
-- 1. user_id 컬럼을 NULL 허용으로 변경 (비회원은 user_id가 없음)
ALTER TABLE quiz_attempts MODIFY user_id BIGINT NULL;

-- 2. guest_id 컬럼 추가 (UUID 저장용, 36자)
ALTER TABLE quiz_attempts ADD COLUMN guest_id VARCHAR(36) AFTER user_id;

-- 3. guest_id 기반 조회를 위한 인덱스 추가
CREATE INDEX idx_quiz_attempts_guest_id ON quiz_attempts (guest_id);
