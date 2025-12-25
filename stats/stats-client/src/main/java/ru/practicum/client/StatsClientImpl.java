package ru.practicum.client;

import lombok.extern.slf4j.Slf4j;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStats;


import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;

@Slf4j
public class StatsClientImpl implements StatsClient {

    private final RestClient restClient;
    private final DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public StatsClientImpl(
            @LoadBalanced RestClient.Builder builder
    ) {
        this.restClient = builder
                .baseUrl("http://stats-server") // ← SERVICE-ID
                .build();
    }

    @Override
    public void hit(EndpointHitDto endpointHitDto) {
        restClient.post()
                .uri("/hit")
                .contentType(MediaType.APPLICATION_JSON)
                .body(endpointHitDto)
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public Collection<ViewStats> getStat(String start, String end,
                                         List<String> urls, Boolean unique) {

        if (start == null || end == null) {
            log.warn("Диапазон не может содержать null");
            throw new IllegalArgumentException("Диапазон не может содержать null");
        }

        LocalDateTime startDateTime = LocalDateTime.parse(start, formatter);
        LocalDateTime endDateTime = LocalDateTime.parse(end, formatter);

        if (startDateTime.isAfter(endDateTime)) {
            log.warn("Задан неверный диапазон");
            throw new IllegalArgumentException("Задан неверный диапазон");
        }

        Collection<ViewStats> stats = restClient.get()
                .uri(uriBuilder -> {
                    UriBuilder builder = uriBuilder.path("/stats")
                            .queryParam("start", start)
                            .queryParam("end", end);

                    if (urls != null && !urls.isEmpty()) {
                        urls.forEach(url -> builder.queryParam("uris", url));
                    }

                    if (unique != null) {
                        builder.queryParam("unique", unique);
                    }

                    return builder.build();
                })
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        log.info("Запрос статистики выполнен с параметрами: start={}, end={}, urls={}, unique={}",
                start, end, urls, unique);

        return stats;
    }
}

