package ru.practicum.server.service;

import ru.practicum.stats.dto.dto.EndpointHitDto;
import ru.practicum.stats.dto.dto.ViewStatsDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.server.dao.EndpointHitRepository;
import ru.practicum.server.model.EndpointHit;

import java.time.LocalDateTime;
import java.util.List;

import static ru.practicum.server.mapper.EndpointHitMapper.toDto;
import static ru.practicum.server.mapper.EndpointHitMapper.toEntity;


@Service
@Slf4j
@RequiredArgsConstructor
public class EndpointHitServiceImpl implements EndpointHitService {

    private final EndpointHitRepository endpointHitRepository;


    @Override
    @Transactional
    public EndpointHitDto save(final EndpointHitDto newEndpointHit) {
        log.debug("сохранение информации о запросе {}", newEndpointHit);
        final EndpointHit endpointHit = toEntity(newEndpointHit);
        return toDto(endpointHitRepository.save(endpointHit));
    }

    @Override
    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("начало не должно быть позже конца");
        }

        if (unique) {
            log.debug("поиск уникальных запросов");
            if (uris == null || uris.isEmpty()) {
                return endpointHitRepository.findAllUniqueStats(start, end);
            } else {
                return endpointHitRepository.findUniqueStatsByUris(uris, start, end);
            }
        } else {
            log.debug("поиск всех запросов");
            if (uris == null) {
                return endpointHitRepository.findAllStats(start, end);
            } else {
                return endpointHitRepository.findAllStatsByUris(uris, start, end);
            }
        }
    }

}
