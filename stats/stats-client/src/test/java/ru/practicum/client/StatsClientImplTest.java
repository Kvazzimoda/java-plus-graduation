package ru.practicum.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;
import ru.practicum.dto.EndpointHitDto;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatsClientImplTest {

    @Mock
    private DiscoveryClient discoveryClient;

    private StatsClientImpl statsClient;

    @BeforeEach
    void setUp() {
        // Создаем реальный клиент
        statsClient = new StatsClientImpl(discoveryClient);

        // Настраиваем RetryTemplate для тестов (упрощенный)
        RetryTemplate retryTemplate = new RetryTemplate();
        ReflectionTestUtils.setField(statsClient, "retryTemplate", retryTemplate);
    }

    @Test
    void constructor_shouldCreateClient() {
        assertNotNull(statsClient);
    }

    @Test
    void getStat_nullStart_shouldThrow() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> statsClient.getStat(
                        null,
                        "2024-01-02 00:00:00",
                        List.of("/test"),
                        false
                )
        );

        assertEquals("Диапазон не может быть null", ex.getMessage());
    }

    @Test
    void getStat_startAfterEnd_shouldThrow() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> statsClient.getStat(
                        "2024-01-03 00:00:00",
                        "2024-01-02 00:00:00",
                        List.of("/test"),
                        false
                )
        );

        assertEquals("Неверный диапазон дат", ex.getMessage());
    }

    @Test
    void hit_shouldThrowWhenServiceNotFound() {
        // Arrange
        when(discoveryClient.getInstances("stats-server"))
                .thenReturn(List.of()); // Пустой список

        EndpointHitDto dto = new EndpointHitDto(
                "app",
                "/test",
                "127.0.0.1",
                LocalDateTime.now()
        );

        // Act & Assert
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> statsClient.hit(dto)
        );

        assertEquals("Stats server not found in Eureka", ex.getMessage());
    }
}