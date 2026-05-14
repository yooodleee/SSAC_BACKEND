-- IF NOT EXISTS: 컬럼이 이미 존재해도 오류 없이 통과 (멱등성 보장)
-- 배경: ddl-auto=update 긴급 배포 후 동일 컬럼을 Flyway가 재추가 시도하면
--       'Duplicate column name' 오류로 FAILED 상태가 되어 이후 모든 배포가 차단됨
ALTER TABLE users ADD COLUMN IF NOT EXISTS level_set_at         DATETIME    NULL;
ALTER TABLE users ADD COLUMN IF NOT EXISTS onboarding_completed TINYINT(1)  NOT NULL DEFAULT 0;

-- 기존에 level이 설정된 사용자는 온보딩 완료로 간주
UPDATE users SET onboarding_completed = 1 WHERE level IS NOT NULL AND onboarding_completed = 0;
