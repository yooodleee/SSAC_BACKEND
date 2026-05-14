package com.ssac.ssacbackend.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Flyway 마이그레이션 전략 설정.
 *
 * <p>배포 전 repair()를 실행하여 schema_history의 FAILED 항목을 제거한 후 migrate()를 수행한다.
 * ddl-auto=update 긴급 배포로 인해 FAILED 상태가 남은 경우에도 안전하게 재실행된다.
 * V12/V13 등 모든 마이그레이션은 IF NOT EXISTS 멱등성 보장으로 재실행 시에도 안전하다.
 */
@Configuration
public class FlywayConfig {

    @Bean
    public FlywayMigrationStrategy repairThenMigrate() {
        return flyway -> {
            flyway.repair();
            flyway.migrate();
        };
    }
}
