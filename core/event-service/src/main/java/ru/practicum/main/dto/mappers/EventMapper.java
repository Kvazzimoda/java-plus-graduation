package ru.practicum.main.dto.mappers;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.practicum.main.dto.request.event.NewEventDto;
import ru.practicum.main.dto.response.event.EventFullDto;
import ru.practicum.main.dto.response.event.EventShortDto;
import ru.practicum.main.dto.response.user.UserDto;
import ru.practicum.main.model.Category;
import ru.practicum.main.model.Event;
import ru.practicum.main.model.LocationEntity;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EventMapper {
    public static EventShortDto toEventShortDto(Event event, UserDto userDto) {
        return EventShortDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(CategoryMapper.toDto(event.getCategory()))
                .confirmedRequests(event.getConfirmedRequests())
                .eventDate(event.getEventDate())
                .initiator(UserMapper.toUserShortDto(userDto))
                .paid(event.getPaid())
                .title(event.getTitle())
                // views будет установлен отдельно в сервисе
                .build();
    }

    public static Set<EventShortDto> toEventShortDto(Set<Event> events, Map<Long, UserDto> usersMap) {
        return events.stream()
                .map(event -> toEventShortDto(event, usersMap.get(event.getInitiatorId())))
                .collect(Collectors.toSet());
    }

    public static Event toEventFromNewEventDto(NewEventDto newEventDto, Long userId, Category category, LocationEntity savedLocationEntity) {
        return Event.builder()
                .annotation(newEventDto.getAnnotation())
                .category(category)
                .description(newEventDto.getDescription())
                .eventDate(newEventDto.getEventDate())
                .initiatorId(userId)
                .locationEntity(savedLocationEntity)
                .paid(newEventDto.getPaid() != null ? newEventDto.getPaid() : false)
                .participantLimit(newEventDto.getParticipantLimit() != null ?
                        newEventDto.getParticipantLimit() : 0)
                .requestModeration(newEventDto.getRequestModeration() != null ?
                        newEventDto.getRequestModeration() : true)
                .title(newEventDto.getTitle())
                .state(Event.EventState.PENDING)
                .confirmedRequests(0)
                .build();
    }

    public static EventFullDto toEventFullDto(Event event, UserDto userDto) {
        return EventFullDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(CategoryMapper.toDto(event.getCategory()))
                .confirmedRequests(event.getConfirmedRequests())
                .createdOn(event.getCreatedOn())
                .description(event.getDescription())
                .eventDate(event.getEventDate())
                .initiator(UserMapper.toUserShortDto(userDto))
                .location(LocationMapper.toLocationDto(event.getLocationEntity()))
                .paid(event.getPaid())
                .participantLimit(event.getParticipantLimit())
                .publishedOn(event.getPublishedOn())
                .requestModeration(event.getRequestModeration())
                .state(event.getState())
                .title(event.getTitle())
                // views устанавливается отдельно в сервисе
                .build();
    }

}
