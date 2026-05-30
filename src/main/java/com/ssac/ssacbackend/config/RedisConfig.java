package com.ssac.ssacbackend.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 설정.
 *
 * <p>홈 화면 캐시에 JSON 직렬화를 적용한다.
 * 콘텐츠 목록 캐시는 {@code NotionSyncService}에서 {@code StringRedisTemplate}으로 직접 관리한다.
 */
@Configuration
public class RedisConfig {

    /**
     * JavaTimeModule이 등록된 ObjectMapper를 기반으로 GenericJackson2JsonRedisSerializer를 생성한다.
     *
     * <p>기본 GenericJackson2JsonRedisSerializer는 새로운 ObjectMapper()를 사용하므로
     * JavaTimeModule 없이 LocalDateTime 직렬화 시 역직렬화에 실패한다.
     */
    private static GenericJackson2JsonRedisSerializer buildSerializer() {
        ObjectMapper om = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.WRAPPER_ARRAY);
        return new GenericJackson2JsonRedisSerializer(om);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(buildSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(buildSerializer());
        return template;
    }

}
