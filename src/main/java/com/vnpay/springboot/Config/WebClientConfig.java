package com.vnpay.springboot.Config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    @Qualifier("vnpayRecurringWebClient")
    public WebClient vnpayRecurringWebClient(VNPayRecurringConfig recurringConfig, WebClient.Builder webClientBuilder) {
        return webClientBuilder.baseUrl(recurringConfig.getApiUrl()).build();
    }
}