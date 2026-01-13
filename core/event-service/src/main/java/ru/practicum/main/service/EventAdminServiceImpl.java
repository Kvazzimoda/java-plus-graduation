package ru.practicum.main.service;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.client.request.RequestClient;
import ru.practicum.main.client.user.UserClient;
import ru.practicum.main.dto.mappers.EventMapper;
import ru.practicum.main.dto.mappers.LocationMapper;
import ru.practicum.main.dto.request.event.SearchOfEventByAdminDto;
import ru.practicum.main.dto.request.event.StateAction;
import ru.practicum.main.dto.request.event.UpdateEventAdminRequest;
import ru.practicum.main.dto.response.event.EventFullDto;
import ru.practicum.main.dto.response.user.UserDto;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.exception.ValidationException;
import ru.practicum.main.model.Category;
import ru.practicum.main.model.Event;
import ru.practicum.main.model.LocationEntity;
import ru.practicum.main.repository.CategoryRepository;
import ru.practicum.main.repository.EventRepository;
import ru.practicum.main.repository.LocationRepository;
import ru.practicum.main.service.interfaces.EventAdminService;
import ru.practicum.stats.client.StatClient;
import ru.practicum.main.model.QEvent;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
public class EventAdminServiceImpl extends AbstractEventService implements EventAdminService {

    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final LocationRepository locationRepository;

    public EventAdminServiceImpl(RequestClient requestClient,
                                 StatClient statClient,
                                 UserClient userClient,
                                 EventRepository eventRepository,
                                 CategoryRepository categoryRepository,
                                 LocationRepository locationRepository) {
        super(requestClient, statClient, userClient);
        this.eventRepository = eventRepository;
        this.categoryRepository = categoryRepository;
        this.locationRepository = locationRepository;
    }

    @Override
    public List<EventFullDto> getEvents(SearchOfEventByAdminDto searchDto, Pageable pageable) {
        log.debug("Админ поиск событий по критериям: {}", searchDto);
        Predicate predicate = buildPredicate(searchDto);
        Page<Event> eventsPage = eventRepository.findAll(predicate, pageable);
        if (eventsPage.isEmpty()) {
            log.debug("События по заданным критериям не найдены");
            return Collections.emptyList();
        }
        List<Event> events = eventsPage.getContent();
        Map<Long, UserDto> initiatorsMap = getInitiatorsMap(events);
        Map<Long, Long> views = getEventsViews(events);
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
                    dto.setViews(views.getOrDefault(event.getId(), 0L));
                    dto.setConfirmedRequests(confirmedRequests.getOrDefault(event.getId(), 0));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto updateEvent(Long eventId, UpdateEventAdminRequest updateRequest) {
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

        Long views = getEventViews(eventId);
        Integer confirmedRequests = getConfirmedRequestsCount(eventId);
        updatedEvent.setConfirmedRequests(confirmedRequests);

        EventFullDto result = EventMapper.toEventFullDto(updatedEvent, userDto);
        result.setViews(views);
        result.setConfirmedRequests(confirmedRequests);

        log.info("Событие {} успешно обновлено администратором", eventId);
        return result;
    }

    private Predicate buildPredicate(SearchOfEventByAdminDto searchDto) {
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
}
