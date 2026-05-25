package com.ssac.ssacbackend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Notion API 연동 설정 프로퍼티.
 */
@ConfigurationProperties(prefix = "notion")
@Getter
@Setter
public class NotionProperties {

    private String apiKey;
    private String databaseId;
}
