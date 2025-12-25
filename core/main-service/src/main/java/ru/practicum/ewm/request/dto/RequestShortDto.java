package ru.practicum.ewm.request.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RequestShortDto {
    private Long id;
    private LocalDateTime created;
    private Long requesterId;
    private String status;
}