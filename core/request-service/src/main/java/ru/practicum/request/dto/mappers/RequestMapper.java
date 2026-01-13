package ru.practicum.request.dto.mappers;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.practicum.request.dto.response.request.ParticipationRequestDto;
import ru.practicum.request.dto.response.request.RequestDto;
import ru.practicum.request.model.Request;

import java.util.List;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RequestMapper {

    public static ParticipationRequestDto toParticipationRequestDto(Request request) {
        return ParticipationRequestDto.builder()
                .id(request.getId())
                .created(request.getCreated())
                .event(request.getEventId())
                .requester(request.getRequesterId())
                .status(request.getStatus().name())
                .build();
    }

    public static List<ParticipationRequestDto> toParticipationRequestsDto(List<Request> requests) {
        return requests.stream().map(RequestMapper::toParticipationRequestDto).toList();
    }

    public static RequestDto toDto(Request request) {
        return RequestDto.builder()
                .id(request.getId())
                .created(request.getCreated())
                .eventId(request.getEventId())
                .requesterId(request.getRequesterId())
                .status(request.getStatus())
                .build();
    }

    public static List<RequestDto> toDtoList(List<Request> requests) {
        return requests.stream()
                .map(RequestMapper::toDto)
                .collect(Collectors.toList());
    }
}
