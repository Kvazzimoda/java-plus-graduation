package ru.practicum.server.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.stats.dto.dto.EndpointHitDto;
import lombok.RequiredArgsConstructor;
import ru.practicum.server.service.EndpointHitService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/hit")
public class EndpointHitController {
    private final EndpointHitService endpointHitService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EndpointHitDto saveEndpointHit(@RequestBody EndpointHitDto endpointHitDto) {
        return endpointHitService.save(endpointHitDto);
    }

}