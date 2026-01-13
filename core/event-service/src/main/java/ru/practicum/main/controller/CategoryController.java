package ru.practicum.main.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.main.dto.request.category.NewCategoryDto;
import ru.practicum.main.dto.response.category.CategoryDto;
import ru.practicum.main.service.interfaces.CategoryService;


import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@Validated
public class CategoryController {
    private final CategoryService service;

    //Публичные методы
    //Получение категорий
    @GetMapping("/categories")
    public List<CategoryDto> getCategories(@PositiveOrZero @RequestParam(name = "from", defaultValue = "0") Integer from,
                                           @Positive @RequestParam(name = "size", defaultValue = "10") Integer size) {
        Pageable pageable = PageRequest.of(from, size);
        return service.getCategories(pageable);
    }

    //Получение категории
    @GetMapping("/categories/{catId}")
    public CategoryDto getCategory(@PathVariable @Positive Long catId) {
        return service.getCategory(catId);
    }

    //Методы администрирования
    //Добавление категории
    @PostMapping("/admin/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDto addCategory(@Valid @RequestBody NewCategoryDto newCategoryDto) {
        return service.addCategory(newCategoryDto);
    }

    //Удаление категории
    @DeleteMapping("/admin/categories/{catId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable @Positive Long catId) {
        service.deleteCategory(catId);
    }

    //Обновление категории
    @PatchMapping("/admin/categories/{catId}")
    public CategoryDto updateCategory(@Valid @RequestBody NewCategoryDto newCategoryDto,
                                      @PathVariable @Positive Long catId) {
        return service.updateCategory(newCategoryDto, catId);
    }

}
