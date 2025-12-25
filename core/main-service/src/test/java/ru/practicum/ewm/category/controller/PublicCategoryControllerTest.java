package ru.practicum.ewm.category.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.category.dto.CategoryDto;
import ru.practicum.ewm.category.service.CategoryService;
import ru.practicum.ewm.exception.NotFoundException;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PublicCategoryController.class)
class PublicCategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CategoryService categoryService;

    @Test
    void getCategories_WithDefaultParams_Success() throws Exception {
        CategoryDto category1 = new CategoryDto();
        category1.setId(1L);
        category1.setName("Category 1");

        CategoryDto category2 = new CategoryDto();
        category2.setId(2L);
        category2.setName("Category 2");

        List<CategoryDto> categories = List.of(category1, category2);

        when(categoryService.getCategories(0, 10)).thenReturn(categories);

        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("Category 1"))
                .andExpect(jsonPath("$[1].id").value(2L))
                .andExpect(jsonPath("$[1].name").value("Category 2"));

        verify(categoryService).getCategories(0, 10);
    }

    @Test
    void getCategories_WithCustomParams_Success() throws Exception {
        CategoryDto category = new CategoryDto();
        category.setId(1L);
        category.setName("Test Category");

        List<CategoryDto> categories = List.of(category);

        when(categoryService.getCategories(5, 20)).thenReturn(categories);

        mockMvc.perform(get("/categories")
                        .param("from", "5")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("Test Category"));

        verify(categoryService).getCategories(5, 20);
    }

    @Test
    void getCategoryById_Success() throws Exception {
        Long categoryId = 1L;
        CategoryDto categoryDto = new CategoryDto();
        categoryDto.setId(categoryId);
        categoryDto.setName("Test Category");

        when(categoryService.getCategoryDto(categoryId)).thenReturn(categoryDto);

        mockMvc.perform(get("/categories/{catId}", categoryId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(categoryId))
                .andExpect(jsonPath("$.name").value("Test Category"));

        verify(categoryService).getCategoryDto(categoryId);
    }

    @Test
    void getCategoryById_NotFound_ShouldReturnNotFound() throws Exception {
        Long categoryId = 999L;

        when(categoryService.getCategoryDto(categoryId))
                .thenThrow(new NotFoundException("Категория с ID 999 не найдена"));

        mockMvc.perform(get("/categories/{catId}", categoryId))
                .andExpect(status().isNotFound());

        verify(categoryService).getCategoryDto(categoryId);
    }

    @Test
    void getCategories_EmptyList_Success() throws Exception {
        when(categoryService.getCategories(0, 10)).thenReturn(List.of());

        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(0));

        verify(categoryService).getCategories(0, 10);
    }
}
