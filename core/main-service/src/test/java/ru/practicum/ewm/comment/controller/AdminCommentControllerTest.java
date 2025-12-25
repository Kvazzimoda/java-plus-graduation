package ru.practicum.ewm.comment.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.comment.dto.CommentDto;
import ru.practicum.ewm.comment.service.CommentAdminService;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.user.dto.UserShortDto;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminCommentController.class)
class AdminCommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CommentAdminService commentAdminService;

    @Test
    void deleteComment_Success() throws Exception {
        Long commentId = 1L;

        doNothing().when(commentAdminService).deleteComment(commentId);

        mockMvc.perform(delete("/admin/comments/{commentId}", commentId))
                .andExpect(status().isNoContent());

        verify(commentAdminService).deleteComment(commentId);
    }

    @Test
    void deleteComment_NotFound_ShouldReturnNotFound() throws Exception {
        Long commentId = 999L;

        doThrow(new NotFoundException("Комментарий с id=" + commentId + " не найден"))
                .when(commentAdminService).deleteComment(commentId);

        mockMvc.perform(delete("/admin/comments/{commentId}", commentId))
                .andExpect(status().isNotFound());

        verify(commentAdminService).deleteComment(commentId);
    }

    @Test
    void getAllComments_Success() throws Exception {
        UserShortDto author1 = new UserShortDto();
        author1.setId(1L);
        author1.setName("Автор 1");

        UserShortDto author2 = new UserShortDto();
        author2.setId(2L);
        author2.setName("Автор 2");

        CommentDto commentDto1 = new CommentDto();
        commentDto1.setId(1L);
        commentDto1.setText("Первый комментарий");
        commentDto1.setAuthor(author1);
        commentDto1.setEventId(10L);
        commentDto1.setCreatedOn(LocalDateTime.now());

        CommentDto commentDto2 = new CommentDto();
        commentDto2.setId(2L);
        commentDto2.setText("Второй комментарий");
        commentDto2.setAuthor(author2);
        commentDto2.setEventId(20L);
        commentDto2.setCreatedOn(LocalDateTime.now());

        List<CommentDto> comments = List.of(commentDto1, commentDto2);

        when(commentAdminService.getAllComments(0, 10)).thenReturn(comments);

        mockMvc.perform(get("/admin/comments")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].text").value("Первый комментарий"))
                .andExpect(jsonPath("$[1].id").value(2L))
                .andExpect(jsonPath("$[1].text").value("Второй комментарий"));

        verify(commentAdminService).getAllComments(0, 10);
    }

    @Test
    void getAllComments_WithDefaultParameters() throws Exception {
        when(commentAdminService.getAllComments(0, 10)).thenReturn(List.of());

        mockMvc.perform(get("/admin/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(commentAdminService).getAllComments(0, 10);
    }

    @Test
    void getComment_Success() throws Exception {
        Long commentId = 1L;

        UserShortDto author = new UserShortDto();
        author.setId(1L);
        author.setName("Тестовый автор");

        CommentDto commentDto = new CommentDto();
        commentDto.setId(commentId);
        commentDto.setText("Тестовый комментарий");
        commentDto.setAuthor(author);
        commentDto.setEventId(10L);
        commentDto.setCreatedOn(LocalDateTime.now());

        when(commentAdminService.getComment(commentId)).thenReturn(commentDto);

        mockMvc.perform(get("/admin/comments/{commentId}", commentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(commentId))
                .andExpect(jsonPath("$.text").value("Тестовый комментарий"))
                .andExpect(jsonPath("$.author.id").value(1L))
                .andExpect(jsonPath("$.author.name").value("Тестовый автор"))
                .andExpect(jsonPath("$.eventId").value(10L));

        verify(commentAdminService).getComment(commentId);
    }
}
