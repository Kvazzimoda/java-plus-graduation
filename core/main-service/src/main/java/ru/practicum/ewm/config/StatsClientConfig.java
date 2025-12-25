package ru.practicum.ewm.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import ru.practicum.client.StatsClient;
import ru.practicum.client.StatsClientImpl;

@Configuration
public class StatsClientConfig {

    @Bean
    @LoadBalanced
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    public StatsClient statsClient(RestClient.Builder builder) {
        return new StatsClientImpl(builder);
    }
}


