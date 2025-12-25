package ru.practicum.ewm.event.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import ru.practicum.client.StatsClient;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStats;
import ru.practicum.ewm.category.model.Category;
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
import ru.practicum.ewm.location.model.Location;
import ru.practicum.ewm.mapper.EwmMapper;
import ru.practicum.ewm.request.repository.ParticipationRequestRepository;
import ru.practicum.ewm.user.model.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventPublicServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private ParticipationRequestRepository requestRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private StatsClient statsClient;

    @Mock
    private EwmMapper mapper;

    @InjectMocks
    private EventPublicService eventPublicService;

    private Event testEvent;
    private EventShortDto testEventShortDto;
    private EventFullDto testEventFullDto;
    private EventSearchParams testParams;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId(1L);
        user.setName("Test User");
        user.setEmail("test@example.com");

        Category category = new Category();
        category.setId(1L);
        category.setName("Test Category");

        Location location = new Location();
        location.setId(1L);
        location.setLat(55.7558f);
        location.setLon(37.6176f);

        testEvent = Event.builder()
                .id(1L)
                .title("Test Event")
                .annotation("Test annotation for event")
                .description("Test description for event")
                .category(category)
                .initiator(user)
                .location(location)
                .eventDate(LocalDateTime.now().plusDays(1))
                .createdOn(LocalDateTime.now())
                .state(EventState.PUBLISHED)
                .paid(false)
                .participantLimit(10)
                .requestModeration(true)
                .build();

        testEventShortDto = new EventShortDto();
        testEventShortDto.setId(1L);
        testEventShortDto.setTitle("Test Event");
        testEventShortDto.setViews(0L);
        testEventShortDto.setConfirmedRequests(0L);
        testEventShortDto.setCommentsCount(0L);

        testEventFullDto = new EventFullDto();
        testEventFullDto.setId(1L);
        testEventFullDto.setTitle("Test Event");
        testEventFullDto.setViews(0L);
        testEventFullDto.setConfirmedRequests(0L);
        testEventFullDto.setCommentsCount(0L);

        testParams = new EventSearchParams();
        testParams.setFrom(0);
        testParams.setSize(10);
    }

    @Test
    void getEvents_WithInvalidDateRange_ShouldThrowValidationException() {
        testParams.setRangeStart(LocalDateTime.now().plusDays(2));
        testParams.setRangeEnd(LocalDateTime.now().plusDays(1));

        ValidationException exception = assertThrows(ValidationException.class,
                () -> eventPublicService.getEvents(testParams, "127.0.0.1", "/events"));

        assertEquals("Дата начала не может быть позже даты окончания", exception.getMessage());
        verify(eventRepository, never()).findPublishedEvents(any(), any(), any(), any(), any(), any());
    }

    @Test
    void getEvents_WithViewsSort_ShouldSortByViews() {
        testParams.setSort(EventSort.VIEWS.name());

        EventShortDto event1 = new EventShortDto();
        event1.setId(1L);
        event1.setViews(5L);
        event1.setCommentsCount(0L);

        EventShortDto event2 = new EventShortDto();
        event2.setId(2L);
        event2.setViews(10L);
        event2.setCommentsCount(0L);

        Event testEvent2 = Event.builder()
                .id(2L)
                .title("Test Event 2")
                .annotation("Test annotation for event 2")
                .category(testEvent.getCategory())
                .initiator(testEvent.getInitiator())
                .location(testEvent.getLocation())
                .eventDate(LocalDateTime.now().plusDays(1))
                .createdOn(LocalDateTime.now())
                .state(EventState.PUBLISHED)
                .build();

        Page<Event> eventPage = new PageImpl<>(List.of(testEvent, testEvent2));
        when(eventRepository.findPublishedEvents(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(eventPage);
        when(mapper.toEventShortDto(testEvent)).thenReturn(event1);
        when(mapper.toEventShortDto(testEvent2)).thenReturn(event2);
        when(requestRepository.countConfirmedRequestsByEventIds(anyList()))
                .thenReturn(List.of(new Object[]{1L, 0L}, new Object[]{2L, 0L}));
        when(commentRepository.countCommentsByEventIds(anyList()))
                .thenReturn(List.of(new Object[]{1L, 0L}, new Object[]{2L, 0L}));

        ViewStats stats1 = new ViewStats("ewm-main-service", "/events/1", 5L);
        ViewStats stats2 = new ViewStats("ewm-main-service", "/events/2", 10L);
        when(statsClient.getStat(anyString(), anyString(), anyList(), anyBoolean()))
                .thenReturn(List.of(stats1, stats2));

        List<EventShortDto> result = eventPublicService.getEvents(testParams, "127.0.0.1", "/events");

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.get(0).getViews() <= result.get(1).getViews());
        verify(statsClient).hit(any(EndpointHitDto.class));
    }

    @Test
    void getEventById_NotFound_ShouldThrowNotFoundException() {
        when(eventRepository.findByIdAndState(999L, EventState.PUBLISHED))
                .thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> eventPublicService.getEventById(999L, "127.0.0.1", "/events/999"));

        assertEquals("Событие с ID 999 не найдено", exception.getMessage());
        verify(statsClient, never()).hit(any());
    }

    @Test
    void getEvents_WithOnlyAvailable_ShouldFilterByAvailability() {
        testParams.setOnlyAvailable(true);

        Event eventWithLimit = Event.builder()
                .id(1L)
                .title("Event with limit")
                .annotation("Test annotation")
                .category(testEvent.getCategory())
                .initiator(testEvent.getInitiator())
                .location(testEvent.getLocation())
                .eventDate(LocalDateTime.now().plusDays(1))
                .createdOn(LocalDateTime.now())
                .state(EventState.PUBLISHED)
                .participantLimit(5)
                .build();

        Event eventWithoutLimit = Event.builder()
                .id(2L)
                .title("Event without limit")
                .annotation("Test annotation")
                .category(testEvent.getCategory())
                .initiator(testEvent.getInitiator())
                .location(testEvent.getLocation())
                .eventDate(LocalDateTime.now().plusDays(1))
                .createdOn(LocalDateTime.now())
                .state(EventState.PUBLISHED)
                .participantLimit(0)
                .build();

        Page<Event> eventPage = new PageImpl<>(List.of(eventWithLimit, eventWithoutLimit));
        when(eventRepository.findPublishedEvents(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(eventPage);

        // Событие с лимитом 5 и 3 подтвержденными заявками - доступно
        // Событие без лимита - всегда доступно
        when(requestRepository.countConfirmedRequestsByEventIds(anyList()))
                .thenReturn(List.of(new Object[]{1L, 3L}, new Object[]{2L, 0L}));
        when(commentRepository.countCommentsByEventIds(anyList()))
                .thenReturn(List.of(new Object[]{1L, 0L}, new Object[]{2L, 0L}));

        EventShortDto dto1 = new EventShortDto();
        dto1.setId(1L);
        dto1.setViews(0L);
        dto1.setConfirmedRequests(3L);
        dto1.setCommentsCount(0L);

        EventShortDto dto2 = new EventShortDto();
        dto2.setId(2L);
        dto2.setViews(0L);
        dto2.setConfirmedRequests(0L);
        dto2.setCommentsCount(0L);

        when(mapper.toEventShortDto(eventWithLimit)).thenReturn(dto1);
        when(mapper.toEventShortDto(eventWithoutLimit)).thenReturn(dto2);
        when(statsClient.getStat(anyString(), anyString(), anyList(), anyBoolean()))
                .thenReturn(List.of());

        List<EventShortDto> result = eventPublicService.getEvents(testParams, "127.0.0.1", "/events");

        assertNotNull(result);
        assertEquals(2, result.size()); // Оба события доступны
        verify(statsClient).hit(any(EndpointHitDto.class));
    }
}
