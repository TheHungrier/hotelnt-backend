package com.hotelnt.helper;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

@Configuration
public class AiClientConfig {

    @Bean
    @ConditionalOnMissingBean(AiClient.class)
    public AiClient defaultAiClient() {
        return (userMessage, suggested) -> Optional.empty();
    }
}
