package ru.practicum.main.service;


import ru.practicum.core.dto.EventDto;

public interface EventInternalService {
    EventDto getEventById(Long eventId);
}
