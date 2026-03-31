package com.openinterview.ai;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiConfiguration {

    @Bean
    @Primary
    public AiAdapter aiAdapter(AiAdapterFactory factory) {
        return factory.getAdapter();
    }
}

