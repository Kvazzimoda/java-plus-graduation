package ru.practicum.comment.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.comment.dto.request.comment.NewCommentDto;
import ru.practicum.comment.dto.response.comment.CommentDto;
import ru.practicum.comment.service.interfaces.CommentPrivateService;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}/comments")
@RequiredArgsConstructor
@Validated
@Slf4j
public class CommentPrivateController {

    private final CommentPrivateService commentPrivateService;

    @PostMapping("/{eventId}")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDto addComment(@PathVariable Long userId,
                                 @PathVariable Long eventId,
                                 @Valid @RequestBody NewCommentDto newCommentDto) {
        log.info("Создание комментария пользователем {} к событию {}", userId, eventId);
        return commentPrivateService.addComment(userId, eventId, newCommentDto);
    }

    @PatchMapping("/{commentId}")
    public CommentDto updateComment(@PathVariable Long userId,
                                    @PathVariable Long commentId,
                                    @Valid @RequestBody NewCommentDto newCommentDto) {
        log.info("Обновление комментария {} пользователем {}", commentId, userId);
        return commentPrivateService.updateComment(userId, commentId, newCommentDto);
    }

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable Long userId,
                              @PathVariable Long commentId) {
        log.info("Удаление комментария {} пользователем {}", commentId, userId);
        commentPrivateService.deleteComment(userId, commentId);
    }

    @GetMapping
    public List<CommentDto> getCommentsByUserId(@PathVariable Long userId,
                                                @PositiveOrZero @RequestParam(defaultValue = "0") Integer from,
                                                @Positive @RequestParam(defaultValue = "10") Integer size) {
        log.info("Получение всех комментариев пользователя {}", userId);
        Pageable pageable = PageRequest.of(from, size);
        return commentPrivateService.getCommentsByUserId(userId, pageable);
    }
}