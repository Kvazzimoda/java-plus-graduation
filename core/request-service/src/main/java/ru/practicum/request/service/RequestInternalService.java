package ru.practicum.request.service;

import ru.practicum.core.dto.ConfirmedRequestsCountDto;
import ru.practicum.core.dto.RequestDto;
import ru.practicum.core.dto.RequestStatusUpdateDto;

import java.util.List;

public interface RequestInternalService {
    Integer countConfirmedRequestsByEventId(Long eventId);
    List<ConfirmedRequestsCountDto> countConfirmedRequestsByEventIds(List<Long> eventIds);
    List<RequestDto> getRequestsByEventId(Long eventId);

    List<RequestDto> findAllByRequesterId(Long requesterId);
    Boolean existsByRequesterIdAndEventId(Long requesterId, Long eventId);
    Integer countByEventId(Long eventId);
    List<RequestDto> findAllByIdInAndEventId(List<Long> ids, Long eventId);
    void updateRequestsStatus(RequestStatusUpdateDto updateDto);
}
