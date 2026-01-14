package ru.practicum.main.service;

import ru.practicum.main.dto.response.event.EventDto;

public interface EventInternalService {
    EventDto getEventById(Long eventId);
}
