package ru.practicum.main.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.dto.mappers.CategoryMapper;
import ru.practicum.main.dto.request.category.NewCategoryDto;
import ru.practicum.main.dto.response.category.CategoryDto;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.model.Category;
import ru.practicum.main.repository.CategoryRepository;
import ru.practicum.main.service.interfaces.CategoryService;


import java.util.List;

import static ru.practicum.main.dto.mappers.CategoryMapper.toEntity;
import static ru.practicum.main.dto.mappers.CategoryMapper.toDto;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImp implements CategoryService {
    private final CategoryRepository repository;

    //Получение категорий
    @Override
    public List<CategoryDto> getCategories(Pageable pageable) {
        List<Category> categories = repository.findAll(pageable).stream().toList();
        return categories.stream()
                .map(CategoryMapper::toDto)
                .toList();
    }

    //Получение категории
    @Override
    public CategoryDto getCategory(Long catId) {
        return toDto(repository.findById(catId).orElseThrow(() -> new NotFoundException("Category with id=" + catId + " was not found")));
    }

    //Добавление
    @Override
    @Transactional
    public CategoryDto addCategory(NewCategoryDto newCategoryDto) {
        log.info("Добавление новой категории " + newCategoryDto);
        Category category = toEntity(newCategoryDto);
        Category savedCategory = repository.save(category);
        log.info("Категория добавлена: {}", savedCategory);
        return toDto(savedCategory);
    }

    //Удаление
    @Override
    @Transactional
    public void deleteCategory(Long catId) {
        log.info("Попытка удаления категории по ID: {}", catId);
        if (!repository.existsById(catId)) {
            throw new NotFoundException("Category with id=" + catId + " was not found");
        }
        repository.deleteById(catId);
        log.info("Категория с ID: {} удалена", catId);
    }

    //Обновление
    @Override
    @Transactional
    public CategoryDto updateCategory(NewCategoryDto newCategoryDto, Long catId) {
        log.info("Попытка обновления категории с ID " + catId);
        Category existingCategory = repository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Category with id=" + catId + " was not found"));

        existingCategory.setName(newCategoryDto.getName());
        Category updatedCategory = repository.save(existingCategory);
        log.info("Категория обновлена: {}", updatedCategory);
        return toDto(updatedCategory);
    }
}
