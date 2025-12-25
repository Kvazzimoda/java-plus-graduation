package ru.practicum.ewm.category.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.category.dto.CategoryDto;
import ru.practicum.ewm.category.dto.NewCategoryDto;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.category.repository.CategoryRepository;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.EwmMapper;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;
    private final EwmMapper mapper;

    @Transactional
    public CategoryDto createCategory(NewCategoryDto request) {
        log.info("Создание категории с названием: {}", request.getName());

        if (categoryRepository.existsByName(request.getName())) {
            throw new ConflictException("Категория с названием " + request.getName() + " уже существует");
        }

        Category category = mapper.toCategory(request);

        try {
            Category savedCategory = categoryRepository.save(category);
            log.info("Категория создана с ID: {}", savedCategory.getId());
            return mapper.toCategoryDto(savedCategory);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Категория с названием " + request.getName() + " уже существует");
        }
    }

    @Transactional
    public CategoryDto updateCategory(Long catId, CategoryDto request) {
        log.info("Обновление категории с ID: {}", catId);

        Category category = getCategoryById(catId);

        if (!category.getName().equals(request.getName()) &&
                categoryRepository.existsByName(request.getName())) {
            throw new ConflictException("Категория с названием " + request.getName() + " уже существует");
        }

        category.setName(request.getName());

        try {
            Category updatedCategory = categoryRepository.save(category);
            log.info("Категория с ID {} обновлена", catId);
            return mapper.toCategoryDto(updatedCategory);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Категория с названием " + request.getName() + " уже существует");
        }
    }

    @Transactional
    public void deleteCategory(Long catId) {
        log.info("Удаление категории с ID: {}", catId);

        if (eventRepository.existsByCategoryId(catId)) {
            throw new ConflictException("Нельзя удалить категорию, связанную с событиями");
        }

        categoryRepository.deleteById(catId);
        log.info("Категория с ID {} удалена", catId);
    }

    public List<CategoryDto> getCategories(int from, int size) {
        log.info("Получение категорий: from={}, size={}", from, size);

        Pageable pageable = PageRequest.of(from / size, size);

        return categoryRepository.findAll(pageable)
                .stream()
                .map(mapper::toCategoryDto)
                .collect(Collectors.toList());
    }

    public CategoryDto getCategoryDto(Long catId) {
        Category category = getCategoryById(catId);
        return mapper.toCategoryDto(category);
    }

    public Category getCategoryById(Long catId) {
        return categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Категория с ID " + catId + " не найдена"));
    }
}
