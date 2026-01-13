package ru.practicum.server.mapper;

import ru.practicum.stats.dto.dto.EndpointHitDto;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.practicum.server.model.EndpointHit;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EndpointHitMapper {

    public static EndpointHit toEntity(EndpointHitDto newEndpointHit) {
        return new EndpointHit(0, newEndpointHit.getApp(), newEndpointHit.getUri(), newEndpointHit.getIp(), newEndpointHit.getTimestamp());

    }

    public static EndpointHitDto toDto(EndpointHit endpointHit) {
        return EndpointHitDto
                .builder()
                .app(endpointHit.getApp())
                .uri(endpointHit.getUri())
                .ip(endpointHit.getIp())
                .timestamp(endpointHit.getTimestamp())
                .build();
    }
}
