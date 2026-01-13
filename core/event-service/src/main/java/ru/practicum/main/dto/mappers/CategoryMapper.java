package ru.practicum.main.dto.mappers;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.practicum.main.dto.request.category.NewCategoryDto;
import ru.practicum.main.dto.response.category.CategoryDto;
import ru.practicum.main.model.Category;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CategoryMapper {
    public static Category toEntity(NewCategoryDto newCategoryDto) {
        return Category.builder()
                .id(0L)
                .name(newCategoryDto.getName())
                .build();
    }

    public static CategoryDto toDto(Category category) {
        return CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .build();
    }
}
