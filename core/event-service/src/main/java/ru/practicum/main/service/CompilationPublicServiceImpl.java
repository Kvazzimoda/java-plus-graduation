package ru.practicum.main.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.practicum.main.client.user.UserClient;
import ru.practicum.main.dto.response.compilation.CompilationDto;
import ru.practicum.main.dto.response.user.UserDto;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.model.Compilation;
import ru.practicum.main.model.Event;
import ru.practicum.main.repository.CompilationRepository;
import ru.practicum.main.service.interfaces.CompilationPublicService;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static ru.practicum.main.dto.mappers.CompilationMapper.toDto;

@Service
@Slf4j
@RequiredArgsConstructor
public class CompilationPublicServiceImpl implements CompilationPublicService {
    private final CompilationRepository compilationRepository;
    private final UserClient userClient;

    @Override
    public List<CompilationDto> findAllByFilters(Boolean pinned, Pageable pageable) {
        log.info("запрос на поиск по фильтрам");
        Page<Compilation> compilations;
        if (pinned == null) {
            compilations = compilationRepository.findAll(pageable);
        } else {
            compilations = compilationRepository.findAllByPinned(pinned, pageable);
        }
        if (compilations.isEmpty()) {
            log.debug("по заданным фильтрам ничего не найдено");
            return Collections.emptyList();
        }

        List<Compilation> compilationList = compilations.getContent();
        Map<Long, UserDto> usersMap = getUsersForAllCompilations(compilationList);

        return compilationList.stream()
                .map(compilation -> toDto(compilation, usersMap))
                .collect(Collectors.toList());
    }

    @Override
    public CompilationDto findById(Long compilationId) {
        log.debug("поиск подборки с id {}", compilationId);

        Compilation compilation = getById(compilationId);
        Map<Long, UserDto> usersMap = getUsersForEvents(compilation.getEvents());

        return toDto(compilation, usersMap);
    }

    private Compilation getById(Long compilationId) {
        return compilationRepository.findById(compilationId).orElseThrow(() ->
                new NotFoundException("подборка с id " + compilationId + " не найдена"));
    }

    private Map<Long, UserDto> getUsersForAllCompilations(List<Compilation> compilations) {
        // Собираем все уникальные ID пользователей-инициаторов из всех событий всех подборок
        Set<Long> initiatorIds = compilations.stream()
                .flatMap(compilation -> compilation.getEvents().stream())
                .map(event -> event.getInitiatorId())
                .collect(Collectors.toSet());

        return getUsersByIds(new ArrayList<>(initiatorIds));
    }

    private Map<Long, UserDto> getUsersForEvents(Set<Event> events) {
        if (events == null || events.isEmpty()) {
            return new HashMap<>();
        }

        Set<Long> initiatorIds = events.stream()
                .map(Event::getInitiatorId)
                .collect(Collectors.toSet());

        return getUsersByIds(new ArrayList<>(initiatorIds));
    }

    private Map<Long, UserDto> getUsersByIds(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return new HashMap<>();
        }

        try {
            List<UserDto> users = userClient.getUsers(userIds);
            return users.stream()
                    .collect(Collectors.toMap(UserDto::getId, Function.identity()));
        } catch (Exception e) {
            log.error("Failed to get users from user-service: {}", e.getMessage());
            // Возвращаем пустую мапу, чтобы не падать полностью
            return new HashMap<>();
        }
    }
}
