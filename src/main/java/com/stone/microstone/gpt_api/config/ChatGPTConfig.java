package com.stone.microstone.gpt_api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

@Configuration
public class ChatGPTConfig {

    @Value("${gpt4o.api.key}")
    private String apiKey;

    @Value("${gpt4o.api.url}")
    private String apiUrl;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    public HttpHeaders httpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.set("Content-Type", "application/json");
        return headers;
    }

    public String getApiUrl() {
        return apiUrl;
    }
}
