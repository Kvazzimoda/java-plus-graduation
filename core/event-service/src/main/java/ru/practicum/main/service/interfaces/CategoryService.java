package ru.practicum.main.service.interfaces;

import org.springframework.data.domain.Pageable;
import ru.practicum.main.dto.request.category.NewCategoryDto;
import ru.practicum.main.dto.response.category.CategoryDto;


import java.util.List;

public interface CategoryService {
    List<CategoryDto> getCategories(Pageable pageable);

    CategoryDto getCategory(Long catId);

    CategoryDto addCategory(NewCategoryDto newCategoryDto);

    void deleteCategory(Long catId);

    CategoryDto updateCategory(NewCategoryDto newCategoryDto, Long catId);
}
