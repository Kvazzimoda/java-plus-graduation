package ru.practicum.main.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.client.request.RequestClient;
import ru.practicum.main.client.user.UserClient;
import ru.practicum.main.dto.mappers.EventMapper;
import ru.practicum.main.dto.mappers.LocationMapper;
import ru.practicum.main.dto.mappers.RequestMapper;
import ru.practicum.main.dto.request.event.*;
import ru.practicum.main.dto.response.event.EventFullDto;
import ru.practicum.main.dto.response.event.EventRequestStatusUpdateResult;
import ru.practicum.main.dto.response.event.EventShortDto;
import ru.practicum.main.dto.response.request.ParticipationRequestDto;
import ru.practicum.main.dto.response.request.RequestDto;
import ru.practicum.main.dto.response.request.RequestStatusUpdateDto;
import ru.practicum.main.dto.response.user.UserDto;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.exception.ValidationException;
import ru.practicum.main.model.*;
import ru.practicum.main.repository.*;
import ru.practicum.main.service.interfaces.EventPrivateService;
import ru.practicum.stats.client.StatClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
public class EventPrivateServiceImpl extends AbstractEventService implements EventPrivateService {

    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final LocationRepository locationRepository;

    public EventPrivateServiceImpl(RequestClient requestClient,
                                   StatClient statClient,
                                   EventRepository eventRepository,
                                   UserClient userClient,
                                   CategoryRepository categoryRepository,
                                   LocationRepository locationRepository) {
        super(requestClient, statClient, userClient);
        this.eventRepository = eventRepository;
        this.categoryRepository = categoryRepository;
        this.locationRepository = locationRepository;
    }

