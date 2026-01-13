package ru.practicum.comment.service;

import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.comment.client.user.UserClient;
import ru.practicum.comment.dto.mappers.CommentMapper;
import ru.practicum.comment.dto.request.comment.SearchOfCommentByAdminDto;
import ru.practicum.comment.dto.response.comment.CommentDto;
import ru.practicum.comment.dto.response.user.UserDto;
import ru.practicum.comment.exception.NotFoundException;
import ru.practicum.comment.model.Comment;
import ru.practicum.comment.model.QComment;
import ru.practicum.comment.repository.CommentRepository;
import ru.practicum.comment.service.interfaces.CommentAdminService;

import java.util.HashMap;
import java.util.List;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CommentAdminServiceImpl implements CommentAdminService {

    private final CommentRepository commentRepository;
    private final UserClient userClient;

    @Override
    @Transactional
    public void deleteComment(Long commentId) {
        log.info("Администратор удаляет комментарий с ID: {}", commentId);
        if (!commentRepository.existsById(commentId)) {
            throw new NotFoundException(String.format("Comment with id=%s was not found", commentId));
        }
        commentRepository.deleteById(commentId);
        log.info("Комментарий с ID: {} удален", commentId);
    }

    @Override
    public List<CommentDto> getComments(SearchOfCommentByAdminDto searchDto, Pageable pageable) {
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
}