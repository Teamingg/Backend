package com.project.Teaming.global.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Profile("local")
@Configuration
@EnableRedisRepositories
@RequiredArgsConstructor
public class LocalRedisConfig {

    private final RedisProperties redisProperties;

    private RedisConnectionFactory redisConnectionFactory(int databaseIndex) {
        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
        redisStandaloneConfiguration.setHostName(redisProperties.getHost());
        redisStandaloneConfiguration.setPort(redisProperties.getPort());
        redisStandaloneConfiguration.setPassword(redisProperties.getPassword());
        redisStandaloneConfiguration.setDatabase(databaseIndex);
        LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(redisStandaloneConfiguration);
        connectionFactory.afterPropertiesSet(); // 명시적으로 초기화
        return connectionFactory;
    }

    // 기본 db(0번)용 RedisTemplate
    @Bean
    public RedisTemplate<?, ?> redisTemplate() {
        // 0번 DB(기본값) 설정
        // redisTemplate 를 받아와서 set, get, delete 를 사용
        RedisTemplate<byte[], byte[]> redisTemplate = new RedisTemplate<>();
        /*
         * setKeySerializer, setValueSerializer 설정
         * redis-cli 을 통해 직접 데이터를 조회 시 알아볼 수 없는 형태로 출력되는 것을 방지
         */
        redisTemplate.setConnectionFactory(redisConnectionFactory(0));

        return redisTemplate;
    }


    // 게시글 db(1번)용 RedisTemplate
    @Bean
    public RedisTemplate<String, Object> postRedisTemplate() {
        // 1번 DB에서 사용할 RedisTemplate 설정
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory(1));

        // 직렬화
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());

        return template;
    }
    // 강퇴, 탈퇴한 팀원 및 지원자 db(2번) 용 RedisTemplate
    @Bean
    public RedisTemplate<String, Object> participationRedisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory(2));
        // JSON 직렬화 설정
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(redisObjectMapper());
        template.setKeySerializer(new StringRedisSerializer()); // 키 직렬화
        template.setValueSerializer(serializer);               // 값 직렬화
        template.setHashKeySerializer(new StringRedisSerializer()); // 해시 키 직렬화
        template.setHashValueSerializer(serializer);                // 해시 값 직렬화

        return template;
    }

    // sse 연결한 사용자 공유용
    @Bean
    public StringRedisTemplate stringRedisTemplate() {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(redisConnectionFactory(3));
        return template;
    }

    @Bean
    public ObjectMapper redisObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        // Redis에서만 사용할 타입 정보 활성화
        objectMapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder()
                        .allowIfSubType(Object.class)
                        .build(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        return objectMapper;
    }
}
