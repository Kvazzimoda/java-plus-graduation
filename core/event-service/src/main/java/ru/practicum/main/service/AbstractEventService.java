package ru.practicum.main.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import ru.practicum.main.client.request.RequestClient;
import ru.practicum.main.client.user.UserClient;
import ru.practicum.main.dto.response.request.ConfirmedRequestsCountDto;
import ru.practicum.main.dto.response.user.UserDto;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.model.Event;
import ru.practicum.stats.client.StatClient;
import ru.practicum.stats.dto.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractEventService {

    protected final RequestClient requestClient;
    protected final StatClient statClient;
    protected final UserClient userClient;

    protected Map<Long, Long> getEventsViews(List<Event> events) {
        if (events.isEmpty()) {
            return Collections.emptyMap();
        }

        List<String> uris = events.stream()
                .map(event -> "/events/" + event.getId())
                .collect(Collectors.toList());

        LocalDateTime start = LocalDateTime.of(2000, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.now();

        try {
            ResponseEntity<List<ViewStatsDto>> response = statClient.getStats(start, end, uris, true);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody().stream()
                        .collect(Collectors.toMap(
                                stats -> extractEventIdFromUri(stats.getUri()),
                                ViewStatsDto::getHits
                        ));
            }
        } catch (Exception e) {
            log.warn("Ошибка при получении статистики просмотров: {}", e.getMessage());
        }

        return events.stream()
                .collect(Collectors.toMap(Event::getId, event -> 0L));
    }

    protected Long extractEventIdFromUri(String uri) {
        try {
            return Long.parseLong(uri.replace("/events/", ""));
        } catch (NumberFormatException e) {
            log.warn("Не удалось извлечь ID события из URI: {}", uri);
            return 0L;
        }
    }

    protected Long getEventViews(Long eventId) {
        try {
            LocalDateTime start = LocalDateTime.of(2000, 1, 1, 0, 0);
            LocalDateTime end = LocalDateTime.now();
            List<String> uris = List.of("/events/" + eventId);

            ResponseEntity<List<ViewStatsDto>> response = statClient.getStats(start, end, uris, true);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody().stream()
                        .findFirst()
                        .map(ViewStatsDto::getHits)
                        .orElse(0L);
            }
        } catch (Exception e) {
            log.warn("Ошибка при получении статистики просмотров для события {}: {}", eventId, e.getMessage());
        }
        return 0L;
    }

    protected Map<Long, Integer> getConfirmedRequests(List<Event> events) {
        if (events.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .toList();
        try {
        List<ConfirmedRequestsCountDto> results = requestClient.countConfirmedRequestsByEventIds(eventIds);
        Map<Long, Long> confirmedRequestsMap = results.stream()
                .collect(Collectors.toMap(
                        ConfirmedRequestsCountDto::getEventId,
                        ConfirmedRequestsCountDto::getCount
                ));
        return eventIds.stream()
                .collect(Collectors.toMap(
                        eventId -> eventId,
                        eventId -> confirmedRequestsMap.getOrDefault(eventId, 0L).intValue()
                ));
        } catch (Exception e) {
            log.warn("Не удалось получить количество подтвержденных запросов для событий {}: {}",
                    eventIds, e.getMessage());
            // Возвращаем 0 для всех событий при ошибке
            return eventIds.stream()
                    .collect(Collectors.toMap(
                            eventId -> eventId,
                            eventId -> 0
                    ));
        }
    }

    protected Integer getConfirmedRequestsCount(Long eventId) {
        try {
            return requestClient.countConfirmedRequestsByEventId(eventId);
        } catch (Exception e) {
            log.warn("Не удалось получить количество подтвержденных запросов для события {}: {}",
                    eventId, e.getMessage());
            return 0; // Возвращаем 0 при ошибке
        }
    }

    protected Map<Long, UserDto> getUsersByIds(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return new HashMap<>();
        }

        try {
            List<UserDto> users = userClient.getUsers(userIds);
            return users.stream()
                    .collect(Collectors.toMap(UserDto::getId, Function.identity()));
        } catch (Exception e) {
            log.error("Failed to get users from user-service: {}", e.getMessage());
            // Возвращаем пустую мапу, чтобы не падать полностью
            return new HashMap<>();
        }
    }

    protected UserDto getUserById(Long userId) {
        try {
            return userClient.getUserById(userId);
        } catch (Exception e) {
            log.warn("Не удалось получить пользователя с ID {}: {}", userId, e.getMessage());
            throw new NotFoundException("Пользователь c userId " + userId + " не найден");
        }
    }
}
