package ru.practicum.main.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.client.user.UserClient;
import ru.practicum.main.dto.request.compilation.NewCompilationDto;
import ru.practicum.main.dto.request.compilation.UpdateCompilationRequest;
import ru.practicum.main.dto.response.compilation.CompilationDto;
import ru.practicum.main.dto.response.user.UserDto;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.model.Compilation;
import ru.practicum.main.model.Event;
import ru.practicum.main.repository.CompilationRepository;
import ru.practicum.main.repository.EventRepository;
import ru.practicum.main.service.interfaces.CompilationAdminService;
import ru.practicum.main.util.Updater;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static ru.practicum.main.dto.mappers.CompilationMapper.toDto;
import static ru.practicum.main.dto.mappers.CompilationMapper.toEntity;


@Service
@Slf4j
@RequiredArgsConstructor
public class CompilationAdminServiceImpl implements CompilationAdminService {
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

    private Compilation getById(Long compilationId) {
        return compilationRepository.findById(compilationId).orElseThrow(() ->
                new NotFoundException("подборка с id " + compilationId + " не найдена"));
    }

    private Map<Long, UserDto> getUsersForEvents(Set<Event> events) {
        if (events == null || events.isEmpty()) {
            return new HashMap<>();
        }

        // Получаем уникальные ID пользователей-инициаторов
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
