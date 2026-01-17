package ru.practicum.main.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.practicum.core.client.UserClient;
import ru.practicum.core.dto.UserDto;
import ru.practicum.core.client.RequestClient;
import ru.practicum.core.dto.ConfirmedRequestsCountDto;

import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.model.Event;
import ru.practicum.stats.client.CollectorClient;
import ru.practicum.stats.client.RecommendationsClient;
import ru.practicum.stats.proto.InteractionsCountRequestProto;
import ru.practicum.stats.proto.RecommendedEventProto;

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
    protected final CollectorClient collectorClient;
    protected final RecommendationsClient recommendationsClient;
    protected final UserClient userClient;

    protected Map<Long, Double> getEventsRatings(List<Event> events) {
        if (events.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .toList();
        try {
            InteractionsCountRequestProto.Builder requestBuilder = InteractionsCountRequestProto.newBuilder();
            eventIds.forEach(requestBuilder::addEventId);
            List<RecommendedEventProto> result = recommendationsClient.getInteractionsCount(requestBuilder.build());
            Map<Long, Double> ratings = result.stream()
                    .collect(Collectors.toMap(
                            RecommendedEventProto::getEventId,
                            RecommendedEventProto::getScore
                    ));
            // Заполняем нулями отсутствующие события
            for (Long eventId : eventIds) {
                ratings.putIfAbsent(eventId, 0.0);
            }
            log.debug("Получены рейтинги для {} событий: {}", eventIds.size(), ratings);
            return ratings;
        } catch (Exception e) {
            log.warn("Ошибка при получении рейтингов через gRPC для событий {}: {}",eventIds, e.getMessage());
            // Возвращаем нулевые рейтинги при ошибке
            return eventIds.stream()
                    .collect(Collectors.toMap(
                            Function.identity(),
                            id -> 0.0
                    ));
        }
    }

    protected Double getEventRating(Long eventId) {
        try {
            InteractionsCountRequestProto request = InteractionsCountRequestProto.newBuilder()
                    .addEventId(eventId)
                    .build();

            List<RecommendedEventProto> result = recommendationsClient.getInteractionsCount(request);
            if (result.isEmpty()) {
                return 0.0;
            }
            return result.getFirst().getScore();

        } catch (Exception e) {
            log.warn("Ошибка при получении рейтинга для события {}: {}", eventId, e.getMessage());
            return 0.0;
        }
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