-- V31: 누락된 FK 제약 조건 추가
--
-- 대상 테이블 4개 (content_view_histories는 V22에서 이미 FK 존재):
--   1. notifications.user_id          → users(id)    ON DELETE CASCADE
--   2. refresh_tokens.user_id         → users(id)    ON DELETE CASCADE
--   3. content_progress.content_id    → contents(id) ON DELETE SET NULL  (nullable 레거시 레코드 보존)
--   4. news_views.news_id             → news(id)     ON DELETE CASCADE
--
-- 제외 대상:
--   - migration_failures.user_id : 감사 로그 목적 → 사용자 삭제 후에도 보존
--   - quiz_attempts.user_id      : 비회원(guest) 지원으로 의도적 nullable
--   - menu_click_events.user_id  : String 타입 (이벤트 수집용, FK 불가)
--   - error_logs.user_id         : String 타입 (로그 수집용, FK 불가)
--
-- 멱등성: information_schema.TABLE_CONSTRAINTS 체크로 중복 실행 방지

-- ── 1. notifications.user_id → users(id) ──────────────────────────────────────
SELECT COUNT(*) INTO @fk_noti_user
FROM information_schema.TABLE_CONSTRAINTS
WHERE TABLE_SCHEMA    = DATABASE()
  AND TABLE_NAME      = 'notifications'
  AND CONSTRAINT_NAME = 'fk_notifications_user_id'
  AND CONSTRAINT_TYPE = 'FOREIGN KEY';

SET @sql = IF(COALESCE(@fk_noti_user, 0) = 0,
    'ALTER TABLE notifications ADD CONSTRAINT fk_notifications_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ── 2. refresh_tokens.user_id → users(id) ─────────────────────────────────────
SELECT COUNT(*) INTO @fk_rt_user
FROM information_schema.TABLE_CONSTRAINTS
WHERE TABLE_SCHEMA    = DATABASE()
  AND TABLE_NAME      = 'refresh_tokens'
  AND CONSTRAINT_NAME = 'fk_refresh_tokens_user_id'
  AND CONSTRAINT_TYPE = 'FOREIGN KEY';

SET @sql = IF(COALESCE(@fk_rt_user, 0) = 0,
    'ALTER TABLE refresh_tokens ADD CONSTRAINT fk_refresh_tokens_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ── 3. content_progress.content_id → contents(id) ────────────────────────────
-- content_id는 nullable: 레거시 진행 기록은 content_id = NULL로 유지됨.
-- 콘텐츠 삭제 시 ON DELETE SET NULL → 레거시 레코드로 자동 전환.
SELECT COUNT(*) INTO @fk_cp_content
FROM information_schema.TABLE_CONSTRAINTS
WHERE TABLE_SCHEMA    = DATABASE()
  AND TABLE_NAME      = 'content_progress'
  AND CONSTRAINT_NAME = 'fk_content_progress_content_id'
  AND CONSTRAINT_TYPE = 'FOREIGN KEY';

SET @sql = IF(COALESCE(@fk_cp_content, 0) = 0,
    'ALTER TABLE content_progress ADD CONSTRAINT fk_content_progress_content_id FOREIGN KEY (content_id) REFERENCES contents(id) ON DELETE SET NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ── 4. news_views.news_id → news(id) ─────────────────────────────────────────
SELECT COUNT(*) INTO @fk_nv_news
FROM information_schema.TABLE_CONSTRAINTS
WHERE TABLE_SCHEMA    = DATABASE()
  AND TABLE_NAME      = 'news_views'
  AND CONSTRAINT_NAME = 'fk_news_views_news_id'
  AND CONSTRAINT_TYPE = 'FOREIGN KEY';

SET @sql = IF(COALESCE(@fk_nv_news, 0) = 0,
    'ALTER TABLE news_views ADD CONSTRAINT fk_news_views_news_id FOREIGN KEY (news_id) REFERENCES news(id) ON DELETE CASCADE',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
