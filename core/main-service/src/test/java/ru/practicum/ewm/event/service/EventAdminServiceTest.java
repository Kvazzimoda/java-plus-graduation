package ru.practicum.ewm.event.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.category.service.CategoryService;
import ru.practicum.ewm.comment.repository.CommentRepository;
import ru.practicum.ewm.event.dto.EventFullDto;
import ru.practicum.ewm.event.dto.UpdateEventAdminRequest;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.event.model.StateAction;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.exception.ValidationException;
import ru.practicum.ewm.location.dto.LocationDto;
import ru.practicum.ewm.location.model.Location;
import ru.practicum.ewm.location.service.LocationService;
import ru.practicum.ewm.mapper.EwmMapper;
import ru.practicum.ewm.request.repository.ParticipationRequestRepository;
import ru.practicum.ewm.user.model.User;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventAdminServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private ParticipationRequestRepository requestRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private CategoryService categoryService;

    @Mock
    private LocationService locationService;

    @Mock
    private EwmMapper mapper;

    @InjectMocks
    private EventAdminService eventAdminService;

    private Event event;
    private User user;
    private Category category;
    private Location location;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .name("Test User")
                .email("test@example.com")
                .build();

        category = Category.builder()
                .id(1L)
                .name("Test Category")
                .build();

        location = Location.builder()
                .id(1L)
                .lat(55.7558f)
                .lon(37.6176f)
                .build();

        event = Event.builder()
                .id(1L)
                .title("Test Event")
                .annotation("Test annotation")
                .description("Test description")
                .eventDate(LocalDateTime.now().plusDays(1))
                .createdOn(LocalDateTime.now())
                .initiator(user)
                .category(category)
                .location(location)
                .state(EventState.PENDING)
                .paid(false)
                .participantLimit(0)
                .requestModeration(true)
                .build();
    }

    @Test
    void getEvents_WithInvalidDateRange_ShouldThrowValidationException() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = LocalDateTime.now();

        ValidationException exception = assertThrows(ValidationException.class,
                () -> eventAdminService.getEvents(null, null, null, start, end, 0, 10));

        assertEquals("Дата начала не может быть позже даты окончания", exception.getMessage());
    }

    @Test
    void updateEvent_ValidRequest_ShouldReturnUpdatedEvent() {
        UpdateEventAdminRequest request = new UpdateEventAdminRequest();
        request.setTitle("Updated Title");
        request.setEventDate(LocalDateTime.now().plusDays(2));

        EventFullDto expectedDto = new EventFullDto();

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenReturn(event);
        when(requestRepository.countConfirmedRequestsByEventId(1L)).thenReturn(0L);
        when(commentRepository.countByEventId(1L)).thenReturn(0L);
        when(mapper.toEventFullDto(any(Event.class), anyLong(), anyLong(), anyLong())).thenReturn(expectedDto);

        EventFullDto result = eventAdminService.updateEvent(1L, request);

        assertNotNull(result);
        verify(eventRepository).save(event);
        assertEquals("Updated Title", event.getTitle());
    }

    @Test
    void updateEvent_WithCategory_ShouldUpdateCategory() {
        UpdateEventAdminRequest request = new UpdateEventAdminRequest();
        request.setCategory(2L);

        Category newCategory = Category.builder().id(2L).name("New Category").build();
        EventFullDto expectedDto = new EventFullDto();

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(categoryService.getCategoryById(2L)).thenReturn(newCategory);
        when(eventRepository.save(any(Event.class))).thenReturn(event);
        when(requestRepository.countConfirmedRequestsByEventId(1L)).thenReturn(0L);
        when(commentRepository.countByEventId(1L)).thenReturn(0L);
        when(mapper.toEventFullDto(any(Event.class), anyLong(), anyLong(), anyLong())).thenReturn(expectedDto);

        EventFullDto result = eventAdminService.updateEvent(1L, request);

        assertNotNull(result);
        verify(categoryService).getCategoryById(2L);
        assertEquals(newCategory, event.getCategory());
    }

    @Test
    void updateEvent_WithLocation_ShouldUpdateLocation() {
        UpdateEventAdminRequest request = new UpdateEventAdminRequest();
        LocationDto locationDto = new LocationDto();
        locationDto.setLat(60.0f);
        locationDto.setLon(30.0f);
        request.setLocation(locationDto);

        Location newLocation = Location.builder().id(2L).lat(60.0f).lon(30.0f).build();
        EventFullDto expectedDto = new EventFullDto();

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(locationService.getOrCreateLocation(locationDto)).thenReturn(newLocation);
        when(eventRepository.save(any(Event.class))).thenReturn(event);
        when(requestRepository.countConfirmedRequestsByEventId(1L)).thenReturn(0L);
        when(commentRepository.countByEventId(1L)).thenReturn(0L);
        when(mapper.toEventFullDto(any(Event.class), anyLong(), anyLong(), anyLong())).thenReturn(expectedDto);

        EventFullDto result = eventAdminService.updateEvent(1L, request);

        assertNotNull(result);
        verify(locationService).getOrCreateLocation(locationDto);
        assertEquals(newLocation, event.getLocation());
    }

    @Test
    void updateEvent_EventNotFound_ShouldThrowNotFoundException() {
        UpdateEventAdminRequest request = new UpdateEventAdminRequest();

        when(eventRepository.findById(1L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> eventAdminService.updateEvent(1L, request));

        assertEquals("Событие с ID 1 не найдено", exception.getMessage());
    }

    @Test
    void updateEvent_WithInvalidEventDate_ShouldThrowValidationException() {
        UpdateEventAdminRequest request = new UpdateEventAdminRequest();
        request.setEventDate(LocalDateTime.now().minusHours(1));

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        ValidationException exception = assertThrows(ValidationException.class,
                () -> eventAdminService.updateEvent(1L, request));

        assertEquals("Дата начала изменяемого события должна быть не ранее чем за час от даты публикации",
                exception.getMessage());
    }

    @Test
    void updateEvent_PublishEvent_ShouldChangeStateToPublished() {
        UpdateEventAdminRequest request = new UpdateEventAdminRequest();
        request.setStateAction(StateAction.PUBLISH_EVENT);
        event.setEventDate(LocalDateTime.now().plusHours(2));

        EventFullDto expectedDto = new EventFullDto();

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenReturn(event);
        when(requestRepository.countConfirmedRequestsByEventId(1L)).thenReturn(0L);
        when(commentRepository.countByEventId(1L)).thenReturn(0L);
        when(mapper.toEventFullDto(any(Event.class), anyLong(), anyLong(), anyLong())).thenReturn(expectedDto);

        EventFullDto result = eventAdminService.updateEvent(1L, request);

        assertNotNull(result);
        assertEquals(EventState.PUBLISHED, event.getState());
        assertNotNull(event.getPublishedOn());
        verify(eventRepository).save(event);
    }

    @Test
    void updateEvent_PublishEventWithInvalidDate_ShouldThrowConflictException() {
        UpdateEventAdminRequest request = new UpdateEventAdminRequest();
        request.setStateAction(StateAction.PUBLISH_EVENT);
        event.setEventDate(LocalDateTime.now().plusMinutes(30)); // Меньше часа

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        ConflictException exception = assertThrows(ConflictException.class,
                () -> eventAdminService.updateEvent(1L, request));

        assertEquals("Дата начала события должна быть не ранее чем за час от даты публикации",
                exception.getMessage());
    }

    @Test
    void updateEvent_PublishAlreadyPublishedEvent_ShouldThrowConflictException() {
        UpdateEventAdminRequest request = new UpdateEventAdminRequest();
        request.setStateAction(StateAction.PUBLISH_EVENT);
        event.setState(EventState.PUBLISHED);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        ConflictException exception = assertThrows(ConflictException.class,
                () -> eventAdminService.updateEvent(1L, request));

        assertEquals("Событие можно публиковать только если оно в состоянии ожидания модерации",
                exception.getMessage());
    }

    @Test
    void updateEvent_RejectEvent_ShouldChangeStateToCanceled() {
        UpdateEventAdminRequest request = new UpdateEventAdminRequest();
        request.setStateAction(StateAction.REJECT_EVENT);

        EventFullDto expectedDto = new EventFullDto();

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenReturn(event);
        when(requestRepository.countConfirmedRequestsByEventId(1L)).thenReturn(0L);
        when(commentRepository.countByEventId(1L)).thenReturn(0L);
        when(mapper.toEventFullDto(any(Event.class), anyLong(), anyLong(), anyLong())).thenReturn(expectedDto);

        EventFullDto result = eventAdminService.updateEvent(1L, request);

        assertNotNull(result);
        assertEquals(EventState.CANCELED, event.getState());
        verify(eventRepository).save(event);
    }

    @Test
    void updateEvent_RejectPublishedEvent_ShouldThrowConflictException() {
        UpdateEventAdminRequest request = new UpdateEventAdminRequest();
        request.setStateAction(StateAction.REJECT_EVENT);
        event.setState(EventState.PUBLISHED);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        ConflictException exception = assertThrows(ConflictException.class,
                () -> eventAdminService.updateEvent(1L, request));

        assertEquals("Событие можно отклонить только если оно еще не опубликовано",
                exception.getMessage());
    }
}
