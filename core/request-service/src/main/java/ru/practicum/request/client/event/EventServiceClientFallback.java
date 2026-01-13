package ru.practicum.request.client.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.request.dto.response.event.EventDto;

@Slf4j
@Component
public class EventServiceClientFallback implements EventClient {

    @Override
    public EventDto getEventById(Long eventId) {
        log.error("Failed to get event with id={} from event-service", eventId);
        throw new RuntimeException("Event service is unavailable");
    }
}
