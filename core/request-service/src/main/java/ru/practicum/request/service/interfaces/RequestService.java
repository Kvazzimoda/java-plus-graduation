package ru.practicum.request.service.interfaces;


import ru.practicum.request.dto.response.request.ParticipationRequestDto;

import java.util.List;

public interface RequestService {
    ParticipationRequestDto addRequest(Long userId, Long eventId);

    List<ParticipationRequestDto> getRequestsByRequesterId(Long userId);

    ParticipationRequestDto cancel(Long userId, Long requestId);


}
