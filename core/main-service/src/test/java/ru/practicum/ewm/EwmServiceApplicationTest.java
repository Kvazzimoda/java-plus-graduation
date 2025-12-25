package ru.practicum.ewm;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class EwmServiceApplicationTest {

    @Test
    void contextLoads() {
        // Проверяем, что контекст Spring успешно загружается
    }
}
