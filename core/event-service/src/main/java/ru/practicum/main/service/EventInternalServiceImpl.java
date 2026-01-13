package ru.practicum.main.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.dto.response.event.EventDto;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.model.Event;
import ru.practicum.main.repository.EventRepository;
import ru.practicum.main.service.interfaces.EventInternalService;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventInternalServiceImpl implements EventInternalService {
    private final EventRepository eventRepository;

    @Override
    public EventDto getEventById(Long eventId) {
        log.debug("Получение события по ID для внутреннего использования: {}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        return EventDto.builder()
                .id(event.getId())
                .initiatorId(event.getInitiatorId())
                .participantLimit(event.getParticipantLimit())
                .requestModeration(event.getRequestModeration())
                .state(event.getState())
                .build();
    }
}
