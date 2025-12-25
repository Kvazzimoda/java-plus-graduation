package ru.practicum.ewm.comment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.comment.dto.CommentDto;
import ru.practicum.ewm.comment.dto.NewCommentDto;
import ru.practicum.ewm.comment.dto.UpdateCommentDto;
import ru.practicum.ewm.comment.model.Comment;
import ru.practicum.ewm.comment.repository.CommentRepository;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.EwmMapper;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class CommentPrivateService {

    private final CommentRepository commentRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final EwmMapper mapper;

    private static final int EDIT_TIME_LIMIT_HOURS = 24; // Время для редактирования - 24 часа

    @Transactional
    public CommentDto createComment(Long userId, Long eventId, NewCommentDto dto) {
        log.info("Создание комментария пользователем userId={}, eventId={}", userId, eventId);

        User author = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        // Проверяем, что событие опубликовано
        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new ConflictException("Нельзя комментировать неопубликованное событие");
        }

        Comment comment = mapper.toComment(dto, event, author);
        comment.setCreatedOn(LocalDateTime.now());

        Comment savedComment = commentRepository.save(comment);

        log.info("Комментарий создан с id={}", savedComment.getId());
        return mapper.toCommentDto(savedComment);
    }

    @Transactional
    public CommentDto updateComment(Long userId, Long commentId, UpdateCommentDto dto) {
        log.info("Обновление комментария userId={}, commentId={}", userId, commentId);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Комментарий с id=" + commentId + " не найден"));

        // Проверяем, что пользователь является автором комментария
        if (!comment.getAuthor().getId().equals(userId)) {
            throw new ConflictException("Пользователь не может редактировать чужой комментарий");
        }

        // Проверяем время редактирования
        if (comment.getCreatedOn().isBefore(LocalDateTime.now().minusHours(EDIT_TIME_LIMIT_HOURS))) {
            throw new ConflictException("Время редактирования комментария истекло");
        }

        comment.setText(dto.getText());
        comment.setUpdatedOn(LocalDateTime.now());

        Comment updatedComment = commentRepository.save(comment);

        log.info("Комментарий с id={} обновлен", commentId);
        return mapper.toCommentDto(updatedComment);
    }

    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        log.info("Удаление комментария userId={}, commentId={}", userId, commentId);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Комментарий с id=" + commentId + " не найден"));

        // Проверяем, что пользователь является автором комментария
        if (!comment.getAuthor().getId().equals(userId)) {
            throw new ConflictException("Пользователь не может удалить чужой комментарий");
        }

        commentRepository.deleteById(commentId);
        log.info("Комментарий с id={} удален", commentId);
    }

    public List<CommentDto> getUserComments(Long userId, int from, int size) {
        log.info("Получение комментариев пользователя userId={}", userId);

        // Проверяем, что пользователь существует
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));

        Pageable pageable = PageRequest.of(from / size, size);

        return commentRepository.findByAuthorIdOrderByCreatedOnDesc(userId, pageable)
                .stream()
                .map(mapper::toCommentDto)
                .collect(Collectors.toList());
    }
}
