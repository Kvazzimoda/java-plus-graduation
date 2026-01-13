package ru.practicum.main.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.main.dto.response.event.EventDto;
import ru.practicum.main.service.interfaces.EventInternalService;

@RestController
@RequestMapping("/internal/events")
@RequiredArgsConstructor
public class EventInternalController {
    private final EventInternalService eventInternalService;

    @GetMapping("/{eventId}")
    public EventDto getEventById(@PathVariable Long eventId) {
        return eventInternalService.getEventById(eventId);
    }
}
