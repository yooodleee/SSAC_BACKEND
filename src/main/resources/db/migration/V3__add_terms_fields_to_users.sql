-- 약관 동의 일시 컬럼 추가
-- serviceTerm, privacyTerm, ageVerification: 필수 동의 항목 (NOT NULL이 아님 - 기존 레코드 고려)
-- marketingTerm: 선택 동의 항목 (NULL 허용)
ALTER TABLE users
    ADD COLUMN service_term_agreed_at    DATETIME(6) NULL AFTER invalidated_before,
    ADD COLUMN privacy_term_agreed_at    DATETIME(6) NULL AFTER service_term_agreed_at,
    ADD COLUMN age_verification_agreed_at DATETIME(6) NULL AFTER privacy_term_agreed_at,
    ADD COLUMN marketing_term_agreed_at  DATETIME(6) NULL AFTER age_verification_agreed_at;
