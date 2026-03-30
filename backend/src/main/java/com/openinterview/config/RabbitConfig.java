package com.openinterview.config;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    @Bean
    public DirectExchange interviewDirectExchange() {
        return new DirectExchange("interview.direct", true, false);
    }
}
