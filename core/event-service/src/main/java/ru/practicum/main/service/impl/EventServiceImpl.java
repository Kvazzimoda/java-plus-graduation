package ru.practicum.main.service.impl;

import com.google.protobuf.Timestamp;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import ru.practicum.core.client.UserClient;
import ru.practicum.core.dto.UserDto;
import ru.practicum.core.client.RequestClient;
import ru.practicum.main.dto.mappers.EventMapper;
import ru.practicum.main.dto.mappers.LocationMapper;
import ru.practicum.main.dto.mappers.RequestMapper;
import ru.practicum.main.dto.request.event.*;
import ru.practicum.main.dto.response.event.EventFullDto;
import ru.practicum.main.dto.response.event.EventRequestStatusUpdateResult;
import ru.practicum.main.dto.response.event.EventShortDto;
import ru.practicum.main.dto.response.request.ParticipationRequestDto;
import ru.practicum.core.dto.RequestDto;
import ru.practicum.core.dto.RequestStatusUpdateDto;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.exception.ValidationException;
import ru.practicum.main.model.*;
import ru.practicum.main.repository.*;
import ru.practicum.main.service.EventService;
import ru.practicum.stats.client.CollectorClient;
import ru.practicum.stats.client.RecommendationsClient;
import ru.practicum.stats.proto.ActionTypeProto;
import ru.practicum.stats.proto.RecommendedEventProto;
import ru.practicum.stats.proto.UserActionProto;
import ru.practicum.stats.proto.UserPredictionsRequestProto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
public class EventServiceImpl extends AbstractEventService implements EventService {

    private static final int MAX_RESULTS = 10;

    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final LocationRepository locationRepository;

    public EventServiceImpl(RequestClient requestClient,
                            CollectorClient collectorClient,
                            RecommendationsClient recommendationsClient,
                            EventRepository eventRepository,
                            UserClient userClient,
                            CategoryRepository categoryRepository,
                            LocationRepository locationRepository) {
        super(requestClient, collectorClient, recommendationsClient, userClient);
        this.eventRepository = eventRepository;
        this.categoryRepository = categoryRepository;
        this.locationRepository = locationRepository;
    }

