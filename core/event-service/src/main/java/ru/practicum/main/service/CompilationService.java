package ru.practicum.main.service;

import org.springframework.data.domain.Pageable;
import ru.practicum.main.dto.request.compilation.NewCompilationDto;
import ru.practicum.main.dto.request.compilation.UpdateCompilationRequest;
import ru.practicum.main.dto.response.compilation.CompilationDto;

import java.util.List;

public interface CompilationService {

    CompilationDto add(NewCompilationDto newCompilation);

    void deleteById(Long compilationId);

    CompilationDto update(Long compilationId, UpdateCompilationRequest updatedCompilation);

    CompilationDto findById(Long compilationId);

    List<CompilationDto> findAllByFilters(Boolean pinned, Pageable pageable);
}
