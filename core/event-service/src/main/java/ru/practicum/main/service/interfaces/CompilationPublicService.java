package ru.practicum.main.service.interfaces;

import org.springframework.data.domain.Pageable;
import ru.practicum.main.dto.response.compilation.CompilationDto;

import java.util.List;

public interface CompilationPublicService {
    CompilationDto findById(Long compilationId);

    List<CompilationDto> findAllByFilters(Boolean pinned, Pageable pageable);


}
