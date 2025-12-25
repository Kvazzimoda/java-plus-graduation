package ru.practicum.service.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStats;
import ru.practicum.service.repository.EndpointHitRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

    @Mock
    private EndpointHitRepository repository;

    @InjectMocks
    private StatsService statsService;

    @Test
    void saveHit_shouldConvertDtoAndSaveToRepository() {
        EndpointHitDto dto = new EndpointHitDto();
        dto.setApp("test-app");
        dto.setUri("/test");
        dto.setIp("127.0.0.1");
        dto.setTimestamp(LocalDateTime.now());

        statsService.saveHit(dto);

        verify(repository).save(argThat(hit ->
                hit.getApp().equals(dto.getApp()) &&
                        hit.getUri().equals(dto.getUri()) &&
                        hit.getIp().equals(dto.getIp()) &&
                        hit.getTimestamp().equals(dto.getTimestamp())
        ));
    }

    @Test
    void getStats_withUniqueFalse_shouldCallFindStats() {
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();
        List<String> uris = List.of("/test");

        ViewStats expectedStats = new ViewStats("test-app", "/test", 10L);
        when(repository.findStats(eq(start), eq(end), eq(uris))).thenReturn(List.of(expectedStats));

        List<ViewStats> result = statsService.getStats(start, end, uris, false);

        assertEquals(1, result.size());
        assertEquals(expectedStats, result.getFirst());
        verify(repository).findStats(start, end, uris);
        verify(repository, never()).findStatsUnique(any(), any(), any());
    }

    @Test
    void getStats_withUniqueTrue_shouldCallFindStatsUnique() {
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();
        List<String> uris = List.of("/test");

        ViewStats expectedStats = new ViewStats("test-app", "/test", 5L);
        when(repository.findStatsUnique(eq(start), eq(end), eq(uris))).thenReturn(List.of(expectedStats));

        List<ViewStats> result = statsService.getStats(start, end, uris, true);

        assertEquals(1, result.size());
        assertEquals(expectedStats, result.getFirst());
        verify(repository).findStatsUnique(start, end, uris);
        verify(repository, never()).findStats(any(), any(), any());
    }

    @Test
    void getStats_withNullUris_shouldPassNullToRepository() {
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();

        ViewStats expectedStats = new ViewStats("test-app", "/test", 20L);
        when(repository.findStats(eq(start), eq(end), eq(null))).thenReturn(List.of(expectedStats));

        List<ViewStats> result = statsService.getStats(start, end, null, false);

        assertEquals(1, result.size());
        verify(repository).findStats(start, end, null);
    }

    @Test
    void saveHit_withFutureTimestamp_shouldThrowIllegalArgumentException() {
        EndpointHitDto dto = new EndpointHitDto();
        dto.setApp("test-app");
        dto.setUri("/test");
        dto.setIp("127.0.0.1");
        dto.setTimestamp(LocalDateTime.now().plusHours(1)); // время в будущем

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> statsService.saveHit(dto)
        );

        assertEquals("Время запроса не может быть в будущем", exception.getMessage());

        // Проверяем, что repository.save не вызывался
        verify(repository, never()).save(any());
    }

    @Test
    void getStats_withEmptyUris_shouldPassNullToRepository() {
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();
        List<String> emptyUris = List.of(); // пустой список

        ViewStats expectedStats = new ViewStats("test-app", "/test", 15L);
        when(repository.findStats(eq(start), eq(end), eq(null))).thenReturn(List.of(expectedStats));

        List<ViewStats> result = statsService.getStats(start, end, emptyUris, false);

        assertEquals(1, result.size());
        verify(repository).findStats(start, end, null); // должен передать null вместо пустого списка
    }

    @Test
    void getStats_withEmptyUrisAndUniqueTrue_shouldPassNullToRepository() {
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();
        List<String> emptyUris = List.of(); // пустой список

        ViewStats expectedStats = new ViewStats("test-app", "/test", 8L);
        when(repository.findStatsUnique(eq(start), eq(end), eq(null))).thenReturn(List.of(expectedStats));

        List<ViewStats> result = statsService.getStats(start, end, emptyUris, true);

        assertEquals(1, result.size());
        verify(repository).findStatsUnique(start, end, null); // должен передать null вместо пустого списка
    }
}