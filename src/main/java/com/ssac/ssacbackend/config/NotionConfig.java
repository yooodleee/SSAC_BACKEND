package com.ssac.ssacbackend.config;

import java.net.http.HttpClient;
import java.time.Duration;
import notion.api.v1.NotionClient;
import notion.api.v1.http.OkHttp4Client;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Notion API 클라이언트 설정.
 *
 * <p>notion-sdk-jvm-core + okhttp4 조합으로 Notion API를 호출한다.
 */
@Configuration
@EnableConfigurationProperties(NotionProperties.class)
public class NotionConfig {

    /** OkHttp4Client: SDK 기본값과 동일하나 코드에 명시하여 의도를 드러낸다. */
    private static final int OKHTTP_CONNECT_TIMEOUT_MS = 5_000;
    private static final int OKHTTP_READ_TIMEOUT_MS    = 30_000;
    private static final int OKHTTP_WRITE_TIMEOUT_MS   = 30_000;

    @Bean
    public NotionClient notionClient(NotionProperties props) {
        NotionClient client = new NotionClient(props.getApiKey());
        client.setHttpClient(new OkHttp4Client(
            OKHTTP_CONNECT_TIMEOUT_MS,
            OKHTTP_READ_TIMEOUT_MS,
            OKHTTP_WRITE_TIMEOUT_MS
        ));
        return client;
    }

    /**
     * UnsupportedBlock 원본 타입 복원용 HttpClient.
     *
     * <p>connectTimeout: Notion API 서버 연결 실패 시 무한 대기 방지 (5초).
     * 요청별 타임아웃은 {@code fetchRawBlock()} 내 HttpRequest에 별도 지정한다.
     */
    @Bean
    public HttpClient httpClient() {
        return HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    }
}
