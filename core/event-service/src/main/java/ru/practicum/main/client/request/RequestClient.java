package ru.practicum.main.client.request;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.main.dto.response.request.ConfirmedRequestsCountDto;
import ru.practicum.main.dto.response.request.RequestDto;
import ru.practicum.main.dto.response.request.RequestStatusUpdateDto;

import java.util.List;

@FeignClient(name = "request-service", path = "/internal/requests", fallback = RequestServiceClientFallback.class)
public interface RequestClient {

    @GetMapping("/count/confirmed")
    Integer countConfirmedRequestsByEventId(@RequestParam("eventId") Long eventId);

    @GetMapping("/count/confirmed/batch")
    List<ConfirmedRequestsCountDto> countConfirmedRequestsByEventIds(@RequestParam("eventIds") List<Long> eventIds);

    @GetMapping
    List<RequestDto> getRequestsByEventId(@RequestParam("eventId") Long eventId);

    @GetMapping("/requester")
    List<RequestDto> findAllByRequesterId(@RequestParam Long requesterId);

    @GetMapping("/exists")
    Boolean existsByRequesterIdAndEventId(@RequestParam Long requesterId,
                                          @RequestParam Long eventId);

    @GetMapping("/count/by-event")
    Integer countByEventId(@RequestParam Long eventId);

    @GetMapping("/by-ids-and-event")
    List<RequestDto> findAllByIdInAndEventId(@RequestParam List<Long> ids,
                                             @RequestParam Long eventId);

    @PostMapping("/status")
    void updateRequestsStatus(@RequestBody RequestStatusUpdateDto updateDto);
}
