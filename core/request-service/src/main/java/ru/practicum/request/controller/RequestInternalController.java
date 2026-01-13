package ru.practicum.request.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.request.dto.request.request.RequestStatusUpdateDto;
import ru.practicum.request.dto.response.request.ConfirmedRequestsCountDto;
import ru.practicum.request.dto.response.request.RequestDto;
import ru.practicum.request.service.interfaces.RequestInternalService;

import java.util.List;

@RestController
@RequestMapping("/internal/requests")
@RequiredArgsConstructor
public class RequestInternalController {
    private final RequestInternalService requestInternalService;

    @GetMapping("/count/confirmed")
    public Integer countConfirmedRequestsByEventId(@RequestParam Long eventId) {
        return requestInternalService.countConfirmedRequestsByEventId(eventId);
    }

    @GetMapping("/count/confirmed/batch")
    public List<ConfirmedRequestsCountDto> countConfirmedRequestsByEventIds(@RequestParam List<Long> eventIds) {
        return requestInternalService.countConfirmedRequestsByEventIds(eventIds);
    }

    @GetMapping
    public List<RequestDto> getRequestsByEventId(@RequestParam Long eventId) {
        return requestInternalService.getRequestsByEventId(eventId);
    }

    @GetMapping("/requester")
    public List<RequestDto> findAllByRequesterId(@RequestParam Long requesterId) {
        return requestInternalService.findAllByRequesterId(requesterId);
    }

    @GetMapping("/exists")
    public Boolean existsByRequesterIdAndEventId(@RequestParam Long requesterId,
                                                 @RequestParam Long eventId) {
        return requestInternalService.existsByRequesterIdAndEventId(requesterId, eventId);
    }

    @GetMapping("/count/by-event")
    public Integer countByEventId(@RequestParam Long eventId) {
        return requestInternalService.countByEventId(eventId);
    }

    @GetMapping("/by-ids-and-event")
    public List<RequestDto> findAllByIdInAndEventId(@RequestParam List<Long> ids,
                                                    @RequestParam Long eventId) {
        return requestInternalService.findAllByIdInAndEventId(ids, eventId);
    }

    @PostMapping("/status")
    public void updateRequestsStatus(@RequestBody RequestStatusUpdateDto updateDto) {
        requestInternalService.updateRequestsStatus(updateDto);
    }
}
