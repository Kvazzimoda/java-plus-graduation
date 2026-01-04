package ru.practicum.ewm.event.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.event.dto.*;
import ru.practicum.ewm.event.service.EventPrivateService;
import ru.practicum.ewm.exception.ValidationException;
import ru.practicum.ewm.request.dto.*;
import ru.practicum.ewm.request.service.RequestPrivateService;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}")
@RequiredArgsConstructor
@Slf4j
public class PrivateEventController {

    private final EventPrivateService eventService;
    private final RequestPrivateService requestService;

    // ------------------ EVENTS ------------------

    @GetMapping("/events")
    public List<EventShortDto> getUserEvents(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "10") int size) {
        log.info("GET /users/{}/events", userId);
        return eventService.getUserEvents(userId, from, size);
    }

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.CREATED)
    public EventFullDto addEvent(
            @PathVariable Long userId,
            @Valid @RequestBody NewEventDto dto) {
        log.info("POST /users/{}/events", userId);
        return eventService.createEvent(userId, dto);
    }

    @GetMapping("/events/{eventId}")
    public EventFullDto getUserEvent(
            @PathVariable Long userId,
            @PathVariable Long eventId) {
        log.info("GET /users/{}/events/{}", userId, eventId);
        return eventService.getUserEvent(userId, eventId);
    }

    @PatchMapping("/events/{eventId}")
    public EventFullDto updateEvent(
            @PathVariable Long userId,
            @PathVariable Long eventId,
            @Valid @RequestBody UpdateEventUserRequest dto) {
        log.info("PATCH /users/{}/events/{}", userId, eventId);
        return eventService.updateEvent(userId, eventId, dto);
    }

    // ------------------ PARTICIPATION REQUESTS ------------------

    @GetMapping("/events/{eventId}/requests")
    public List<ParticipationRequestDto> getEventParticipants(
            @PathVariable Long userId,
            @PathVariable Long eventId) {
        log.info("GET /users/{}/events/{}/requests", userId, eventId);
        return requestService.getRequestsForUserEvent(userId, eventId);
    }

    @PatchMapping("/events/{eventId}/requests")
    public EventRequestStatusUpdateResult changeRequestStatus(
            @PathVariable Long userId,
            @PathVariable Long eventId,
            @Valid @RequestBody EventRequestStatusUpdateRequest dto) {
        log.info("PATCH /users/{}/events/{}/requests", userId, eventId);
        return requestService.changeRequestStatus(userId, eventId, dto);
    }

    @GetMapping("/requests")
    public List<ParticipationRequestDto> getUserRequests(@PathVariable Long userId) {
        log.info("GET /users/{}/requests", userId);
        return requestService.getUserRequests(userId);
    }

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipationRequestDto addParticipationRequest(
            @PathVariable Long userId,
            @RequestParam(required = false) Long eventId) {

        if (eventId == null) {
            throw new ValidationException("ID события не может быть пустым");
        }

        log.info("POST /users/{}/requests?eventId={}", userId, eventId);
        return requestService.addRequest(userId, eventId);
    }

    @PatchMapping("/requests/{requestId}/cancel")
    public ParticipationRequestDto cancelRequest(
            @PathVariable Long userId,
            @PathVariable Long requestId) {
        log.info("PATCH /users/{}/requests/{}/cancel", userId, requestId);
        return requestService.cancelRequest(userId, requestId);
    }
}