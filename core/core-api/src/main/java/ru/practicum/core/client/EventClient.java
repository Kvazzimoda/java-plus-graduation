package ru.practicum.core.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.core.dto.EventDto;


@FeignClient(
        name = "event-service",
        path = "/internal/events"
)
public interface EventClient {

    @GetMapping("/{eventId}")
    EventDto getEventById(@PathVariable Long eventId);
}
