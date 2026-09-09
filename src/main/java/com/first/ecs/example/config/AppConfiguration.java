package com.first.ecs.example.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@Getter
public class AppConfiguration {
    @Value("${app.service.url}")
    private String serviceUrl;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
