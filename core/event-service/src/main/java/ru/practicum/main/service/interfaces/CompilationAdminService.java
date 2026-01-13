package ru.practicum.main.service.interfaces;

import ru.practicum.main.dto.request.compilation.NewCompilationDto;
import ru.practicum.main.dto.request.compilation.UpdateCompilationRequest;
import ru.practicum.main.dto.response.compilation.CompilationDto;

public interface CompilationAdminService {

    CompilationDto add(NewCompilationDto newCompilation);

    void deleteById(Long compilationId);

    CompilationDto update(Long compilationId, UpdateCompilationRequest updatedCompilation);
}
