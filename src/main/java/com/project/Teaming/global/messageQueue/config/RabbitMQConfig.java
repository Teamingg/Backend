package com.project.Teaming.global.messageQueue.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${server.id}") // application.yml에서 설정된 SERVER_ID 값을 주입
    private String serverId;

    @Value("${spring.rabbitmq.host}")
    private String rabbitHost;
    @Bean
    public Queue queue() {
        return QueueBuilder.durable(serverId)
                .withArgument("x-dead-letter-exchange", "notification.dlx")
                .withArgument("x-dead-letter-routing-key", "dlq.routing.key")
                .build();
    }
    @Bean
    public Binding binding(FanoutExchange exchange, Queue queue) {
        return BindingBuilder.bind(queue).to(exchange);
    }
    @Bean
    public FanoutExchange exchange() {
        return new FanoutExchange("notification.exchange");
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange("notification.dlx");
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable("notification.dlq").build();
    }

    @Bean
    public Binding dlqBinding() {
        return BindingBuilder
                .bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with("dlq.routing.key"); // 일반 큐에서 설정한 routing key
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(CachingConnectionFactory cachingConnectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(cachingConnectionFactory);
        rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter()); // ✅ JSON 메시지 변환기 적용
        return rabbitTemplate;
    }

    @Bean
    public CachingConnectionFactory cachingConnectionFactory() {
        CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory(rabbitHost);
        cachingConnectionFactory.setUsername("admin");
        cachingConnectionFactory.setPassword("password");
        cachingConnectionFactory.setChannelCacheSize(50);
        cachingConnectionFactory.setCacheMode(CachingConnectionFactory.CacheMode.CHANNEL);
        return cachingConnectionFactory;
    }
}