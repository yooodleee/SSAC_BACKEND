-- V8(SET+PREPARE 방식) 이 level 컬럼을 추가하지 못한 경우를 보정한다.
--
-- 현재 production DB 상태: Hibernate 검증 결과 level 컬럼 부재 확인.
-- 단순 ALTER TABLE을 사용한다.
--   - level 컬럼이 없는 경우(정상): 컬럼 추가 성공 → Hibernate 검증 통과
--   - level 컬럼이 있는 경우(V7 또는 V8이 이미 추가): "Duplicate column name" 오류 발생
--     → ignore-migration-patterns: "*:failed" 로 건너뜀 → Hibernate 검증은 정상 통과
ALTER TABLE users ADD COLUMN level VARCHAR(20) NULL;