    @Override
    public List<EventShortDto> getUserEvents(Long userId, Pageable pageable) {
        UserDto userDto = validateAndGetUser(userId);
        log.debug("Получаем события пользователя {} с пагинацией: {}", userId, pageable);
        Page<Event> eventsPage = eventRepository.findByInitiatorIdOrderByCreatedOnDesc(userId, pageable);
        if (eventsPage.isEmpty()) {
            log.debug("События для пользователя {} не найдены", userId);
            return Collections.emptyList();
        }
        List<Event> events = eventsPage.getContent();
        Map<Long, Double> ratings = getEventsRatings(events);
        return events.stream()
                .map(event -> {
                    EventShortDto dto = EventMapper.toEventShortDto(event, userDto);
                    dto.setRating(ratings.get(event.getId()));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto addEventByUser(Long userId, NewEventDto newEventDto) {
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
        result.setRating(0.0);
        return result;
    }

    @Override
    public EventFullDto getUserEvent(Long eventId, Long userId) {
        UserDto userDto = validateAndGetUser(userId);
        Event event = validateEventOfInitiator(eventId, userId);
        Integer confirmedRequests = getConfirmedRequestsCount(eventId);
        event.setConfirmedRequests(confirmedRequests);
        EventFullDto result = EventMapper.toEventFullDto(event, userDto);
        result.setRating(getEventRating(eventId));
        log.debug("Событие {} пользователя {} найдено", eventId, userId);
        return result;
    }

    @Override
    @Transactional
    public EventFullDto updateEventByUser(UserIdAndEventIdDto userIdAndEventIdDto, UpdateEventUserRequest updateEventUserRequest) {
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
        EventFullDto result = EventMapper.toEventFullDto(updatedEvent, userDto);
        result.setRating(getEventRating(eventId));
        log.info("Событие {} пользователя {} успешно обновлено", eventId, userId);
        return result;
    }

    @Override
    public List<EventShortDto> getPublicEvents(SearchOfEventByPublicDto searchDto, Pageable pageable, HttpServletRequest request) {
        log.debug("Публичный поиск событий по критериям: {}", searchDto);
        if (searchDto.getRangeStart() != null
                && searchDto.getRangeEnd() != null
                && searchDto.getRangeEnd().isBefore(searchDto.getRangeStart())) {
            throw new ValidationException("Дата окончания события должна быть после даты начала");
        }
        Predicate predicate = buildPublicPredicate(searchDto);
        Page<Event> eventsPage = eventRepository.findAll(predicate, pageable);
        if (eventsPage.isEmpty()) {
            log.debug("События по заданным критериям не найдены");
            return Collections.emptyList();
        }
        List<Event> events = eventsPage.getContent();
        Map<Long, UserDto> initiatorsMap = getInitiatorsMap(events);
        Map<Long, Double> ratings = getEventsRatings(events);
        Map<Long, Integer> confirmedRequests = getConfirmedRequests(events);
        List<EventShortDto> result = events.stream()
                .map(event -> {

                    UserDto userDto = initiatorsMap.get(event.getInitiatorId());
                    if (userDto == null) {
                        log.warn("Пользователь с ID {} не найден для события {}",
                                event.getInitiatorId(), event.getId());
                        throw new NotFoundException("Пользователь c userId " + event.getInitiatorId() + " не найден");
                    }

                    EventShortDto dto = EventMapper.toEventShortDto(event, userDto);
                    dto.setRating(ratings.get(event.getId()));
                    dto.setConfirmedRequests(confirmedRequests.getOrDefault(event.getId(), 0));
                    return dto;
                })
                .toList();
        return result;
    }

    @Override
    public EventFullDto getPublicEvent(Long id, Long userId, HttpServletRequest request) {
        collectorClient.sendUserAction(createUserAction(id, userId, ActionTypeProto.ACTION_VIEW));
        log.debug("Получение публичного события {}", id);
        Event event = eventRepository.findByIdAndState(id, Event.EventState.PUBLISHED)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Событие с id=%d не было найдено или не опубликовано", id)));

        UserDto userDto = getUserById(event.getInitiatorId());
        Integer confirmedRequests = getConfirmedRequestsCount(id);
        event.setConfirmedRequests(confirmedRequests);
        EventFullDto result = EventMapper.toEventFullDto(event, userDto);
        result.setRating(getEventRating(id));
        result.setConfirmedRequests(confirmedRequests);

        log.debug("Событие {} найдено", id);
        return result;
    }

    @Override
    public List<EventFullDto> getAdminEvents(SearchOfEventByAdminDto searchDto, Pageable pageable) {
        log.debug("Админ поиск событий по критериям: {}", searchDto);
        Predicate predicate = buildAdminPredicate(searchDto);
        Page<Event> eventsPage = eventRepository.findAll(predicate, pageable);
        if (eventsPage.isEmpty()) {
            log.debug("События по заданным критериям не найдены");
            return Collections.emptyList();
        }
        List<Event> events = eventsPage.getContent();
        Map<Long, UserDto> initiatorsMap = getInitiatorsMap(events);
        Map<Long, Double> ratings = getEventsRatings(events);
        Map<Long, Integer> confirmedRequests = getConfirmedRequests(events);
        return events.stream()
                .map(event -> {

                    UserDto userDto = initiatorsMap.get(event.getInitiatorId());
                    if (userDto == null) {
                        log.warn("Пользователь с ID {} не найден для события {}",
                                event.getInitiatorId(), event.getId());
                        throw new NotFoundException("Пользователь c userId " + event.getInitiatorId() + " не найден");
                    }

                    EventFullDto dto = EventMapper.toEventFullDto(event, userDto);
                    dto.setRating(ratings.get(event.getId()));
                    dto.setConfirmedRequests(confirmedRequests.getOrDefault(event.getId(), 0));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest updateRequest) {
        log.debug("Админ обновление события {}: {}", eventId, updateRequest);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Событие с id=%d не найдено", eventId)));
        updateEventFields(event, updateRequest);
        if (updateRequest.getStateAction() != null) {
            processAdminStateAction(event, updateRequest.getStateAction());
        }
        if (updateRequest.getEventDate() != null) {
            validateEventDateForAdmin(event, updateRequest.getEventDate());
            event.setEventDate(updateRequest.getEventDate());
        }
        Event updatedEvent = eventRepository.save(event);

        UserDto userDto = getUserById(event.getInitiatorId());

        Integer confirmedRequests = getConfirmedRequestsCount(eventId);
        updatedEvent.setConfirmedRequests(confirmedRequests);

        EventFullDto result = EventMapper.toEventFullDto(updatedEvent, userDto);
        result.setRating(getEventRating(eventId));
        result.setConfirmedRequests(confirmedRequests);

        log.info("Событие {} успешно обновлено администратором", eventId);
        return result;
    }

    @Override
    public List<EventShortDto> getRecommendations(Long userId) {
        log.debug("Получение рекомендаций для пользователя: {}", userId);
        List<RecommendedEventProto> recommendedEventProtos =  recommendationsClient.getRecommendationsForUser(
                UserPredictionsRequestProto.newBuilder()
                        .setUserId(userId)
                        .setMaxResults(MAX_RESULTS)
                        .build()
        );
        List<Long> eventIds = recommendedEventProtos.stream()
                .map(RecommendedEventProto::getEventId)
                .toList();
        List<Event> events = eventRepository.findAllById(eventIds);
        Map<Long, UserDto> initiatorsMap = getInitiatorsMap(events);
        Map<Long, Double> ratings = getEventsRatings(events);
        Map<Long, Integer> confirmedRequests = getConfirmedRequests(events);
        return events.stream()
                .map(event -> {

                    UserDto userDto = initiatorsMap.get(event.getInitiatorId());
                    if (userDto == null) {
                        log.warn("Пользователь с ID {} не найден для события {}",
                                event.getInitiatorId(), event.getId());
                        throw new NotFoundException("Пользователь c userId " + event.getInitiatorId() + " не найден");
                    }

                    EventShortDto dto = EventMapper.toEventShortDto(event, userDto);
                    dto.setRating(ratings.get(event.getId()));
                    dto.setConfirmedRequests(confirmedRequests.getOrDefault(event.getId(), 0));
                    return dto;
                })
                .toList();
    }

    @Override
    public void like(Long eventId, Long userId) {
        if (!requestClient.existsByRequesterIdAndEventId(userId, eventId)) {
            throw new ValidationException("Пользователь " + userId + " не принимал участи в событии " + eventId);
        }
        collectorClient.sendUserAction(createUserAction(eventId, userId, ActionTypeProto.ACTION_LIKE));
    }

    private Predicate buildPublicPredicate(SearchOfEventByPublicDto searchDto) {
        QEvent event = QEvent.event;
        BooleanBuilder predicate = new BooleanBuilder();

        // Только опубликованные события
        predicate.and(event.state.eq(Event.EventState.PUBLISHED));

        // Текст в аннотации или описании
        if (StringUtils.hasText(searchDto.getText())) {
            String text = searchDto.getText().toLowerCase();
            predicate.and(event.annotation.toLowerCase().contains(text)
                    .or(event.description.toLowerCase().contains(text)));
        }

        // Категории
        if (searchDto.getCategories() != null && !searchDto.getCategories().isEmpty()) {
            predicate.and(event.category.id.in(searchDto.getCategories()));
        }

        // Платные/бесплатные
        if (searchDto.getPaid() != null) {
            predicate.and(event.paid.eq(searchDto.getPaid()));
        }

        // Диапазон дат
        if (searchDto.getRangeStart() != null) {
            predicate.and(event.eventDate.goe(searchDto.getRangeStart()));
        }
        if (searchDto.getRangeEnd() != null) {
            predicate.and(event.eventDate.loe(searchDto.getRangeEnd()));
        }

        // Если не указан диапазон - только будущие события
        if (searchDto.getRangeStart() == null && searchDto.getRangeEnd() == null) {
            predicate.and(event.eventDate.after(LocalDateTime.now()));
        }

        // Только доступные (если требуется)
        if (Boolean.TRUE.equals(searchDto.getOnlyAvailable())) {
            predicate.and(event.participantLimit.eq(0)
                    .or(event.participantLimit.gt(event.confirmedRequests)));
        }

        return predicate;
    }

    private Predicate buildAdminPredicate(SearchOfEventByAdminDto searchDto) {
        QEvent event = QEvent.event;
        BooleanBuilder predicate = new BooleanBuilder();

        // Пользователи
        if (searchDto.getUsers() != null && !searchDto.getUsers().isEmpty()) {
            predicate.and(event.initiatorId.in(searchDto.getUsers()));
        }

        // Статусы
        if (searchDto.getStates() != null && !searchDto.getStates().isEmpty()) {
            List<Event.EventState> states = searchDto.getStates().stream()
                    .map(Event.EventState::valueOf)
                    .collect(Collectors.toList());
            predicate.and(event.state.in(states));
        }

        // Категории
        if (searchDto.getCategories() != null && !searchDto.getCategories().isEmpty()) {
            predicate.and(event.category.id.in(searchDto.getCategories()));
        }

        // Диапазон дат
        if (searchDto.getRangeStart() != null) {
            predicate.and(event.eventDate.goe(searchDto.getRangeStart()));
        }
        if (searchDto.getRangeEnd() != null) {
            predicate.and(event.eventDate.loe(searchDto.getRangeEnd()));
        }

        return predicate;
    }

    private void updateEventFields(Event event, UpdateEventAdminRequest updateRequest) {
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

    private void processAdminStateAction(Event event, StateAction stateAction) {
        switch (stateAction) {
            case PUBLISH_EVENT:
                validateEventCanBePublished(event);
                event.setState(Event.EventState.PUBLISHED);
                event.setPublishedOn(LocalDateTime.now());
                break;
            case REJECT_EVENT:
                validateEventCanBeRejected(event);
                event.setState(Event.EventState.CANCELED);
                break;
            default:
                throw new ValidationException("Неверное действие: " + stateAction);
        }
    }

    private void validateEventCanBePublished(Event event) {
        // Можно публиковать только события в состоянии ожидания публикации
        if (event.getState() != Event.EventState.PENDING) {
            throw new ConflictException("Не можем опубликовать событие, так как оно не в том состоянии: " + event.getState());
        }
        // Дата события должна быть не ранее чем через час от публикации
        if (event.getEventDate().isBefore(LocalDateTime.now().plusHours(1))) {
            throw new ConflictException("Не можем опубликовать событие, так как оно начинается слишком рано");
        }
    }

    private void validateEventCanBeRejected(Event event) {
        // Можно отклонять только неопубликованные события
        if (event.getState() == Event.EventState.PUBLISHED) {
            throw new ConflictException("Нельзя отклонить, так как уже опубликовано");
        }
    }

    private void validateEventDateForAdmin(Event event, LocalDateTime newEventDate) {
        // Для админа: дата должна быть минимум через 1 час если событие опубликовано
        if (event.getState() == Event.EventState.PUBLISHED &&
                newEventDate.isBefore(LocalDateTime.now().plusHours(1))) {
            throw new ConflictException("Дата начала изменяемого события должна быть не ранее чем за час от даты публикации");
        }
    }

    private Map<Long, UserDto> getInitiatorsMap(List<Event> events) {
        Set<Long> initiatorIds = events.stream()
                .map(Event::getInitiatorId)
                .collect(Collectors.toSet());

        return getUsersByIds(new ArrayList<>(initiatorIds));
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
    public List<ParticipationRequestDto> getParticipationRequests(Long userId, Long eventId) {
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
    public EventRequestStatusUpdateResult updateParticipationRequests(UserIdAndEventIdDto userIdAndEventIdDto, EventRequestStatusUpdateRequest updateRequest) {
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
                .toList();

        List<RequestDto> updatedRejected = updatedRequests.stream()
                .filter(req -> RequestDto.RequestStatusDto.REJECTED == req.getStatus())
                .toList();

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

    UserActionProto createUserAction(Long eventId, Long userId, ActionTypeProto typeProto) {
        Instant timestamp = Instant.now();
        return UserActionProto.newBuilder()
                .setUserId(userId)
                .setEventId(eventId)
                .setActionType(typeProto)
                .setTimestamp(Timestamp.newBuilder()
                        .setSeconds(timestamp.getEpochSecond())
                        .setNanos(timestamp.getNano())
                        .build())
                .build();
    }
}
