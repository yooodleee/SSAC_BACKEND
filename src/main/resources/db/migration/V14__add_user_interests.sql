CREATE TABLE IF NOT EXISTS user_interests (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    domain_id   VARCHAR(50) NOT NULL,
    created_at  DATETIME NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
