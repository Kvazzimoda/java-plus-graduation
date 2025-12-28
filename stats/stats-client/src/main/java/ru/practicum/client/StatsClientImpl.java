package ru.practicum.client;

import lombok.extern.slf4j.Slf4j;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.MaxAttemptsRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStats;


import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;

@Component
@Slf4j
public class StatsClientImpl implements StatsClient {

    private static final String STATS_SERVICE_ID = "stats-server";

    private final DiscoveryClient discoveryClient;
    private final RestClient restClient;
    private final RetryTemplate retryTemplate;

    private final DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public StatsClientImpl(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
        this.restClient = RestClient.create();
        this.retryTemplate = createRetryTemplate();
    }


    @Override
    public void hit(EndpointHitDto dto) {
        URI uri = makeUri("/hit");

        restClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .body(dto)
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public Collection<ViewStats> getStat(
            String start,
            String end,
            List<String> urls,
            Boolean unique
    ) {
        validateRange(start, end);

        URI uri = makeUri(buildStatsPath(start, end, urls, unique));

        return restClient.get()
                .uri(uri)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    // ================== Discovery ==================

    private URI makeUri(String path) {
        ServiceInstance instance = retryTemplate.execute(
                context -> getInstance()
        );

        return URI.create(
                "http://" + instance.getHost()
                        + ":" + instance.getPort()
                        + path
        );
    }

    private ServiceInstance getInstance() {
        List<ServiceInstance> instances = discoveryClient.getInstances(STATS_SERVICE_ID);
        if (instances == null || instances.isEmpty()) {
            throw new IllegalStateException("Stats server not found in Eureka");
        }
        return instances.getFirst();
    }

    // ================== Retry ==================

    private RetryTemplate createRetryTemplate() {
        RetryTemplate template = new RetryTemplate();

        FixedBackOffPolicy backOff = new FixedBackOffPolicy();
        backOff.setBackOffPeriod(3000);

        MaxAttemptsRetryPolicy retryPolicy = new MaxAttemptsRetryPolicy();
        retryPolicy.setMaxAttempts(3);

        template.setBackOffPolicy(backOff);
        template.setRetryPolicy(retryPolicy);

        return template;
    }

    // ================== Utils ==================

    private void validateRange(String start, String end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Диапазон не может быть null");
        }

        LocalDateTime s = LocalDateTime.parse(start, formatter);
        LocalDateTime e = LocalDateTime.parse(end, formatter);

        if (s.isAfter(e)) {
            throw new IllegalArgumentException("Неверный диапазон дат");
        }
    }

    private String buildStatsPath(
            String start,
            String end,
            List<String> urls,
            Boolean unique
    ) {
        StringBuilder sb = new StringBuilder("/stats?")
                .append("start=").append(start)
                .append("&end=").append(end);

        if (urls != null) {
            urls.forEach(u -> sb.append("&uris=").append(u));
        }
        if (unique != null) {
            sb.append("&unique=").append(unique);
        }

        return sb.toString();
    }
}
