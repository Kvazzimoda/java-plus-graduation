package ru.practicum.ewm.category.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import ru.practicum.ewm.category.dto.CategoryDto;
import ru.practicum.ewm.category.dto.NewCategoryDto;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.category.repository.CategoryRepository;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.EwmMapper;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EwmMapper mapper;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void createCategory_Success() {
        NewCategoryDto newCategoryDto = new NewCategoryDto();
        newCategoryDto.setName("Test Category");

        Category category = new Category();
        category.setName("Test Category");

        Category savedCategory = new Category();
        savedCategory.setId(1L);
        savedCategory.setName("Test Category");

        CategoryDto expectedDto = new CategoryDto();
        expectedDto.setId(1L);
        expectedDto.setName("Test Category");

        when(categoryRepository.existsByName("Test Category")).thenReturn(false);
        when(mapper.toCategory(newCategoryDto)).thenReturn(category);
        when(categoryRepository.save(category)).thenReturn(savedCategory);
        when(mapper.toCategoryDto(savedCategory)).thenReturn(expectedDto);

        CategoryDto result = categoryService.createCategory(newCategoryDto);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test Category", result.getName());
        verify(categoryRepository).save(category);
    }

    @Test
    void createCategory_WithExistingName_ShouldThrowConflictException() {
        NewCategoryDto newCategoryDto = new NewCategoryDto();
        newCategoryDto.setName("Existing Category");

        when(categoryRepository.existsByName("Existing Category")).thenReturn(true);

        ConflictException exception = assertThrows(ConflictException.class,
                () -> categoryService.createCategory(newCategoryDto));

        assertEquals("Категория с названием Existing Category уже существует", exception.getMessage());
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void getCategoryById_Success() {
        Long categoryId = 1L;
        Category category = new Category();
        category.setId(categoryId);
        category.setName("Test Category");

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        Category result = categoryService.getCategoryById(categoryId);

        assertNotNull(result);
        assertEquals(categoryId, result.getId());
        assertEquals("Test Category", result.getName());
    }

    @Test
    void getCategoryById_NotFound_ShouldThrowNotFoundException() {
        Long categoryId = 999L;

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> categoryService.getCategoryById(categoryId));

        assertEquals("Категория с ID 999 не найдена", exception.getMessage());
    }

    @Test
    void getCategories_Success() {
        Category category1 = new Category();
        category1.setId(1L);
        category1.setName("Category 1");

        Category category2 = new Category();
        category2.setId(2L);
        category2.setName("Category 2");

        List<Category> categories = List.of(category1, category2);
        Page<Category> categoryPage = new PageImpl<>(categories);

        CategoryDto dto1 = new CategoryDto();
        dto1.setId(1L);
        dto1.setName("Category 1");

        CategoryDto dto2 = new CategoryDto();
        dto2.setId(2L);
        dto2.setName("Category 2");

        when(categoryRepository.findAll(any(Pageable.class))).thenReturn(categoryPage);
        when(mapper.toCategoryDto(category1)).thenReturn(dto1);
        when(mapper.toCategoryDto(category2)).thenReturn(dto2);

        List<CategoryDto> result = categoryService.getCategories(0, 10);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Category 1", result.get(0).getName());
        assertEquals("Category 2", result.get(1).getName());
    }

    @Test
    void deleteCategory_WithLinkedEvents_ShouldThrowConflictException() {
        Long categoryId = 1L;

        when(eventRepository.existsByCategoryId(categoryId)).thenReturn(true);

        ConflictException exception = assertThrows(ConflictException.class,
                () -> categoryService.deleteCategory(categoryId));

        assertEquals("Нельзя удалить категорию, связанную с событиями", exception.getMessage());
        verify(categoryRepository, never()).deleteById(any());
    }
}