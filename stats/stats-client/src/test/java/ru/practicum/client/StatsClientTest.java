package ru.practicum.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.dto.EndpointHitDto;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class StatsClientTest {

    @Test
    void testStatsClientInterface_hasCorrectMethods() {
        Class<StatsClient> interfaceClass = StatsClient.class;

        assertTrue(interfaceClass.isInterface());

        Method[] methods = interfaceClass.getDeclaredMethods();
        assertEquals(2, methods.length);

        // Проверяем наличие метода hit
        boolean hasHitMethod = false;
        boolean hasGetStatMethod = false;

        for (Method method : methods) {
            if ("hit".equals(method.getName())) {
                hasHitMethod = true;
                assertEquals(void.class, method.getReturnType());
                assertEquals(1, method.getParameterCount());
                assertEquals(EndpointHitDto.class, method.getParameterTypes()[0]);
            }

            if ("getStat".equals(method.getName())) {
                hasGetStatMethod = true;
                assertEquals(Collection.class, method.getReturnType());
                assertEquals(4, method.getParameterCount());
                assertEquals(String.class, method.getParameterTypes()[0]);
                assertEquals(String.class, method.getParameterTypes()[1]);
                assertEquals(List.class, method.getParameterTypes()[2]);
                assertEquals(Boolean.class, method.getParameterTypes()[3]);
            }
        }

        assertTrue(hasHitMethod, "Interface should have hit method");
        assertTrue(hasGetStatMethod, "Interface should have getStat method");
    }
}
