package ru.practicum.comment.client.event;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.comment.dto.response.event.EventDto;


@FeignClient(name = "event-service", fallback = EventServiceClientFallback.class)
public interface EventClient {

    @GetMapping("/internal/events/{eventId}")
    EventDto getEventById(@PathVariable("eventId") Long eventId);
}
