package ru.practicum.ewm.config;

import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.practicum.client.StatsClient;
import ru.practicum.client.StatsClientImpl;

@Configuration
public class StatsClientConfig {

    @Bean
    public StatsClient statsClient(DiscoveryClient discoveryClient) {
        return new StatsClientImpl(discoveryClient);
    }
}
