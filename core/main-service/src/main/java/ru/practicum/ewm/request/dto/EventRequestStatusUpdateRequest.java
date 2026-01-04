package ru.practicum.ewm.request.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class EventRequestStatusUpdateRequest {

    @NotEmpty(message = "Список ID запросов не может быть пустым")
    private List<Long> requestIds;

    private String status; // CONFIRMED, REJECTED
}