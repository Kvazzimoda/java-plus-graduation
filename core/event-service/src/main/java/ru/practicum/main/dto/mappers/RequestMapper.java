package ru.practicum.main.dto.mappers;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.practicum.main.dto.response.request.ParticipationRequestDto;
import ru.practicum.main.dto.response.request.RequestDto;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RequestMapper {

    public static ParticipationRequestDto toParticipationRequestDto(RequestDto request) {
        return ParticipationRequestDto.builder()
                .id(request.getId())
                .created(request.getCreated())
                .event(request.getEventId())
                .requester(request.getRequesterId())
                .status(request.getStatus().name())
                .build();
    }

    public static List<ParticipationRequestDto> toDto(List<RequestDto> requests) {
        return requests.stream().map(RequestMapper::toParticipationRequestDto).toList();
    }
}
