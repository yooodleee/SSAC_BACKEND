package com.ssac.ssacbackend.config;

import java.net.http.HttpClient;
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

    @Bean
    public NotionClient notionClient(NotionProperties props) {
        NotionClient client = new NotionClient(props.getApiKey());
        client.setHttpClient(new OkHttp4Client());
        return client;
    }

    @Bean
    public HttpClient httpClient() {
        return HttpClient.newHttpClient();
    }
}
