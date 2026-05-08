-- V5: attempt_answers 테이블 생성
-- 퀴즈 응시(quiz_attempts) 내 문항별 답안 및 채점 결과 저장
CREATE TABLE IF NOT EXISTS attempt_answers (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    quiz_attempt_id  BIGINT       NOT NULL,
    question_id      BIGINT       NOT NULL,
    selected_answer  VARCHAR(200) NOT NULL,
    correct          TINYINT(1)   NOT NULL,
    earned_points    INT          NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_attempt_answers_quiz_attempt_id ON attempt_answers (quiz_attempt_id);
