package ru.practicum.comment.controller;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.comment.dto.request.comment.SearchOfCommentByAdminDto;
import ru.practicum.comment.dto.response.comment.CommentDto;
import ru.practicum.comment.service.interfaces.CommentAdminService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/admin/comments")
@RequiredArgsConstructor
@Validated
@Slf4j
public class CommentAdminController {

    private final CommentAdminService commentAdminService;

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCommentByAdmin(@PathVariable Long commentId) {
        log.info("Администратор удаляет комментарий с ID {}", commentId);
        commentAdminService.deleteComment(commentId);
    }

    @GetMapping
    public List<CommentDto> getComments(@RequestParam(required = false) List<Long> userIds,
                                        @RequestParam(required = false) List<Long> eventIds,
                                        @RequestParam(name = "rangeStart", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeStart,
                                        @RequestParam(name = "rangeEnd", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeEnd,
                                        @PositiveOrZero @RequestParam(defaultValue = "0") Integer from,
                                        @Positive @RequestParam(defaultValue = "10") Integer size) {
        log.info("Админ запрос на получение комментариев");
        SearchOfCommentByAdminDto searchOfCommentByAdminDto = SearchOfCommentByAdminDto.builder()
                .users(userIds)
                .events(eventIds)
                .rangeStart(rangeStart)
                .rangeEnd(rangeEnd)
                .build();
        Pageable pageable = PageRequest.of(from, size);
        return commentAdminService.getComments(searchOfCommentByAdminDto, pageable);
    }
}