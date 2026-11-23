package com.example.Backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessagingJacksonConfiguration {

    @Bean
    ObjectMapper messagingObjectMapper() {
        return new ObjectMapper();
    }
}
