package ru.practicum.main.dto.response.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestDto {
    private Long id;
    private LocalDateTime created;
    private Long eventId;
    private Long requesterId;
    private RequestStatusDto status;

    public enum RequestStatusDto {
        PENDING, CONFIRMED, REJECTED, CANCELED
    }
}
