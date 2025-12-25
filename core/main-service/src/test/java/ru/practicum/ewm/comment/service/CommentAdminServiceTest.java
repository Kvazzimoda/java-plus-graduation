package ru.practicum.ewm.comment.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import ru.practicum.ewm.comment.dto.CommentDto;
import ru.practicum.ewm.comment.model.Comment;
import ru.practicum.ewm.comment.repository.CommentRepository;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.EwmMapper;
import ru.practicum.ewm.user.dto.UserShortDto;
import ru.practicum.ewm.user.model.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentAdminServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private EwmMapper mapper;

    @InjectMocks
    private CommentAdminService commentAdminService;

    @Test
    void deleteComment_Success() {
        Long commentId = 1L;

        when(commentRepository.existsById(commentId)).thenReturn(true);

        commentAdminService.deleteComment(commentId);

        verify(commentRepository).existsById(commentId);
        verify(commentRepository).deleteById(commentId);
    }

    @Test
    void deleteComment_NotFound_ShouldThrowNotFoundException() {
        Long commentId = 999L;

        when(commentRepository.existsById(commentId)).thenReturn(false);

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> commentAdminService.deleteComment(commentId));

        assertEquals("Комментарий с id=" + commentId + " не найден", exception.getMessage());
        verify(commentRepository, never()).deleteById(any());
    }

    @Test
    void getAllComments_Success() {
        User author = new User();
        author.setId(1L);
        author.setName("Test Author");

        Event event = new Event();
        event.setId(1L);

        Comment comment1 = Comment.builder()
                .id(1L)
                .text("Первый комментарий")
                .author(author)
                .event(event)
                .createdOn(LocalDateTime.now())
                .build();

        Comment comment2 = Comment.builder()
                .id(2L)
                .text("Второй комментарий")
                .author(author)
                .event(event)
                .createdOn(LocalDateTime.now())
                .build();

        List<Comment> comments = List.of(comment1, comment2);
        Page<Comment> commentPage = new PageImpl<>(comments);

        UserShortDto authorDto = new UserShortDto();
        authorDto.setId(1L);
        authorDto.setName("Test Author");

        CommentDto dto1 = new CommentDto();
        dto1.setId(1L);
        dto1.setText("Первый комментарий");
        dto1.setAuthor(authorDto);
        dto1.setEventId(1L);

        CommentDto dto2 = new CommentDto();
        dto2.setId(2L);
        dto2.setText("Второй комментарий");
        dto2.setAuthor(authorDto);
        dto2.setEventId(1L);

        when(commentRepository.findAll(any(Pageable.class))).thenReturn(commentPage);
        when(mapper.toCommentDto(comment1)).thenReturn(dto1);
        when(mapper.toCommentDto(comment2)).thenReturn(dto2);

        List<CommentDto> result = commentAdminService.getAllComments(0, 10);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Первый комментарий", result.get(0).getText());
        assertEquals("Второй комментарий", result.get(1).getText());
        verify(commentRepository).findAll(any(Pageable.class));
    }

    @Test
    void getComment_Success() {
        Long commentId = 1L;

        User author = new User();
        author.setId(1L);
        author.setName("Test Author");

        Event event = new Event();
        event.setId(1L);

        Comment comment = Comment.builder()
                .id(commentId)
                .text("Тестовый комментарий")
                .author(author)
                .event(event)
                .createdOn(LocalDateTime.now())
                .build();

        UserShortDto authorDto = new UserShortDto();
        authorDto.setId(1L);
        authorDto.setName("Test Author");

        CommentDto expectedDto = new CommentDto();
        expectedDto.setId(commentId);
        expectedDto.setText("Тестовый комментарий");
        expectedDto.setAuthor(authorDto);
        expectedDto.setEventId(1L);

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(mapper.toCommentDto(comment)).thenReturn(expectedDto);

        CommentDto result = commentAdminService.getComment(commentId);

        assertNotNull(result);
        assertEquals(commentId, result.getId());
        assertEquals("Тестовый комментарий", result.getText());
        assertEquals(1L, result.getEventId());
        verify(commentRepository).findById(commentId);
        verify(mapper).toCommentDto(comment);
    }

    @Test
    void getComment_NotFound_ShouldThrowNotFoundException() {
        Long commentId = 999L;

        when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> commentAdminService.getComment(commentId));

        assertEquals("Комментарий с id=" + commentId + " не найден", exception.getMessage());
        verify(commentRepository).findById(commentId);
        verify(mapper, never()).toCommentDto(any());
    }
}
