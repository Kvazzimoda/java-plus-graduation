package ru.practicum.main.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import ru.practicum.main.dto.response.compilation.CompilationDto;
import ru.practicum.main.service.interfaces.CompilationPublicService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/compilations")
public class CompilationPublicController {
    private final CompilationPublicService compilationPublicService;

    @GetMapping
    public List<CompilationDto> findAllByFilters(@RequestParam(required = false) Boolean pinned,
                                                 @RequestParam(defaultValue = "0") int from,
                                                 @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(from, size);
        return compilationPublicService.findAllByFilters(pinned, pageable);
    }

    @GetMapping("/{compId}")
    public CompilationDto findById(@PathVariable Long compId) {
        return compilationPublicService.findById(compId);
    }
}
