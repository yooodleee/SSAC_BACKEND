CREATE TABLE IF NOT EXISTS level_histories (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id        BIGINT NOT NULL,
    previous_level VARCHAR(20) NOT NULL,
    new_level      VARCHAR(20) NOT NULL,
    created_at     DATETIME NOT NULL DEFAULT NOW(),
    FOREIGN KEY (user_id) REFERENCES users(id)
);
