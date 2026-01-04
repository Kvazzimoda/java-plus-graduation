package ru.practicum.ewm.compilation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.compilation.dto.CompilationDto;
import ru.practicum.ewm.compilation.dto.NewCompilationDto;
import ru.practicum.ewm.compilation.dto.UpdateCompilationRequest;
import ru.practicum.ewm.compilation.model.Compilation;
import ru.practicum.ewm.compilation.repository.CompilationRepository;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.EwmMapper;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class CompilationService {

    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final EwmMapper mapper;

    @Transactional
    public CompilationDto createCompilation(NewCompilationDto request) {
        log.info("Создание новой подборки: {}", request.getTitle());

        Set<Event> events = new HashSet<>();
        if (request.getEvents() != null && !request.getEvents().isEmpty()) {
            events = new HashSet<>(eventRepository.findAllById(request.getEvents()));
        }

        Compilation compilation = mapper.toCompilation(request);
        compilation.setEvents(events);

        Compilation savedCompilation = compilationRepository.save(compilation);

        log.info("Подборка создана с ID: {}", savedCompilation.getId());
        return mapper.toCompilationDto(savedCompilation);
    }

    @Transactional
    public CompilationDto updateCompilation(Long compId, UpdateCompilationRequest request) {
        log.info("Обновление подборки с ID: {}", compId);

        Compilation compilation = findCompilationById(compId);

        if (request.getTitle() != null) {
            compilation.setTitle(request.getTitle());
        }

        if (request.getPinned() != null) {
            compilation.setPinned(request.getPinned());
        }

        if (request.getEvents() != null) {
            Set<Event> events = new HashSet<>(eventRepository.findAllById(request.getEvents()));
            compilation.setEvents(events);
        }

        Compilation updatedCompilation = compilationRepository.save(compilation);

        log.info("Подборка с ID {} обновлена", compId);
        return mapper.toCompilationDto(updatedCompilation);
    }

    @Transactional
    public void deleteCompilation(Long compId) {
        log.info("Удаление подборки с ID: {}", compId);

        Compilation compilation = findCompilationById(compId);
        compilationRepository.delete(compilation);

        log.info("Подборка с ID {} удалена", compId);
    }

    public List<CompilationDto> getCompilations(Boolean pinned, int from, int size) {
        log.info("Получение подборок: pinned={}, from={}, size={}", pinned, from, size);

        Pageable pageable = PageRequest.of(from / size, size);
        return compilationRepository.findAllByPinned(pinned, pageable)
                .stream()
                .map(mapper::toCompilationDto)
                .collect(Collectors.toList());
    }

    public CompilationDto getCompilationById(Long compId) {
        log.info("Получение подборки по ID: {}", compId);

        Compilation compilation = findCompilationById(compId);
        return mapper.toCompilationDto(compilation);
    }

    private Compilation findCompilationById(Long compId) {
        return compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Подборка с ID " + compId + " не найдена"));
    }
}
