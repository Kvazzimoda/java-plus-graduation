package ru.practicum.ewm.compilation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.compilation.dto.CompilationDto;
import ru.practicum.ewm.compilation.service.CompilationService;
import ru.practicum.ewm.exception.NotFoundException;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PublicCompilationController.class)
class PublicCompilationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CompilationService compilationService;

    @Test
    void getCompilations_WithPinnedTrue_Success() throws Exception {
        CompilationDto compilation = new CompilationDto();
        compilation.setId(1L);
        compilation.setTitle("Pinned Compilation");
        compilation.setPinned(true);

        List<CompilationDto> compilations = List.of(compilation);

        when(compilationService.getCompilations(true, 0, 10)).thenReturn(compilations);

        mockMvc.perform(get("/compilations")
                        .param("pinned", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].pinned").value(true));

        verify(compilationService).getCompilations(true, 0, 10);
    }

    @Test
    void getCompilations_WithCustomParams_Success() throws Exception {
        CompilationDto compilation = new CompilationDto();
        compilation.setId(1L);
        compilation.setTitle("Test Compilation");

        List<CompilationDto> compilations = List.of(compilation);

        when(compilationService.getCompilations(false, 5, 20)).thenReturn(compilations);

        mockMvc.perform(get("/compilations")
                        .param("pinned", "false")
                        .param("from", "5")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1L));

        verify(compilationService).getCompilations(false, 5, 20);
    }

    @Test
    void getCompilations_EmptyList_Success() throws Exception {
        when(compilationService.getCompilations(null, 0, 10)).thenReturn(List.of());

        mockMvc.perform(get("/compilations"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(0));

        verify(compilationService).getCompilations(null, 0, 10);
    }

    @Test
    void getCompilationById_Success() throws Exception {
        Long compId = 1L;
        CompilationDto compilationDto = new CompilationDto();
        compilationDto.setId(compId);
        compilationDto.setTitle("Test Compilation");
        compilationDto.setPinned(true);

        when(compilationService.getCompilationById(compId)).thenReturn(compilationDto);

        mockMvc.perform(get("/compilations/{compId}", compId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(compId))
                .andExpect(jsonPath("$.title").value("Test Compilation"))
                .andExpect(jsonPath("$.pinned").value(true));

        verify(compilationService).getCompilationById(compId);
    }

    @Test
    void getCompilationById_NotFound_ShouldReturnNotFound() throws Exception {
        Long compId = 999L;

        when(compilationService.getCompilationById(compId))
                .thenThrow(new NotFoundException("Подборка с ID 999 не найдена"));

        mockMvc.perform(get("/compilations/{compId}", compId))
                .andExpect(status().isNotFound());

        verify(compilationService).getCompilationById(compId);
    }

    @Test
    void getCompilations_WithLargeFromValue_Success() throws Exception {
        CompilationDto compilation = new CompilationDto();
        compilation.setId(1L);

        when(compilationService.getCompilations(null, 100, 10)).thenReturn(List.of(compilation));

        mockMvc.perform(get("/compilations")
                        .param("from", "100")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(compilationService).getCompilations(null, 100, 10);
    }
}