    @Override
    public List<EventShortDto> getEvents(Long userId, Pageable pageable) {
        UserDto userDto = validateAndGetUser(userId);
        log.debug("Получаем события пользователя {} с пагинацией: {}", userId, pageable);
        Page<Event> eventsPage = eventRepository.findByInitiatorIdOrderByCreatedOnDesc(userId, pageable);
        if (eventsPage.isEmpty()) {
            log.debug("События для пользователя {} не найдены", userId);
            return Collections.emptyList();
        }
        List<Event> events = eventsPage.getContent();
        Map<Long, Long> views = getEventsViews(events);
        return events.stream()
                .map(event -> {
                    EventShortDto dto = EventMapper.toEventShortDto(event, userDto);
                    dto.setViews(views.getOrDefault(event.getId(), 0L));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto addEvent(Long userId, NewEventDto newEventDto) {
        UserDto userDto = validateAndGetUser(userId);
        Category category = categoryRepository.findById(newEventDto.getCategory())
                .orElseThrow(() -> new NotFoundException(
                        "Категория c id " + newEventDto.getCategory() + " не найдена"));
        validateEventDate(newEventDto.getEventDate());
        LocationEntity locationEntity = LocationMapper.toLocation(newEventDto.getLocation());
        LocationEntity savedLocationEntity = locationRepository.save(locationEntity);
        Event event = EventMapper.toEventFromNewEventDto(newEventDto, userId, category, savedLocationEntity);
        event.setConfirmedRequests(0);
        Event savedEvent = eventRepository.save(event);
        log.info("Событие создано успешно: ID {}", savedEvent.getId());
        EventFullDto result = EventMapper.toEventFullDto(savedEvent, userDto);
        result.setViews(0L);
        return result;
    }

    @Override
    public EventFullDto getEvent(Long eventId, Long userId) {
        UserDto userDto = validateAndGetUser(userId);
        Event event = validateEventOfInitiator(eventId, userId);
        Integer confirmedRequests = getConfirmedRequestsCount(eventId);
        event.setConfirmedRequests(confirmedRequests);
        Long views = getEventViews(eventId);
        EventFullDto result = EventMapper.toEventFullDto(event, userDto);
        result.setViews(views);
        log.debug("Событие {} пользователя {} найдено", eventId, userId);
        return result;
    }

    @Override
    @Transactional
    public EventFullDto updateEvent(UserIdAndEventIdDto userIdAndEventIdDto, UpdateEventUserRequest updateEventUserRequest) {
        Long userId = userIdAndEventIdDto.getUserId();
        Long eventId = userIdAndEventIdDto.getEventId();
        log.debug("Обновление события {} пользователя {}: {}", eventId, userId, updateEventUserRequest);

        UserDto userDto = validateAndGetUser(userId);

        Event event = validateEventOfInitiator(eventId, userId);
        validateEventCanBeUpdated(event);
        updateEventFields(event, updateEventUserRequest);
        if (updateEventUserRequest.getEventDate() != null) {
            validateEventDate(updateEventUserRequest.getEventDate());
            event.setEventDate(updateEventUserRequest.getEventDate());
        }
        if (updateEventUserRequest.getStateAction() != null) {
            processStateAction(event, updateEventUserRequest.getStateAction());
        }
        Integer confirmedRequests = getConfirmedRequestsCount(eventId);
        event.setConfirmedRequests(confirmedRequests);
        Event updatedEvent = eventRepository.save(event);
        Long views = getEventViews(eventId);
        EventFullDto result = EventMapper.toEventFullDto(updatedEvent, userDto);
        result.setViews(views);
        log.info("Событие {} пользователя {} успешно обновлено", eventId, userId);
        return result;
    }

    private void updateEventFields(Event event, UpdateEventUserRequest updateRequest) {
        if (updateRequest.getAnnotation() != null) {
            event.setAnnotation(updateRequest.getAnnotation());
        }
        if (updateRequest.getCategory() != null) {
            Category category = categoryRepository.findById(updateRequest.getCategory())
                    .orElseThrow(() -> new NotFoundException(
                            String.format("Категория с id=%d не найдена", updateRequest.getCategory())));
            event.setCategory(category);
        }
        if (updateRequest.getDescription() != null) {
            event.setDescription(updateRequest.getDescription());
        }
        if (updateRequest.getLocation() != null) {
            LocationEntity locationEntity = LocationMapper.toLocation(updateRequest.getLocation());
            LocationEntity savedLocation = locationRepository.save(locationEntity);
            event.setLocationEntity(savedLocation);
        }
        if (updateRequest.getPaid() != null) {
            event.setPaid(updateRequest.getPaid());
        }
        if (updateRequest.getParticipantLimit() != null) {
            event.setParticipantLimit(updateRequest.getParticipantLimit());
        }
        if (updateRequest.getRequestModeration() != null) {
            event.setRequestModeration(updateRequest.getRequestModeration());
        }
        if (updateRequest.getTitle() != null) {
            event.setTitle(updateRequest.getTitle());
        }
    }

    private void processStateAction(Event event, StateAction stateAction) {
        switch (stateAction) {
            case SEND_TO_REVIEW:
                event.setState(Event.EventState.PENDING);
                break;
            case CANCEL_REVIEW:
                event.setState(Event.EventState.CANCELED);
                break;
            default:
                throw new ValidationException("Неверное действие: " + stateAction);
        }
    }

    @Override
    public List<ParticipationRequestDto> getRequests(Long userId, Long eventId) {
        log.debug("Получение запросов на участие в событии {} пользователя {}", eventId, userId);
        UserDto userDto = validateAndGetUser(userId);
        Event event = validateEventOfInitiator(eventId, userId);
        List<RequestDto> requests;
        try {
            requests = requestClient.getRequestsByEventId(eventId);
        } catch (Exception e) {
            log.warn("Не удалось получить запросы для события {}: {}", eventId, e.getMessage());
            return Collections.emptyList();
        }
        if (requests.isEmpty()) {
            log.debug("Запросы на участие в событии {} не найдены", eventId);
            return Collections.emptyList();
        }
        return requests.stream()
                .map(RequestMapper::toParticipationRequestDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateRequests(UserIdAndEventIdDto userIdAndEventIdDto, EventRequestStatusUpdateRequest updateRequest) {
        Long userId = userIdAndEventIdDto.getUserId();
        Long eventId = userIdAndEventIdDto.getEventId();
        log.debug("Обработка изменения статуса заявок для события {} пользователя {}: {}",
                eventId, userId, updateRequest);
        UserDto userDto = validateAndGetUser(userId);
        Event event = validateEventOfInitiator(eventId, userId);
        if (!isModerationRequired(event)) {
            throw new ConflictException("Подтверждение заявок не требуется для этого события");
        }
        List<RequestDto> requestsToProcess = getRequestsToProcess(updateRequest.getRequestIds().stream().toList(), eventId);
        validateRequestsCanBeProcessed(requestsToProcess, event, updateRequest.getStatus());
        EventRequestStatusUpdateResult result = processRequests(requestsToProcess, event, updateRequest.getStatus());
        log.info("Статусы заявок для события {} обновлены: подтверждено {}, отклонено {}",
                eventId, result.getConfirmedRequests().size(), result.getRejectedRequests().size());
        return result;
    }

    private void validateEventDate(LocalDateTime eventDate) {
        LocalDateTime minAllowedDate = LocalDateTime.now().plusHours(2);
        if (eventDate.isBefore(minAllowedDate)) {
            throw new ValidationException(
                    String.format("Дата события должна быть не раньше чем через 2 часа. " +
                                    "Текущее время: %s, указанное время: %s",
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                            eventDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
            );
        }
    }

    private UserDto validateAndGetUser(Long userId) {
        //Проверим пользователя через клиент
        try {
            UserDto user = userClient.getUserById(userId);
            log.debug("Existing User received from user-service: {}", user);
            return user;
        } catch (Exception e) {
            log.debug("Failed to get user from user-service: {}", e.getMessage());
            throw new NotFoundException("Пользователь c userId " + userId + " не найден");
        }
    }

    private Event validateEventOfInitiator(Long eventId, Long userId) {
        log.debug("Проверяем, что событие {} создано пользователем с userId {}", eventId, userId);
        return eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Событие с id=%d не найдено для пользователя с id=%d", eventId, userId)));
    }

    private void validateEventCanBeUpdated(Event event) {
        // Можно редактировать только отмененные события или события в состоянии ожидания модерации
        if (event.getState() != Event.EventState.PENDING && event.getState() != Event.EventState.CANCELED) {
            throw new ConflictException("Изменить можно только отмененные события или события в состоянии ожидания модерации");
        }
    }

    private boolean isModerationRequired(Event event) {
        // Модерация не требуется если:
        // 1. Лимит участников = 0 (безлимитно)
        // 2. Пре-модерация отключена
        return event.getParticipantLimit() != 0 && event.getRequestModeration();
    }

    private List<RequestDto> getRequestsToProcess(List<Long> requestIds, Long eventId) {
        List<RequestDto> requests;
        try {
            requests = requestClient.findAllByIdInAndEventId(requestIds, eventId);
        } catch (Exception e) {
            log.warn("Не удалось получить запросы по IDs {} для события {}: {}",
                    requestIds, eventId, e.getMessage());
            throw new NotFoundException("Не удалось получить запросы");
        }
        if (requests.size() != requestIds.size()) {
            throw new NotFoundException("Некоторые запросы не найдены или не принадлежат событию");
        }
        return requests;
    }

    private void validateRequestsCanBeProcessed(List<RequestDto> requests, Event event, RequestDto.RequestStatusDto newStatus) {
        // Проверяем что все запросы в состоянии PENDING
        requests.forEach(request -> {
            if (request.getStatus() != RequestDto.RequestStatusDto.PENDING) {
                throw new ConflictException(
                        String.format("Запрос %d уже обработан (статус: %s)",
                                request.getId(), request.getStatus()));
            }
        });

        // Проверяем лимит участников для подтверждения
        if (newStatus == RequestDto.RequestStatusDto.CONFIRMED) {
            int confirmedCount = getConfirmedRequestsCount(event.getId());
            int availableSlots = event.getParticipantLimit() - confirmedCount;

            if (availableSlots <= 0) {
                throw new ConflictException("Лимит участников для события исчерпан");
            }

            if (requests.size() > availableSlots) {
                throw new ConflictException(
                        String.format("Недостаточно свободных мест: доступно %d, запрошено %d",
                                availableSlots, requests.size()));
            }
        }
    }

    private EventRequestStatusUpdateResult processRequests(List<RequestDto> requests, Event event,
                                                           RequestDto.RequestStatusDto newStatus) {
        EventRequestStatusUpdateResult result = new EventRequestStatusUpdateResult();

        if (newStatus == RequestDto.RequestStatusDto.CONFIRMED) {
            processConfirmation(requests, event, result);
        } else if (newStatus == RequestDto.RequestStatusDto.REJECTED) {
            processRejection(requests, event, result);
        }

        return result;
    }

    private void processConfirmation(List<RequestDto> requests, Event event,
                                     EventRequestStatusUpdateResult result) {
        int confirmedCount = getConfirmedRequestsCount(event.getId());
        int availableSlots = event.getParticipantLimit() - confirmedCount;

        // Подтверждаем сколько можем
        List<RequestDto> toConfirm = requests.stream()
                .limit(availableSlots)
                .toList();

        // Отклоняем остальные (если лимит исчерпан)
        List<RequestDto> toReject = requests.stream()
                .skip(availableSlots)
                .toList();

        // Получаем ID всех запросов для обновления
        List<Long> allRequestIds = requests.stream()
                .map(RequestDto::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // Обновляем статусы
        if (!toConfirm.isEmpty()) {
            List<Long> confirmIds = toConfirm.stream()
                    .map(RequestDto::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            updateRequestsStatusInternal(confirmIds, RequestDto.RequestStatusDto.CONFIRMED);
        }

        if (!toReject.isEmpty()) {
            List<Long> rejectIds = toReject.stream()
                    .map(RequestDto::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            updateRequestsStatusInternal(rejectIds, RequestDto.RequestStatusDto.REJECTED);
        }

        updateEventConfirmedRequests(event);

        // ЗАНОВО ПОЛУЧАЕМ ОБНОВЛЕННЫЕ ЗАПРОСЫ ИЗ REQUEST-SERVICE
        List<RequestDto> updatedRequests = getUpdatedRequests(allRequestIds, event.getId());

        // Разделяем обновленные запросы по статусам
        List<RequestDto> updatedConfirmed = updatedRequests.stream()
                .filter(req -> RequestDto.RequestStatusDto.CONFIRMED == req.getStatus())
                .collect(Collectors.toList());

        List<RequestDto> updatedRejected = updatedRequests.stream()
                .filter(req -> RequestDto.RequestStatusDto.REJECTED == req.getStatus())
                .collect(Collectors.toList());

        // Заполняем результат ОБНОВЛЕННЫМИ данными
        result.setConfirmedRequests(updatedConfirmed.stream()
                .map(RequestMapper::toParticipationRequestDto)
                .collect(Collectors.toList()));

        result.setRejectedRequests(updatedRejected.stream()
                .map(RequestMapper::toParticipationRequestDto)
                .collect(Collectors.toList()));

        log.debug("После подтверждения: CONFIRMED={}, REJECTED={}",
                updatedConfirmed.size(), updatedRejected.size());
    }

    private void processRejection(List<RequestDto> requests, Event event, EventRequestStatusUpdateResult result) {

        // Получаем ID запросов
        List<Long> requestIds = requests.stream()
                .map(RequestDto::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // Обновляем статусы
        updateRequestsStatusInternal(requestIds, RequestDto.RequestStatusDto.REJECTED);

        updateEventConfirmedRequests(event);

        // ЗАНОВО ПОЛУЧАЕМ ОБНОВЛЕННЫЕ ЗАПРОСЫ ИЗ REQUEST-SERVICE
        List<RequestDto> updatedRequests = getUpdatedRequests(requestIds, event.getId());

        result.setRejectedRequests(updatedRequests.stream()
                .map(RequestMapper::toParticipationRequestDto)
                .collect(Collectors.toList()));

        result.setConfirmedRequests(Collections.emptyList());

        log.debug("После отклонения: REJECTED={}", updatedRequests.size());
    }

    private void updateRequestsStatusInternal(List<Long> requestIds, RequestDto.RequestStatusDto status) {
        try {
            if (requestIds == null || requestIds.isEmpty()) {
                log.debug("Нет ID запросов для обновления");
                return;
            }

            RequestStatusUpdateDto updateDto = RequestStatusUpdateDto.builder()
                    .requestIds(requestIds)
                    .status(status.name())
                    .build();

            requestClient.updateRequestsStatus(updateDto);
            log.debug("Обновление статусов {} запросов на {}", requestIds.size(), status);
        } catch (Exception e) {
            log.warn("Не удалось обновить статусы запросов: {}", e.getMessage());
            throw new ConflictException("Не удалось обновить статусы запросов");
        }
    }

    private void updateEventConfirmedRequests(Event event) {
        // Получаем актуальное количество подтвержденных запросов
        int confirmedCount = getConfirmedRequestsCount(event.getId());
        event.setConfirmedRequests(confirmedCount);
        eventRepository.save(event);
        log.debug("Обновлено confirmedRequests для события {}: {}", event.getId(), confirmedCount);
    }

    private List<RequestDto> getUpdatedRequests(List<Long> requestIds, Long eventId) {
        try {
            // Запрашиваем обновленные запросы из request-service
            return requestClient.findAllByIdInAndEventId(requestIds, eventId);
        } catch (Exception e) {
            log.warn("Не удалось получить обновленные запросы: {}", e.getMessage());
            // Если не удалось получить обновленные, возвращаем пустой список
            return Collections.emptyList();
        }
    }
}
