package ru.practicum.comment.dto.response.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventDto {
    private Long id;
    private Long initiatorId;
    private Integer participantLimit;
    private Boolean requestModeration;
    private EventState state;

    public enum EventState {
        PENDING, PUBLISHED, CANCELED
    }
}
