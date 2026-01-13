package ru.practicum.main.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.main.dto.request.event.EventRequestStatusUpdateRequest;
import ru.practicum.main.dto.request.event.NewEventDto;
import ru.practicum.main.dto.request.event.UpdateEventUserRequest;
import ru.practicum.main.dto.request.event.UserIdAndEventIdDto;
import ru.practicum.main.dto.response.event.EventFullDto;
import ru.practicum.main.dto.response.event.EventRequestStatusUpdateResult;
import ru.practicum.main.dto.response.event.EventShortDto;
import ru.practicum.main.dto.response.request.ParticipationRequestDto;
import ru.practicum.main.service.interfaces.EventPrivateService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Validated
public class EventPrivateController {

    private final EventPrivateService eventPrivateService;

    @GetMapping("/{userId}/events")
    public List<EventShortDto> getEvents(@PathVariable @Positive Long userId,
                                         @PositiveOrZero @RequestParam(name = "from", defaultValue = "0") Integer from,
                                         @Positive @RequestParam(name = "size", defaultValue = "10") Integer size) {
        log.debug("Поступил запрос на получение событий, добавленных текущим пользователем {}, от {} события, всего {}",
                userId, from, size);
        Pageable pageable = PageRequest.of(from, size);
        return eventPrivateService.getEvents(userId, pageable);
    }

    @PostMapping("/{userId}/events")
    @ResponseStatus(HttpStatus.CREATED)
    public EventFullDto addEvent(@PathVariable @Positive Long userId,
                                 @Valid @RequestBody NewEventDto newEventDto) {
        log.debug("Поступил запрос на создание события {} текущим пользователем {}", newEventDto, userId);
        return eventPrivateService.addEvent(userId, newEventDto);
    }

    @GetMapping("/{userId}/events/{eventId}")
    public EventFullDto getEvent(@PathVariable @Positive Long userId,
                                 @PathVariable @Positive Long eventId) {
        log.debug("Поступил запрос на получение события {} пользователя {}", eventId, userId);
        return eventPrivateService.getEvent(eventId, userId);
    }

    @PatchMapping("/{userId}/events/{eventId}")
    public EventFullDto updateEvent(@PathVariable @Positive Long userId,
                                    @PathVariable @Positive Long eventId,
                                    @Valid @RequestBody UpdateEventUserRequest updateEventUserRequest) {
        log.debug("Поступил запрос на обновление события {} с id {} пользователя {}", updateEventUserRequest,
                eventId, userId);
        UserIdAndEventIdDto userIdAndEventIdDto = UserIdAndEventIdDto.builder()
                .userId(userId)
                .eventId(eventId)
                .build();
        return eventPrivateService.updateEvent(userIdAndEventIdDto, updateEventUserRequest);
    }

    @GetMapping("/{userId}/events/{eventId}/requests")
    public List<ParticipationRequestDto> getRequests(@PathVariable @Positive Long userId,
                                                     @PathVariable @Positive Long eventId) {
        log.debug("Поступил запрос на получение информации о запросах на участие в событии {} " +
                "текущего пользователя {}", eventId, userId);
        return eventPrivateService.getRequests(userId, eventId);
    }

    @PatchMapping("/{userId}/events/{eventId}/requests")
    public EventRequestStatusUpdateResult updateRequests(@PathVariable @Positive Long userId,
                                                         @PathVariable @Positive Long eventId,
                                                         @Valid @RequestBody EventRequestStatusUpdateRequest eventRequestStatusUpdateRequest) {
        log.debug("Поступил запрос {} на изменение статуса (подтверждена, отменена) заявок на участие в событии {}  " +
                "текущего пользователя {}", eventRequestStatusUpdateRequest, eventId, userId);
        UserIdAndEventIdDto userIdAndEventIdDto = UserIdAndEventIdDto.builder()
                .userId(userId)
                .eventId(eventId)
                .build();
        return eventPrivateService.updateRequests(userIdAndEventIdDto, eventRequestStatusUpdateRequest);
    }
}
