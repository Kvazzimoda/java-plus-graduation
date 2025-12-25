package ru.practicum.ewm.comment.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.comment.dto.CommentShortDto;
import ru.practicum.ewm.comment.service.CommentPublicService;
import ru.practicum.ewm.exception.NotFoundException;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PublicCommentController.class)
class PublicCommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CommentPublicService commentPublicService;

    @Test
    void getEventComments_Success() throws Exception {
        Long eventId = 1L;

        CommentShortDto comment1 = CommentShortDto.builder()
                .id(1L)
                .text("Первый комментарий")
                .authorName("Автор 1")
                .createdOn(LocalDateTime.now())
                .build();

        CommentShortDto comment2 = CommentShortDto.builder()
                .id(2L)
                .text("Второй комментарий")
                .authorName("Автор 2")
                .createdOn(LocalDateTime.now())
                .build();

        List<CommentShortDto> comments = List.of(comment1, comment2);

        when(commentPublicService.getEventComments(eventId, 0, 10)).thenReturn(comments);

        mockMvc.perform(get("/events/{eventId}/comments", eventId)
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].text").value("Первый комментарий"))
                .andExpect(jsonPath("$[0].authorName").value("Автор 1"))
                .andExpect(jsonPath("$[1].id").value(2L))
                .andExpect(jsonPath("$[1].text").value("Второй комментарий"))
                .andExpect(jsonPath("$[1].authorName").value("Автор 2"));

        verify(commentPublicService).getEventComments(eventId, 0, 10);
    }

    @Test
    void getEventComments_WithDefaultParameters() throws Exception {
        Long eventId = 1L;

        when(commentPublicService.getEventComments(eventId, 0, 10)).thenReturn(List.of());

        mockMvc.perform(get("/events/{eventId}/comments", eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(commentPublicService).getEventComments(eventId, 0, 10);
    }

    @Test
    void getEventComments_WithCustomParameters() throws Exception {
        Long eventId = 1L;
        int from = 5;
        int size = 20;

        when(commentPublicService.getEventComments(eventId, from, size)).thenReturn(List.of());

        mockMvc.perform(get("/events/{eventId}/comments", eventId)
                        .param("from", String.valueOf(from))
                        .param("size", String.valueOf(size)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(commentPublicService).getEventComments(eventId, from, size);
    }

    @Test
    void getEventComments_EventNotFound_ShouldReturnNotFound() throws Exception {
        Long eventId = 999L;

        when(commentPublicService.getEventComments(eventId, 0, 10))
                .thenThrow(new NotFoundException("Событие с id=" + eventId + " не найдено"));

        mockMvc.perform(get("/events/{eventId}/comments", eventId))
                .andExpect(status().isNotFound());

        verify(commentPublicService).getEventComments(eventId, 0, 10);
    }
}
