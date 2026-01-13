package ru.practicum.main.dto.mappers;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.practicum.main.dto.request.compilation.NewCompilationDto;
import ru.practicum.main.dto.response.compilation.CompilationDto;
import ru.practicum.main.dto.response.event.EventShortDto;
import ru.practicum.main.dto.response.user.UserDto;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.model.Compilation;
import ru.practicum.main.model.Event;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CompilationMapper {

    public static Compilation toEntity(NewCompilationDto newCompilation, Set<Event> events) {
        return Compilation
                .builder()
                .title(newCompilation.getTitle())
                .events(events)
                .pinned(newCompilation.getPinned())
                .build();
    }

    public static CompilationDto toDto(Compilation compilation, Map<Long, UserDto> usersMap) {
        Set<EventShortDto> eventDtos = compilation.getEvents().stream()
                .map(event -> {
                    UserDto userDto = usersMap.get(event.getInitiatorId());
                    if (userDto == null) {
                        throw new NotFoundException("Пользователь не найден");
                    }
                    return EventMapper.toEventShortDto(event, userDto);
                })
                .collect(Collectors.toSet());

        return CompilationDto
                .builder()
                .events(eventDtos)
                .id(compilation.getId())
                .pinned(compilation.getPinned())
                .title(compilation.getTitle())
                .build();
    }

    public static List<CompilationDto> toDto(List<Compilation> compilations, Map<Long, UserDto> usersMap) {
        return compilations.stream()
                .map(compilation -> toDto(compilation, usersMap))
                .collect(Collectors.toList());
    }
}
