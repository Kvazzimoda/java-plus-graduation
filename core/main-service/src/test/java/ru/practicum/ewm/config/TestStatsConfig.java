package ru.practicum.ewm.config;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import ru.practicum.client.StatsClient;

@TestConfiguration
public class TestStatsConfig {

    @Bean
    @Primary
    public StatsClient statsClient() {
        return Mockito.mock(StatsClient.class);
    }
}
