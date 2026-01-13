package ru.practicum.request.service.interfaces;

import ru.practicum.request.dto.request.request.RequestStatusUpdateDto;
import ru.practicum.request.dto.response.request.ConfirmedRequestsCountDto;
import ru.practicum.request.dto.response.request.RequestDto;

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
