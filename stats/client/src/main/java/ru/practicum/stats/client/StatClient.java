package ru.practicum.stats.client;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import ru.practicum.stats.client.exception.StatsServerUnavailable;
import ru.practicum.stats.dto.dto.EndpointHitDto;
import ru.practicum.stats.dto.dto.ViewStatsDto;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class StatClient {
    private final RestTemplate restTemplate;
    private final DiscoveryClient discoveryClient;
    private final String statsServiceId;

    private static final String API_PREFIX = "/";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public StatClient(String statsServiceId,
                      DiscoveryClient discoveryClient,
                      RestTemplateBuilder builder) {
        this.statsServiceId = statsServiceId;
        this.discoveryClient = discoveryClient;
        this.restTemplate = builder
                .requestFactory(() -> new HttpComponentsClientHttpRequestFactory())
                .build();
    }

    public ResponseEntity<Void> hit(EndpointHitDto hitDto) {
        String serverUrl = getStatsServerUrl();
        return restTemplate.postForEntity(serverUrl + API_PREFIX + "hit", hitDto, Void.class);
    }

    public ResponseEntity<List<ViewStatsDto>> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        String serverUrl = getStatsServerUrl();
        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromUriString(serverUrl + API_PREFIX + "stats")
                .queryParam("start", start.format(DATE_TIME_FORMATTER))
                .queryParam("end", end.format(DATE_TIME_FORMATTER));

        if (uris != null && !uris.isEmpty()) {
            uriBuilder.queryParam("uris", uris.toArray());
        }

        if (unique != null) {
            uriBuilder.queryParam("unique", unique);
        }

        return restTemplate.exchange(
                uriBuilder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<ViewStatsDto>>() {
                }
        );
    }

    private String getStatsServerUrl() {
        try {
            List<ServiceInstance> instances = discoveryClient.getInstances(statsServiceId);
            if (instances == null || instances.isEmpty()) {
                throw new StatsServerUnavailable(
                        "Сервис статистики с ID '" + statsServiceId + "' не найден в Eureka"
                );
            }

            ServiceInstance instance = instances.getFirst();
            return instance.getUri().toString();

        } catch (Exception exception) {
            throw new StatsServerUnavailable(
                    "Ошибка обнаружения адреса сервиса статистики с id: " + statsServiceId,
                    exception
            );
        }
    }
}
