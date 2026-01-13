package ru.practicum.comment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.practicum.comment.client.user.UserClient;
import ru.practicum.comment.dto.mappers.CommentMapper;
import ru.practicum.comment.dto.response.comment.CommentDto;
import ru.practicum.comment.dto.response.user.UserDto;
import ru.practicum.comment.exception.NotFoundException;
import ru.practicum.comment.model.Comment;
import ru.practicum.comment.repository.CommentRepository;
import ru.practicum.comment.service.interfaces.CommentPublicService;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentPublicServiceImpl implements CommentPublicService {

    private final CommentRepository commentRepository;
    private final UserClient userClient;

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
}