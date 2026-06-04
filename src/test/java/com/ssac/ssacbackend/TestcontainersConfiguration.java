package com.ssac.ssacbackend;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MySQLContainer;

/**
 * 통합 테스트용 MySQL Testcontainers 공유 설정.
 *
 * <p>@Import(TestcontainersConfiguration.class) 를 선언한 테스트 클래스에
 * MySQLContainer 를 {@link ServiceConnection} 으로 주입한다.
 * Spring Boot 컨텍스트 캐싱으로 동일 설정의 테스트 간 컨테이너가 재사용된다.
 */
@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    MySQLContainer<?> mysqlContainer() {
        return new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("ssac_test")
            .withUsername("ssac")
            .withPassword("ssac");
    }
}
