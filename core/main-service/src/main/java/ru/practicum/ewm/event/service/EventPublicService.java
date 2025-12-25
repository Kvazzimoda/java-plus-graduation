package ru.practicum.ewm.event.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.client.StatsClient;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStats;
import ru.practicum.ewm.comment.repository.CommentRepository;
import ru.practicum.ewm.event.dto.EventFullDto;
import ru.practicum.ewm.event.dto.EventSearchParams;
import ru.practicum.ewm.event.dto.EventShortDto;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventSort;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.exception.ValidationException;
import ru.practicum.ewm.mapper.EwmMapper;
import ru.practicum.ewm.request.repository.ParticipationRequestRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class EventPublicService {

    private final EventRepository eventRepository;
    private final ParticipationRequestRepository requestRepository;
    private final CommentRepository commentRepository;
    private final StatsClient statsClient;
    private final EwmMapper mapper;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public List<EventShortDto> getEvents(EventSearchParams params, String ip, String uri) {
        log.info("Поиск событий с параметрами: {}", params);

        // Валидация дат
        if (params.getRangeStart() != null && params.getRangeEnd() != null) {
            if (params.getRangeStart().isAfter(params.getRangeEnd())) {
                throw new ValidationException("Дата начала не может быть позже даты окончания");
            }
        }

        // Установка дефолтных значений для дат
        LocalDateTime rangeStart = params.getRangeStart();
        LocalDateTime rangeEnd = params.getRangeEnd();

        if (rangeStart == null) {
            rangeStart = LocalDateTime.now();
        }
        if (rangeEnd == null) {
            rangeEnd = LocalDateTime.now().plusYears(100);
        }

        // Создание Pageable с сортировкой
        Pageable pageable;
        if (EventSort.EVENT_DATE.name().equals(params.getSort())) {
            pageable = PageRequest.of(params.getFrom() / params.getSize(), params.getSize(),
                    Sort.by("eventDate").ascending());
        } else {
            pageable = PageRequest.of(params.getFrom() / params.getSize(), params.getSize());
        }

        // Поиск событий
        List<Event> events = eventRepository.findPublishedEvents(
                params.getText(),
                params.getCategories(),
                params.getPaid(),
                rangeStart,
                rangeEnd,
                pageable
        ).getContent();

        // Фильтрация по доступности мест (до преобразования в DTO)
        if (Boolean.TRUE.equals(params.getOnlyAvailable())) {
            Map<Long, Long> confirmedRequestsMap = getConfirmedRequestsMap(
                    events.stream().map(Event::getId).collect(Collectors.toList())
            );

            events = events.stream()
                    .filter(event -> event.getParticipantLimit() == 0 ||
                            confirmedRequestsMap.getOrDefault(event.getId(), 0L) < event.getParticipantLimit())
                    .collect(Collectors.toList());
        }

        // Получение статистики просмотров
        List<EventShortDto> eventDtos = convertToEventShortDtoList(events);

        // Сортировка по просмотрам (если требуется)
        if (EventSort.VIEWS.name().equals(params.getSort())) {
            eventDtos.sort(Comparator.comparing(EventShortDto::getViews,
                    Comparator.nullsLast(Comparator.naturalOrder())));
        }

        saveStats(ip, uri);

        log.info("Найдено {} событий", eventDtos.size());
        return eventDtos;
    }

    public EventFullDto getEventById(Long eventId, String ip, String uri) {
        log.info("Получение события по ID: {}", eventId);

        Event event = eventRepository.findByIdAndState(eventId, EventState.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Событие с ID " + eventId + " не найдено"));

        EventFullDto eventDto = convertToEventFullDto(event);

        saveStats(ip, uri);

        log.info("Событие с ID {} найдено", eventId);
        return eventDto;
    }

    private List<EventShortDto> convertToEventShortDtoList(List<Event> events) {
        if (events.isEmpty()) {
            return List.of();
        }

        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .collect(Collectors.toList());

        // Получение статистики просмотров
        Map<Long, Long> viewsMap = getViewsMap(eventIds);

        // Получение количества подтвержденных заявок
        Map<Long, Long> confirmedRequestsMap = getConfirmedRequestsMap(eventIds);

        // Получение количества комментариев
        Map<Long, Long> commentsCountMap = getCommentsCountMap(eventIds);

        return events.stream()
                .map(event -> {
                    EventShortDto dto = mapper.toEventShortDto(event);
                    dto.setViews(viewsMap.getOrDefault(event.getId(), 0L));
                    dto.setConfirmedRequests(confirmedRequestsMap.getOrDefault(event.getId(), 0L));
                    dto.setCommentsCount(commentsCountMap.getOrDefault(event.getId(), 0L));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    private EventFullDto convertToEventFullDto(Event event) {
        EventFullDto dto = mapper.toEventFullDto(event);

        // Получение статистики просмотров для одного события
        Map<Long, Long> viewsMap = getViewsMap(List.of(event.getId()));
        dto.setViews(viewsMap.getOrDefault(event.getId(), 0L));

        // Получение количества подтвержденных заявок
        Map<Long, Long> confirmedRequestsMap = getConfirmedRequestsMap(List.of(event.getId()));
        dto.setConfirmedRequests(confirmedRequestsMap.getOrDefault(event.getId(), 0L));

        // Получение количества комментариев
        Map<Long, Long> commentsCountMap = getCommentsCountMap(List.of(event.getId()));
        dto.setCommentsCount(commentsCountMap.getOrDefault(event.getId(), 0L));

        return dto;
    }

    private Map<Long, Long> getViewsMap(List<Long> eventIds) {
        try {
            // Формирование списка URI для запроса статистики
            List<String> uris = eventIds.stream()
                    .map(id -> "/events/" + id)
                    .collect(Collectors.toList());

            // Запрос статистики с начала времени до текущего момента
            LocalDateTime start = LocalDateTime.of(1970, 1, 1, 0, 0, 0);
            LocalDateTime end = LocalDateTime.now();

            Collection<ViewStats> stats = statsClient.getStat(
                    start.format(FORMATTER),
                    end.format(FORMATTER),
                    uris,
                    true
            );

            return stats.stream()
                    .collect(Collectors.toMap(
                            stat -> extractEventIdFromUri(stat.getUri()),
                            ViewStats::getHits,
                            (existing, replacement) -> existing
                    ));
        } catch (Exception e) {
            log.warn("Ошибка при получении статистики просмотров: {}", e.getMessage());
            return Map.of();
        }
    }

    private Map<Long, Long> getConfirmedRequestsMap(List<Long> eventIds) {
        if (eventIds.isEmpty()) {
            return Map.of();
        }

        return requestRepository.countConfirmedRequestsByEventIds(eventIds)
                .stream()
                .collect(Collectors.toMap(
                        result -> (Long) result[0],
                        result -> (Long) result[1]
                ));
    }

    private Map<Long, Long> getCommentsCountMap(List<Long> eventIds) {
        if (eventIds.isEmpty()) {
            return Map.of();
        }

        return commentRepository.countCommentsByEventIds(eventIds)
                .stream()
                .collect(Collectors.toMap(
                        result -> (Long) result[0],
                        result -> (Long) result[1]
                ));
    }

    private Long extractEventIdFromUri(String uri) {
        try {
            String[] parts = uri.split("/");
            return Long.parseLong(parts[parts.length - 1]);
        } catch (Exception e) {
            log.warn("Ошибка при извлечении ID события из URI: {}", uri);
            return 0L;
        }
    }

    private void saveStats(String ip, String uri) {
        try {
            EndpointHitDto hitDto = new EndpointHitDto(
                    "ewm-main-service",
                    uri,
                    ip,
                    LocalDateTime.now()
            );

            statsClient.hit(hitDto);
            log.debug("Статистика сохранена: ip={}, uri={}", ip, uri);
        } catch (Exception e) {
            log.warn("Ошибка при сохранении статистики: {}", e.getMessage());
        }
    }
}
