-- V2: 메뉴 클릭 이벤트 수집 테이블 생성
CREATE TABLE menu_click_events (
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

CREATE INDEX idx_menu_click_events_menu_id_clicked_at
    ON menu_click_events (menu_id, clicked_at);

CREATE INDEX idx_menu_click_events_clicked_at
    ON menu_click_events (clicked_at);
