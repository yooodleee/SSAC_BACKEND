package com.ssac.ssacbackend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Flyway 마이그레이션 + Hibernate 스키마 검증 통합 테스트.
 *
 * <p>새 JPA 엔티티를 추가할 때 Flyway 마이그레이션 스크립트를 누락하면
 * 이 테스트가 실패한다. prod 배포 전에 로컬/CI에서 미리 감지하기 위한 안전망이다.
 *
 * <p>실패 시: src/main/resources/db/migration/ 에 누락된 테이블을 생성하는
 * V{N}__create_{table_name}.sql 을 추가하라.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@ActiveProfiles("schematest")
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.flyway.enabled=true",
    "spring.flyway.locations=classpath:db/migration",
    "spring.flyway.baseline-on-migrate=true",
    "spring.data.redis.host=localhost",
    "spring.data.redis.port=6379",
    "spring.data.redis.password=",
})
class SchemaValidationTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("ssac_test")
        .withUsername("ssac")
        .withPassword("ssac");

    @Test
    void flywayMigrations_matchAllJpaEntities() {
        // Spring 컨텍스트가 로드되면 검증 완료:
        // 1. Flyway가 모든 마이그레이션 스크립트를 MySQL에 적용
        // 2. Hibernate가 ddl-auto=validate 로 모든 엔티티 테이블을 검증
        // 테이블 또는 컬럼이 누락된 경우 컨텍스트 로드 실패 → 테스트 실패
    }
}
