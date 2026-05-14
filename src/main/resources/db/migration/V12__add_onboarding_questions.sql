-- IF NOT EXISTS: 이미 테이블이 존재해도 오류 없이 통과 (멱등성 보장)
CREATE TABLE IF NOT EXISTS onboarding_questions (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_type       VARCHAR(20)  NOT NULL,
  question_order  INT          NOT NULL,
  content         VARCHAR(255) NOT NULL,
  is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
  created_at      DATETIME     NOT NULL,
  updated_at      DATETIME     NOT NULL
);

-- 데이터 중복 삽입 방지: 테이블이 비어 있을 때만 삽입
INSERT INTO onboarding_questions
  (user_type, question_order, content, is_active, created_at, updated_at)
SELECT vals.*
FROM (
  SELECT 'HIGH_SCHOOL' AS user_type, 1 AS question_order, '신용카드가 뭔지 알고 계신가요?'    AS content, TRUE AS is_active, NOW() AS created_at, NOW() AS updated_at UNION ALL
  SELECT 'HIGH_SCHOOL', 2, '청약통장 들어본 적 있나요?',        TRUE, NOW(), NOW() UNION ALL
  SELECT 'HIGH_SCHOOL', 3, '학자금 대출에 대해 알고 계신가요?', TRUE, NOW(), NOW() UNION ALL
  SELECT 'HIGH_SCHOOL', 4, '아르바이트 주휴수당 알고 계신가요?', TRUE, NOW(), NOW() UNION ALL
  SELECT 'HIGH_SCHOOL', 5, '월세 / 전세 차이 알고 계신가요?',   TRUE, NOW(), NOW() UNION ALL
  SELECT 'EARLY_CAREER', 1, '연말정산 직접 해본 적 있나요?',      TRUE, NOW(), NOW() UNION ALL
  SELECT 'EARLY_CAREER', 2, '퇴직연금(IRP) 들고 계신가요?',       TRUE, NOW(), NOW() UNION ALL
  SELECT 'EARLY_CAREER', 3, '신용점수 조회해본 적 있나요?',        TRUE, NOW(), NOW() UNION ALL
  SELECT 'EARLY_CAREER', 4, '전입신고 / 확정일자 알고 계신가요?', TRUE, NOW(), NOW() UNION ALL
  SELECT 'EARLY_CAREER', 5, 'ETF 투자 경험이 있으신가요?',        TRUE, NOW(), NOW()
) AS vals
WHERE NOT EXISTS (SELECT 1 FROM onboarding_questions LIMIT 1);
