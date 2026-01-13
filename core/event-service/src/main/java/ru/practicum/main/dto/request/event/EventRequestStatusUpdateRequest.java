package ru.practicum.main.dto.request.event;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.main.dto.response.request.RequestDto;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRequestStatusUpdateRequest {

    @NotEmpty(message = "Список идентификаторов запросов не может быть пустым")
    private Set<Long> requestIds;

    @NotNull(message = "Статус не может быть null")
    private RequestDto.RequestStatusDto status;
}
