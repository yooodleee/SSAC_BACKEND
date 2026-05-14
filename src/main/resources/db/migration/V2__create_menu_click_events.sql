-- V2: 메뉴 클릭 이벤트 수집 테이블 생성
CREATE TABLE IF NOT EXISTS menu_click_events (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    event_type  VARCHAR(50)  NOT NULL,
    menu_id     VARCHAR(100) NOT NULL,
    menu_name   VARCHAR(200) NOT NULL,
    user_id     VARCHAR(100) NULL,
    guest_id    VARCHAR(100) NULL,
    clicked_at  DATETIME(6)  NOT NULL,
    page_context VARCHAR(200) NOT NULL,
    created_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id)
);

SELECT COUNT(*) INTO @idx_mce1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'menu_click_events' AND index_name = 'idx_menu_click_events_menu_id_clicked_at';
SET @sql_mce1 = IF(COALESCE(@idx_mce1, 0) = 0, 'CREATE INDEX idx_menu_click_events_menu_id_clicked_at ON menu_click_events (menu_id, clicked_at)', 'SELECT 1');
PREPARE stmt_mce1 FROM @sql_mce1; EXECUTE stmt_mce1; DEALLOCATE PREPARE stmt_mce1;

SELECT COUNT(*) INTO @idx_mce2 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'menu_click_events' AND index_name = 'idx_menu_click_events_clicked_at';
SET @sql_mce2 = IF(COALESCE(@idx_mce2, 0) = 0, 'CREATE INDEX idx_menu_click_events_clicked_at ON menu_click_events (clicked_at)', 'SELECT 1');
PREPARE stmt_mce2 FROM @sql_mce2; EXECUTE stmt_mce2; DEALLOCATE PREPARE stmt_mce2;
