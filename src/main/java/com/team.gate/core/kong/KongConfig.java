package com.team.gate.core.kong;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class KongConfig {

    @Bean
    WebClient kongAdminClient(
            @Value("${kong.admin.base-url}") String baseUrl,
            @Value("${kong.admin.token:}") String token
    ) {
        WebClient.Builder b = WebClient.builder().baseUrl(baseUrl);
        if (token != null && !token.isBlank()) {
            b.defaultHeader("Kong-Admin-Token", token);
        }
        return b.build();
    }
}