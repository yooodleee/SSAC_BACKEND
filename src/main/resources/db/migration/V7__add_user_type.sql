-- 사용자 유형(userType), 유형 설정 일시(userTypeSetAt), 레벨(level) 컬럼 추가
ALTER TABLE users ADD COLUMN IF NOT EXISTS user_type        VARCHAR(20) NULL;
ALTER TABLE users ADD COLUMN IF NOT EXISTS user_type_set_at  DATETIME NULL;
ALTER TABLE users ADD COLUMN IF NOT EXISTS level             VARCHAR(20) NULL;
