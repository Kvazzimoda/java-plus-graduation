package ru.practicum.ewm.comment.controller;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.comment.dto.CommentDto;
import ru.practicum.ewm.comment.service.CommentAdminService;

import java.util.List;

@RestController
@RequestMapping("/admin/comments")
@RequiredArgsConstructor
@Validated
@Slf4j
public class AdminCommentController {

    private final CommentAdminService commentAdminService;

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable Long commentId) {
        log.info("Удаление комментария администратором commentId={}", commentId);
        commentAdminService.deleteComment(commentId);
    }

    @GetMapping
    public List<CommentDto> getAllComments(
            @RequestParam(defaultValue = "0") @PositiveOrZero int from,
            @RequestParam(defaultValue = "10") @Positive int size) {

        log.info("Получение всех комментариев администратором from={}, size={}", from, size);
        return commentAdminService.getAllComments(from, size);
    }

    @GetMapping("/{commentId}")
    public CommentDto getComment(@PathVariable Long commentId) {
        log.info("Получение комментария администратором commentId={}", commentId);
        return commentAdminService.getComment(commentId);
    }
}
