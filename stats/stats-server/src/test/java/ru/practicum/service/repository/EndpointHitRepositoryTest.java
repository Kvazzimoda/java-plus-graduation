package ru.practicum.service.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import ru.practicum.dto.ViewStats;
import ru.practicum.service.model.EndpointHit;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class EndpointHitRepositoryTest {

    @Autowired
    private EndpointHitRepository repository;

    private final LocalDateTime now = LocalDateTime.now();

    @BeforeEach
    void setUp() {
        repository.deleteAll();

        repository.save(EndpointHit.builder()
                .app("app1").uri("/uri1").ip("1.1.1.1").timestamp(now.minusHours(1))
                .build());
        repository.save(EndpointHit.builder()
                .app("app1").uri("/uri1").ip("1.1.1.2").timestamp(now.minusHours(1))
                .build());
        repository.save(EndpointHit.builder()
                .app("app1").uri("/uri2").ip("1.1.1.1").timestamp(now.minusHours(1))
                .build());
        repository.save(EndpointHit.builder()
                .app("app2").uri("/uri3").ip("2.2.2.2").timestamp(now.minusHours(1))
                .build());
    }

    @Test
    void findStats_shouldReturnCorrectCounts() {
        List<ViewStats> stats = repository.findStats(now.minusDays(1), now.plusDays(1), null);

        assertEquals(3, stats.size()); // /uri1, /uri2, /uri3

        ViewStats uri1 = stats.stream().filter(s -> s.getUri().equals("/uri1")).findFirst().orElse(null);
        assertNotNull(uri1);
        assertEquals(2L, uri1.getHits()); // два хита на /uri1
    }

    @Test
    void findStatsUnique_shouldReturnCorrectUniqueCounts() {
        List<ViewStats> stats = repository.findStatsUnique(now.minusDays(1), now.plusDays(1), null);

        ViewStats uri1 = stats.stream().filter(s -> s.getUri().equals("/uri1")).findFirst().orElse(null);
        assertNotNull(uri1);
        assertEquals(2L, uri1.getHits()); // два уникальных IP на /uri1

        ViewStats uri2 = stats.stream().filter(s -> s.getUri().equals("/uri2")).findFirst().orElse(null);
        assertEquals(1L, uri2.getHits());
    }

    @Test
    void findStats_withUriFilter_shouldFilterCorrectly() {
        List<ViewStats> stats = repository.findStats(now.minusDays(1), now.plusDays(1), List.of("/uri1"));

        assertEquals(1, stats.size());
        assertEquals("/uri1", stats.getFirst().getUri());
    }
}