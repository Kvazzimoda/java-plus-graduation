package ru.practicum.main.service.interfaces;

import org.springframework.data.domain.Pageable;
import ru.practicum.main.dto.request.event.EventRequestStatusUpdateRequest;
import ru.practicum.main.dto.request.event.NewEventDto;
import ru.practicum.main.dto.request.event.UpdateEventUserRequest;
import ru.practicum.main.dto.request.event.UserIdAndEventIdDto;
import ru.practicum.main.dto.response.event.EventFullDto;
import ru.practicum.main.dto.response.event.EventRequestStatusUpdateResult;
import ru.practicum.main.dto.response.event.EventShortDto;
import ru.practicum.main.dto.response.request.ParticipationRequestDto;

import java.util.List;

public interface EventPrivateService {

    List<EventShortDto> getEvents(Long userId, Pageable pageable);

    EventFullDto addEvent(Long userId, NewEventDto newEventDto);

    EventFullDto getEvent(Long eventId, Long userId);

    EventFullDto updateEvent(UserIdAndEventIdDto userIdAndEventIdDto, UpdateEventUserRequest updateEventUserRequest);

    List<ParticipationRequestDto> getRequests(Long userId, Long eventId);

    EventRequestStatusUpdateResult updateRequests(UserIdAndEventIdDto userIdAndEventIdDto, EventRequestStatusUpdateRequest eventRequestStatusUpdateRequest);
}
