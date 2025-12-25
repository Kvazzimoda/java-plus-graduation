package ru.practicum.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest
@ActiveProfiles("test")
class StatsServerApplicationTest {

    @Test
    void contextLoads() {
        // Проверяем, что контекст Spring успешно загружается
    }

    @Test
    void main_WhenRun_ThenNoExceptions() {
        // Проверяем, что метод main запускается без исключений
        assertDoesNotThrow(() -> StatsServerApplication.main(new String[]{}));
    }
}