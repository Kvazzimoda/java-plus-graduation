package ru.practicum.server.controller;

import ru.practicum.stats.dto.dto.ViewStatsDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.server.service.EndpointHitService;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
public class ViewStatsController {
    private final EndpointHitService endpointHitService;

    @GetMapping
    public List<ViewStatsDto> findStats(@RequestParam String start,
                                        @RequestParam String end,
                                        @RequestParam(required = false) List<String> uris,
                                        @RequestParam(defaultValue = "false") boolean unique) {

        // Декодируем и парсим вручную
        LocalDateTime startDate = parseDateTime(URLDecoder.decode(start, StandardCharsets.UTF_8));
        LocalDateTime endDate = parseDateTime(URLDecoder.decode(end, StandardCharsets.UTF_8));

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Начало не должно быть позже конца");
        }

        return endpointHitService.getStats(startDate, endDate, uris, unique);
    }

    private LocalDateTime parseDateTime(String dateTimeStr) {
        try {
            return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format. Expected: yyyy-MM-dd HH:mm:ss", e);
        }
    }
}
