-- V8/V9 보정 최종본: level 컬럼이 없는 경우에만 추가한다.
--
-- V8 실패 원인: SET @var = (SELECT ...) 방식에서 @var가 NULL로 평가되는 버그
--   → SELECT COUNT(*) INTO @var 방식으로 수정 (단일 행 결과 직접 할당, NULL 불가)
-- V9 실패 원인: 단순 ALTER TABLE → column 이미 존재 시 "Duplicate column name" FAILED 상태
--   → information_schema 조회 후 조건부 실행으로 수정
-- COALESCE: @level_col_exists가 예외적으로 NULL인 경우를 방어

SELECT COUNT(*) INTO @level_col_exists
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name   = 'users'
  AND column_name  = 'level';

SET @add_level_sql = IF(
    COALESCE(@level_col_exists, 0) = 0,
    'ALTER TABLE users ADD COLUMN level VARCHAR(20) NULL',
    'SELECT 1'
);

PREPARE add_level_stmt FROM @add_level_sql;
EXECUTE add_level_stmt;
DEALLOCATE PREPARE add_level_stmt;
