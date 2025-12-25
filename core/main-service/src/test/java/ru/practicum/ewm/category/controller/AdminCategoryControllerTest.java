package ru.practicum.ewm.category.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.category.dto.CategoryDto;
import ru.practicum.ewm.category.dto.NewCategoryDto;
import ru.practicum.ewm.category.service.CategoryService;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminCategoryController.class)
class AdminCategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CategoryService categoryService;

    @Test
    void createCategory_Success() throws Exception {
        NewCategoryDto newCategoryDto = new NewCategoryDto();
        newCategoryDto.setName("Test Category");

        CategoryDto responseDto = new CategoryDto();
        responseDto.setId(1L);
        responseDto.setName("Test Category");

        when(categoryService.createCategory(any(NewCategoryDto.class))).thenReturn(responseDto);

        mockMvc.perform(post("/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newCategoryDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Test Category"));

        verify(categoryService).createCategory(any(NewCategoryDto.class));
    }

    @Test
    void createCategory_WithBlankName_ShouldReturnBadRequest() throws Exception {
        NewCategoryDto newCategoryDto = new NewCategoryDto();
        newCategoryDto.setName(""); // пустое имя

        mockMvc.perform(post("/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newCategoryDto)))
                .andExpect(status().isBadRequest());

        verify(categoryService, never()).createCategory(any());
    }

    @Test
    void updateCategory_Success() throws Exception {
        Long categoryId = 1L;
        CategoryDto updateDto = new CategoryDto();
        updateDto.setName("Updated Category");

        CategoryDto responseDto = new CategoryDto();
        responseDto.setId(categoryId);
        responseDto.setName("Updated Category");

        when(categoryService.updateCategory(eq(categoryId), any(CategoryDto.class))).thenReturn(responseDto);

        mockMvc.perform(patch("/admin/categories/{catId}", categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(categoryId))
                .andExpect(jsonPath("$.name").value("Updated Category"));

        verify(categoryService).updateCategory(eq(categoryId), any(CategoryDto.class));
    }

    @Test
    void updateCategory_NotFound_ShouldReturnNotFound() throws Exception {
        Long categoryId = 999L;
        CategoryDto updateDto = new CategoryDto();
        updateDto.setName("Updated Category");

        when(categoryService.updateCategory(eq(categoryId), any(CategoryDto.class)))
                .thenThrow(new NotFoundException("Категория с ID 999 не найдена"));

        mockMvc.perform(patch("/admin/categories/{catId}", categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteCategory_Success() throws Exception {
        Long categoryId = 1L;

        doNothing().when(categoryService).deleteCategory(categoryId);

        mockMvc.perform(delete("/admin/categories/{catId}", categoryId))
                .andExpect(status().isNoContent());

        verify(categoryService).deleteCategory(categoryId);
    }

    @Test
    void deleteCategory_WithLinkedEvents_ShouldReturnConflict() throws Exception {
        Long categoryId = 1L;

        doThrow(new ConflictException("Нельзя удалить категорию, связанную с событиями"))
                .when(categoryService).deleteCategory(categoryId);

        mockMvc.perform(delete("/admin/categories/{catId}", categoryId))
                .andExpect(status().isConflict());
    }
}
