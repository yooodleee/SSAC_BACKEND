package com.ssac.ssacbackend.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.web.filter.CharacterEncodingFilter;

import java.nio.charset.StandardCharsets;

/**
 * 프로젝트 전반의 문자 인코딩을 UTF-8로 고정한다.
 *
 * <p>Spring Boot 의 {@code server.servlet.encoding.*} 자동 설정과 함께 동작하여
 * HTTP 레이어뿐 아니라 MessageSource(i18n)까지 일관된 UTF-8 환경을 보장한다.
 */
@Configuration
public class EncodingConfig {

    /**
     * 모든 HTTP 요청·응답에 UTF-8 인코딩을 강제하는 필터.
     *
     * <p>{@code application.properties}의 {@code server.servlet.encoding.force=true} 설정이
     * 이미 동일한 필터를 등록하므로 중복 등록을 방지하기 위해 조건부로 사용한다.
     * 필요 시 아래 Bean을 활성화하여 명시적으로 제어할 수 있다.
     */
    // @Bean  // application.properties 설정으로 충분할 경우 주석 유지
    public CharacterEncodingFilter characterEncodingFilter() {
        CharacterEncodingFilter filter = new CharacterEncodingFilter();
        filter.setEncoding(StandardCharsets.UTF_8.name());
        filter.setForceRequestEncoding(true);
        filter.setForceResponseEncoding(true);
        return filter;
    }

    /**
     * i18n 메시지 소스를 UTF-8로 로드한다.
     *
     * <p>src/main/resources/messages*.properties 파일이 UTF-8로 저장된 경우
     * 이 설정 없이 기본 ISO-8859-1로 읽히면 한글이 깨진다.
     */
    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource =
                new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:messages");
        messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());
        messageSource.setCacheSeconds(3600);
        messageSource.setUseCodeAsDefaultMessage(true);
        return messageSource;
    }
}
