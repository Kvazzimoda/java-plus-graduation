package ru.practicum.ewm.compilation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.compilation.dto.CompilationDto;
import ru.practicum.ewm.compilation.dto.NewCompilationDto;
import ru.practicum.ewm.compilation.dto.UpdateCompilationRequest;
import ru.practicum.ewm.compilation.service.CompilationService;
import ru.practicum.ewm.exception.NotFoundException;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminCompilationController.class)
class AdminCompilationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CompilationService compilationService;

    @Test
    void createCompilation_Success() throws Exception {
        NewCompilationDto request = new NewCompilationDto();
        request.setTitle("Test Compilation");
        request.setPinned(true);
        request.setEvents(Set.of(1L, 2L));

        CompilationDto response = new CompilationDto();
        response.setId(1L);
        response.setTitle("Test Compilation");
        response.setPinned(true);

        when(compilationService.createCompilation(any(NewCompilationDto.class))).thenReturn(response);

        mockMvc.perform(post("/admin/compilations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Test Compilation"))
                .andExpect(jsonPath("$.pinned").value(true));

        verify(compilationService).createCompilation(any(NewCompilationDto.class));
    }

    @Test
    void createCompilation_WithoutTitle_ShouldReturnBadRequest() throws Exception {
        NewCompilationDto request = new NewCompilationDto();
        request.setPinned(true);

        mockMvc.perform(post("/admin/compilations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(compilationService, never()).createCompilation(any());
    }

    @Test
    void createCompilation_WithBlankTitle_ShouldReturnBadRequest() throws Exception {
        NewCompilationDto request = new NewCompilationDto();
        request.setTitle("");
        request.setPinned(true);

        mockMvc.perform(post("/admin/compilations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(compilationService, never()).createCompilation(any());
    }

    @Test
    void createCompilation_WithLongTitle_ShouldReturnBadRequest() throws Exception {
        NewCompilationDto request = new NewCompilationDto();
        request.setTitle("a".repeat(51));
        request.setPinned(true);

        mockMvc.perform(post("/admin/compilations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(compilationService, never()).createCompilation(any());
    }

    @Test
    void updateCompilation_Success() throws Exception {
        Long compId = 1L;
        UpdateCompilationRequest request = new UpdateCompilationRequest();
        request.setTitle("Updated Title");
        request.setPinned(false);

        CompilationDto response = new CompilationDto();
        response.setId(compId);
        response.setTitle("Updated Title");
        response.setPinned(false);

        when(compilationService.updateCompilation(eq(compId), any(UpdateCompilationRequest.class)))
                .thenReturn(response);

        mockMvc.perform(patch("/admin/compilations/{compId}", compId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(compId))
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.pinned").value(false));

        verify(compilationService).updateCompilation(eq(compId), any(UpdateCompilationRequest.class));
    }

    @Test
    void updateCompilation_NotFound_ShouldReturnNotFound() throws Exception {
        Long compId = 999L;
        UpdateCompilationRequest request = new UpdateCompilationRequest();
        request.setTitle("Updated Title");

        when(compilationService.updateCompilation(eq(compId), any(UpdateCompilationRequest.class)))
                .thenThrow(new NotFoundException("Подборка с ID 999 не найдена"));

        mockMvc.perform(patch("/admin/compilations/{compId}", compId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());

        verify(compilationService).updateCompilation(eq(compId), any(UpdateCompilationRequest.class));
    }

    @Test
    void updateCompilation_WithLongTitle_ShouldReturnBadRequest() throws Exception {
        Long compId = 1L;
        UpdateCompilationRequest request = new UpdateCompilationRequest();
        request.setTitle("a".repeat(51));

        mockMvc.perform(patch("/admin/compilations/{compId}", compId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(compilationService, never()).updateCompilation(any(), any());
    }

    @Test
    void updateCompilation_WithEmptyRequest_Success() throws Exception {
        Long compId = 1L;
        UpdateCompilationRequest request = new UpdateCompilationRequest();

        CompilationDto response = new CompilationDto();
        response.setId(compId);

        when(compilationService.updateCompilation(eq(compId), any(UpdateCompilationRequest.class)))
                .thenReturn(response);

        mockMvc.perform(patch("/admin/compilations/{compId}", compId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(compId));

        verify(compilationService).updateCompilation(eq(compId), any(UpdateCompilationRequest.class));
    }

    @Test
    void deleteCompilation_Success() throws Exception {
        Long compId = 1L;

        doNothing().when(compilationService).deleteCompilation(compId);

        mockMvc.perform(delete("/admin/compilations/{compId}", compId))
                .andExpect(status().isNoContent());

        verify(compilationService).deleteCompilation(compId);
    }

    @Test
    void deleteCompilation_NotFound_ShouldReturnNotFound() throws Exception {
        Long compId = 999L;

        doThrow(new NotFoundException("Подборка с ID 999 не найдена"))
                .when(compilationService).deleteCompilation(compId);

        mockMvc.perform(delete("/admin/compilations/{compId}", compId))
                .andExpect(status().isNotFound());

        verify(compilationService).deleteCompilation(compId);
    }

    @Test
    void createCompilation_WithoutPinned_Success() throws Exception {
        NewCompilationDto request = new NewCompilationDto();
        request.setTitle("Test Compilation");

        CompilationDto response = new CompilationDto();
        response.setId(1L);
        response.setTitle("Test Compilation");
        response.setPinned(false);

        when(compilationService.createCompilation(any(NewCompilationDto.class))).thenReturn(response);

        mockMvc.perform(post("/admin/compilations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Test Compilation"))
                .andExpect(jsonPath("$.pinned").value(false));

        verify(compilationService).createCompilation(any(NewCompilationDto.class));
    }

    @Test
    void createCompilation_WithoutEvents_Success() throws Exception {
        NewCompilationDto request = new NewCompilationDto();
        request.setTitle("Test Compilation");
        request.setPinned(true);

        CompilationDto response = new CompilationDto();
        response.setId(1L);
        response.setTitle("Test Compilation");
        response.setPinned(true);

        when(compilationService.createCompilation(any(NewCompilationDto.class))).thenReturn(response);

        mockMvc.perform(post("/admin/compilations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L));

        verify(compilationService).createCompilation(any(NewCompilationDto.class));
    }
}
