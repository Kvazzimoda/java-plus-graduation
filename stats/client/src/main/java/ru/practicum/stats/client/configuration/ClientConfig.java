package ru.practicum.stats.client.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.practicum.stats.client.StatClient;

@Configuration
public class ClientConfig {
    @Bean
    public StatClient statClient(@Value("${stats-server.id:stats-server}") String statsServiceId,
                                 DiscoveryClient discoveryClient,
                                 RestTemplateBuilder builder) {
        return new StatClient(statsServiceId, discoveryClient, builder);
    }
}
