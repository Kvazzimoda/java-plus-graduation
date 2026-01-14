package ru.practicum.main.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Pageable;
import ru.practicum.main.dto.request.event.*;
import ru.practicum.main.dto.response.event.EventFullDto;
import ru.practicum.main.dto.response.event.EventRequestStatusUpdateResult;
import ru.practicum.main.dto.response.event.EventShortDto;
import ru.practicum.main.dto.response.request.ParticipationRequestDto;

import java.util.List;

public interface EventService {

    // ===================== PUBLIC =====================

    List<EventShortDto> getPublicEvents(SearchOfEventByPublicDto searchDto,
                                        Pageable pageable,
                                        HttpServletRequest request);

    EventFullDto getPublicEvent(Long id, Long userId, HttpServletRequest request);

    List<EventShortDto> getRecommendations(Long userId);

    void like(Long eventId, Long userId);

    // ===================== PRIVATE (USER) =====================

    List<EventShortDto> getUserEvents(Long userId, Pageable pageable);

    EventFullDto addEventByUser(Long userId, NewEventDto newEventDto);

    EventFullDto getUserEvent(Long userId, Long eventId);

    EventFullDto updateEventByUser(UserIdAndEventIdDto userIdAndEventIdDto,
                                   UpdateEventUserRequest updateEventUserRequest);

    List<ParticipationRequestDto> getParticipationRequests(Long userId, Long eventId);

    EventRequestStatusUpdateResult updateParticipationRequests(
            UserIdAndEventIdDto userIdAndEventIdDto,
            EventRequestStatusUpdateRequest eventRequestStatusUpdateRequest
    );

    // ===================== ADMIN =====================

    List<EventFullDto> getAdminEvents(SearchOfEventByAdminDto searchDto,
                                      Pageable pageable);

    EventFullDto updateEventByAdmin(Long eventId,
                                    UpdateEventAdminRequest updateEventAdminRequest);
}
