package ru.practicum.main.service.interfaces;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Pageable;
import ru.practicum.main.dto.request.event.SearchOfEventByPublicDto;
import ru.practicum.main.dto.response.event.EventFullDto;
import ru.practicum.main.dto.response.event.EventShortDto;

import java.util.List;

public interface EventPublicService {

    List<EventShortDto> getEvents(SearchOfEventByPublicDto searchOfEventByPublicDto, Pageable pageable, HttpServletRequest request);

    EventFullDto getEvent(Long id, HttpServletRequest request);
}
