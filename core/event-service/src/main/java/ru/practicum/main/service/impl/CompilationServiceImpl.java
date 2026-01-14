package ru.practicum.main.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.core.client.UserClient;
import ru.practicum.core.dto.UserDto;
import ru.practicum.main.dto.request.compilation.NewCompilationDto;
import ru.practicum.main.dto.request.compilation.UpdateCompilationRequest;
import ru.practicum.main.dto.response.compilation.CompilationDto;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.model.Compilation;
import ru.practicum.main.model.Event;
import ru.practicum.main.repository.CompilationRepository;
import ru.practicum.main.repository.EventRepository;
import ru.practicum.main.service.CompilationService;
import ru.practicum.main.util.Updater;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static ru.practicum.main.dto.mappers.CompilationMapper.toDto;
import static ru.practicum.main.dto.mappers.CompilationMapper.toEntity;


@Service
@Slf4j
@RequiredArgsConstructor
public class CompilationServiceImpl implements CompilationService {
    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final UserClient userClient;

    @Override
    @Transactional
    public CompilationDto add(NewCompilationDto newCompilation) {
        log.debug("добавление новой подборки{}", newCompilation);
        Set<Event> events = eventRepository.findAllByIdIn(newCompilation.getEvents());

        Map<Long, UserDto> usersMap = getUsersForEvents(events);

        Compilation compilation = toEntity(newCompilation, events);
        Compilation savedCompilation = compilationRepository.save(compilation);
        return toDto(savedCompilation, usersMap);
    }

    @Override
    @Transactional
    public void deleteById(Long compilationId) {
        log.debug("удаление подборки с id{}", compilationId);
        compilationRepository.deleteById(compilationId);
    }

    @Override
    @Transactional
    public CompilationDto update(Long compilationId, UpdateCompilationRequest updatedCompilation) {
        log.debug("обновление подборки с id{}", compilationId);
        Compilation oldCompilation = getById(compilationId);
        Updater.update(updatedCompilation.getEvents(), () -> oldCompilation.setEvents(eventRepository.findAllByIdIn(updatedCompilation.getEvents())));
        Updater.update(updatedCompilation.getTitle(), () -> oldCompilation.setTitle(updatedCompilation.getTitle()));
        Updater.update(updatedCompilation.getPinned(), () -> oldCompilation.setPinned(updatedCompilation.getPinned()));

        Compilation updated = compilationRepository.save(oldCompilation);
        Map<Long, UserDto> usersMap = getUsersForEvents(updated.getEvents());

        log.info("обновленная подборка{}", updatedCompilation);

        return toDto(updated, usersMap);
    }

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
                .map(Event::getInitiatorId)
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
