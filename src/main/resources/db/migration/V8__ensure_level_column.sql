-- V7 스키마 불일치 보정: level 컬럼이 없는 경우에만 추가한다.
--
-- 적용 배경:
--   V7(add_user_type)이 이전 배포에서 user_type/user_type_set_at 만 포함된 버전으로
--   적용된 후 level 컬럼이 추가되어 체크섬 불일치 또는 FAILED 상태가 발생한 경우를 보정한다.
--   MySQL은 ADD COLUMN IF NOT EXISTS를 지원하지 않으므로 information_schema를 조회한 뒤
--   SET + PREPARE 방식으로 조건부 실행한다.
SET @col_count = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name   = 'users'
      AND column_name  = 'level'
);

SET @sql = IF(
    @col_count = 0,
    'ALTER TABLE users ADD COLUMN level VARCHAR(20) NULL',
    'SELECT 1 -- level column already exists, skip'
);

PREPARE ensure_level FROM @sql;
EXECUTE ensure_level;
DEALLOCATE PREPARE ensure_level;
