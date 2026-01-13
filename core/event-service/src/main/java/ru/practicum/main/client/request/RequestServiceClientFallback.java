package ru.practicum.main.client.request;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.main.dto.response.request.ConfirmedRequestsCountDto;
import ru.practicum.main.dto.response.request.RequestDto;
import ru.practicum.main.dto.response.request.RequestStatusUpdateDto;
import ru.practicum.main.exception.NotFoundException;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class RequestServiceClientFallback implements RequestClient {

    @Override
    public Integer countConfirmedRequestsByEventId(Long eventId) {
        log.warn("Request service unavailable, returning 0 confirmed requests for eventId={}", eventId);
        return 0;
    }

    @Override
    public List<ConfirmedRequestsCountDto> countConfirmedRequestsByEventIds(List<Long> eventIds) {
        log.warn("Request service unavailable, returning empty list for eventIds={}", eventIds);
        return Collections.emptyList();
    }

    @Override
    public List<RequestDto> getRequestsByEventId(Long eventId) {
        log.warn("Request service unavailable, returning empty list for eventId={}", eventId);
        return Collections.emptyList();
    }

    @Override
    public List<RequestDto> findAllByRequesterId(Long requesterId) {
        log.warn("Request service unavailable, returning empty list for requesterId={}", requesterId);
        return Collections.emptyList();
    }

    @Override
    public Boolean existsByRequesterIdAndEventId(Long requesterId, Long eventId) {
        log.warn("Request service unavailable, returning false for requesterId={}, eventId={}", requesterId, eventId);
        return false; // Лучше вернуть false чем null
    }

    @Override
    public Integer countByEventId(Long eventId) {
        log.warn("Request service unavailable, returning 0 for eventId={}", eventId);
        return 0;
    }

    @Override
    public List<RequestDto> findAllByIdInAndEventId(List<Long> ids, Long eventId) {
        log.warn("Request service unavailable, returning empty list for ids={}, eventId={}", ids, eventId);
        return Collections.emptyList();
    }

    @Override
    public void updateRequestsStatus(RequestStatusUpdateDto updateDto) {
        log.warn("Request service unavailable, cannot update request statuses for requestIds={}, status={}",
                updateDto != null ? updateDto.getRequestIds() : "null",
                updateDto != null ? updateDto.getStatus() : "null");

         throw new NotFoundException("Request service is unavailable");
    }
}
