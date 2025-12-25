package ru.practicum.ewm.comment.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import ru.practicum.ewm.comment.dto.CommentShortDto;
import ru.practicum.ewm.comment.model.Comment;
import ru.practicum.ewm.comment.repository.CommentRepository;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.EwmMapper;
import ru.practicum.ewm.user.model.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentPublicServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EwmMapper mapper;

    @InjectMocks
    private CommentPublicService commentPublicService;

    @Test
    void getEventComments_Success() {
        Long eventId = 1L;

        Event event = new Event();
        event.setId(eventId);
        event.setState(EventState.PUBLISHED);

        User author = new User();
        author.setId(1L);
        author.setName("Test Author");

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
                .createdOn(LocalDateTime.now().minusHours(1))
                .build();

        List<Comment> comments = List.of(comment1, comment2);
        Page<Comment> commentPage = new PageImpl<>(comments);

        CommentShortDto dto1 = CommentShortDto.builder()
                .id(1L)
                .text("Первый комментарий")
                .authorName("Test Author")
                .createdOn(LocalDateTime.now())
                .build();

        CommentShortDto dto2 = CommentShortDto.builder()
                .id(2L)
                .text("Второй комментарий")
                .authorName("Test Author")
                .createdOn(LocalDateTime.now().minusHours(1))
                .build();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(commentRepository.findByEventIdOrderByCreatedOnDesc(eq(eventId), any(Pageable.class)))
                .thenReturn(commentPage);
        when(mapper.toCommentShortDto(comment1)).thenReturn(dto1);
        when(mapper.toCommentShortDto(comment2)).thenReturn(dto2);

        List<CommentShortDto> result = commentPublicService.getEventComments(eventId, 0, 10);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Первый комментарий", result.get(0).getText());
        assertEquals("Второй комментарий", result.get(1).getText());
        verify(eventRepository).findById(eventId);
        verify(commentRepository).findByEventIdOrderByCreatedOnDesc(eq(eventId), any(Pageable.class));
    }

    @Test
    void getEventComments_EventNotFound_ShouldThrowNotFoundException() {
        Long eventId = 999L;

        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> commentPublicService.getEventComments(eventId, 0, 10));

        assertEquals("Событие с id=" + eventId + " не найдено", exception.getMessage());
        verify(eventRepository).findById(eventId);
        verify(commentRepository, never()).findByEventIdOrderByCreatedOnDesc(any(), any());
    }

    @Test
    void getEventComments_EventNotPublished_ShouldThrowNotFoundException() {
        Long eventId = 1L;

        Event event = new Event();
        event.setId(eventId);
        event.setState(EventState.PENDING);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> commentPublicService.getEventComments(eventId, 0, 10));

        assertEquals("Событие не опубликовано", exception.getMessage());
        verify(eventRepository).findById(eventId);
        verify(commentRepository, never()).findByEventIdOrderByCreatedOnDesc(any(), any());
    }

    @Test
    void getEventComments_EmptyResult() {
        Long eventId = 1L;

        Event event = new Event();
        event.setId(eventId);
        event.setState(EventState.PUBLISHED);

        Page<Comment> emptyPage = new PageImpl<>(List.of());

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(commentRepository.findByEventIdOrderByCreatedOnDesc(eq(eventId), any(Pageable.class)))
                .thenReturn(emptyPage);

        List<CommentShortDto> result = commentPublicService.getEventComments(eventId, 0, 10);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(eventRepository).findById(eventId);
        verify(commentRepository).findByEventIdOrderByCreatedOnDesc(eq(eventId), any(Pageable.class));
    }

    @Test
    void getEventComments_WithCustomPagination() {
        Long eventId = 1L;
        int from = 5;
        int size = 20;

        Event event = new Event();
        event.setId(eventId);
        event.setState(EventState.PUBLISHED);

        Page<Comment> emptyPage = new PageImpl<>(List.of());

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(commentRepository.findByEventIdOrderByCreatedOnDesc(eq(eventId), any(Pageable.class)))
                .thenReturn(emptyPage);

        List<CommentShortDto> result = commentPublicService.getEventComments(eventId, from, size);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(eventRepository).findById(eventId);
        verify(commentRepository).findByEventIdOrderByCreatedOnDesc(eq(eventId), any(Pageable.class));
    }
}
