package ru.practicum.comment.service.impl;

import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import ru.practicum.core.client.EventClient;
import ru.practicum.comment.dto.mappers.CommentMapper;
import ru.practicum.comment.dto.request.comment.NewCommentDto;
import ru.practicum.comment.dto.request.comment.SearchOfCommentByAdminDto;
import ru.practicum.comment.dto.response.comment.CommentDto;
import ru.practicum.core.client.UserClient;
import ru.practicum.core.dto.EventDto;
import ru.practicum.comment.exception.NotFoundException;
import ru.practicum.comment.model.Comment;
import ru.practicum.comment.model.QComment;
import ru.practicum.comment.repository.CommentRepository;
import ru.practicum.comment.service.CommentService;
import ru.practicum.core.dto.UserDto;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentServiceImpl implements CommentService {

    private final TransactionTemplate transactionTemplate;
    private final CommentRepository commentRepository;
    private final UserClient userClient;
    private final EventClient eventClient;

    @Override
    public List<CommentDto> getCommentsByEventId(Long eventId, Pageable pageable) {
        log.info("Получение комментариев для события с ID: {}", eventId);

        // Получаем комментарии
        List<Comment> comments = commentRepository.findByEventIdOrderByCreatedOnDesc(eventId, pageable);

        // Получаем уникальные ID пользователей из комментариев
        Set<Long> userIds = comments.stream()
                .map(Comment::getUserId)
                .collect(Collectors.toSet());

        // Получаем пользователей через клиент
        Map<Long, UserDto> usersMap = getUsersByIds(userIds);

        return comments.stream()
                .map(comment -> {
                    UserDto userDto = usersMap.get(comment.getUserId());
                    if (userDto == null) {
                        log.warn("Пользователь с ID {} не найден для комментария {}",
                                comment.getUserId(), comment.getId());
                        throw new NotFoundException("Пользователь c userId " + comment.getUserId() + " не найден");
                    }
                    return CommentMapper.toDto(comment, userDto);
                })
                .collect(Collectors.toList());
    }


    @Override
    public CommentDto addComment(Long userId, Long eventId, NewCommentDto newCommentDto) {

        // 1. ВНЕ транзакции
        UserDto user = getUserById(userId);
        EventDto event = getEventById(eventId);

        // 2. В транзакции — только БД
        Comment savedComment = transactionTemplate.execute(status -> {
            Comment comment = CommentMapper.toEntity(newCommentDto);
            comment.setUserId(userId);
            comment.setEventId(event.getId());
            comment.setCreatedOn(LocalDateTime.now());
            comment.setUpdatedOn(LocalDateTime.now());
            return commentRepository.save(comment);
        });

        return CommentMapper.toDto(savedComment, user);
    }

    @Override
    public void deleteCommentByUser(Long userId, Long commentId) {
        // Вне транзакции вызываем внешние сервисы
        UserDto user = getUserById(userId);

        // В транзакции только доступ к БД
        transactionTemplate.execute(status -> {
            Comment comment = commentRepository.findById(commentId)
                    .orElseThrow(() -> new NotFoundException("Comment with id=" + commentId + " не найден"));

            if (!comment.getUserId().equals(userId)) {
                throw new IllegalArgumentException("User is not the author");
            }

            commentRepository.delete(comment);
            return null;
        });

        log.info("Комментарий с ID: {} удален пользователем {}", commentId, userId);
    }


    @Override
    public CommentDto updateCommentByUser(Long userId, Long commentId, NewCommentDto updateCommentDto) {
        // Вне транзакции
        UserDto user = getUserById(userId);

        // Транзакция только на сохранение комментария
        Comment updatedComment = transactionTemplate.execute(status -> {
            Comment comment = commentRepository.findById(commentId)
                    .orElseThrow(() -> new NotFoundException("Comment with id=" + commentId + " не найден"));

            if (!comment.getUserId().equals(userId)) {
                throw new IllegalArgumentException("User is not the author");
            }

            comment.setText(updateCommentDto.getText());
            comment.setUpdatedOn(LocalDateTime.now());
            return commentRepository.save(comment);
        });

        return CommentMapper.toDto(updatedComment, user);
    }


    @Override
    public List<CommentDto> getCommentsByUserId(Long userId, Pageable pageable) {
        log.info("Получение комментариев пользователя с ID: {}", userId);
        UserDto user = getUserById(userId);
        return commentRepository.findByUserIdOrderByCreatedOnDesc(userId, pageable).stream()
                .map(comment -> CommentMapper.toDto(comment, user))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteCommentByAdmin(Long commentId) {
        log.info("Администратор удаляет комментарий с ID: {}", commentId);
        if (!commentRepository.existsById(commentId)) {
            throw new NotFoundException(String.format("Comment with id=%s was not found", commentId));
        }
        commentRepository.deleteById(commentId);
        log.info("Комментарий с ID: {} удален", commentId);
    }

    @Override
    public List<CommentDto> getCommentsByAdmin(SearchOfCommentByAdminDto searchDto, Pageable pageable) {
        log.info("Администратор получает комментарии с фильтрами: {}", searchDto);

        QComment comment = QComment.comment;
        BooleanBuilder predicate = new BooleanBuilder();

        if (searchDto.getUsers() != null && !searchDto.getUsers().isEmpty()) {
            predicate.and(comment.userId.in(searchDto.getUsers()));
        }
        if (searchDto.getEvents() != null && !searchDto.getEvents().isEmpty()) {
            predicate.and(comment.eventId.in(searchDto.getEvents()));
        }
        if (searchDto.getRangeStart() != null) {
            predicate.and(comment.createdOn.goe(searchDto.getRangeStart()));
        }
        if (searchDto.getRangeEnd() != null) {
            predicate.and(comment.createdOn.loe(searchDto.getRangeEnd()));
        }

        // Получаем все комментарии
        List<Comment> comments = commentRepository.findAll(predicate, pageable).getContent();

        // Получаем пользователей из списка в searchDto (или всех, если список пустой)
        List<Long> userIdsToFetch;
        if (searchDto.getUsers() != null && !searchDto.getUsers().isEmpty()) {
            userIdsToFetch = searchDto.getUsers();
        } else {
            // Если фильтр по пользователям не задан, получаем всех пользователей из комментариев
            userIdsToFetch = comments.stream()
                    .map(Comment::getUserId)
                    .distinct()
                    .collect(Collectors.toList());
        }

        // Получаем пользователей через клиент
        Map<Long, UserDto> usersMap = getUsersByIds(userIdsToFetch);

        return comments.stream()
                .map(commentEntity -> {
                    UserDto userDto = usersMap.get(commentEntity.getUserId());
                    if (userDto == null) {
                        log.warn("Пользователь с ID {} не найден для комментария {}",
                                commentEntity.getUserId(), commentEntity.getId());
                        throw new NotFoundException("Пользователь c userId " + commentEntity.getUserId() + " не найден");
                    }
                    return CommentMapper.toDto(commentEntity, userDto);
                })
                .collect(Collectors.toList());
    }

    private Map<Long, UserDto> getUsersByIds(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return new HashMap<>();
        }

        try {
            // Используем существующий метод getUsers, который принимает List<Long> ids
            List<UserDto> users = userClient.getUsers(userIds);
            return users.stream()
                    .collect(Collectors.toMap(UserDto::getId, Function.identity()));
        } catch (Exception e) {
            log.error("Failed to get users from user-service: {}", e.getMessage());
            // Возвращаем пустую мапу, чтобы не падать полностью
            return new HashMap<>();
        }
    }

    private UserDto getUserById(Long userId) {
        //Получаем пользователя через клиент
        try {
            UserDto user = userClient.getUserById(userId);
            log.debug("Existing User received from user-service: {}", user);
            return user;
        } catch (Exception e) {
            log.debug("Failed to get user from user-service: {}", e.getMessage());
            throw new NotFoundException("Пользователь c userId " + userId + " не найден");
        }
    }

    private Map<Long, UserDto> getUsersByIds(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return new HashMap<>();
        }

        try {
            // Используем существующий метод getUsers, который принимает List<Long> ids
            List<UserDto> users = userClient.getUsers(new ArrayList<>(userIds));
            return users.stream()
                    .collect(Collectors.toMap(UserDto::getId, Function.identity()));
        } catch (Exception e) {
            log.error("Failed to get users from user-service: {}", e.getMessage());
            // Возвращаем пустую мапу, чтобы не падать полностью
            return new HashMap<>();
        }
    }

    private EventDto getEventById(Long eventId) {
        try {
            EventDto event = eventClient.getEventById(eventId);
            log.debug("Existing Event received from event-service: {}", event);
            return event;
        } catch (Exception e) {
            log.warn("Failed to get event from event-service: {}", e.getMessage());
            throw new NotFoundException("Событие c eventId " + eventId + " не найдено");
        }
    }
}
