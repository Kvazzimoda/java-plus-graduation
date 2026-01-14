package ru.practicum.comment.service;

import org.springframework.data.domain.Pageable;
import ru.practicum.comment.dto.request.comment.NewCommentDto;
import ru.practicum.comment.dto.request.comment.SearchOfCommentByAdminDto;
import ru.practicum.comment.dto.response.comment.CommentDto;

import java.util.List;

public interface CommentService {

    // Admin
    void deleteCommentByAdmin(Long commentId);

    List<CommentDto> getCommentsByAdmin(SearchOfCommentByAdminDto searchDto, Pageable pageable);

    // Private (user)
    CommentDto addComment(Long userId, Long eventId, NewCommentDto newCommentDto);

    void deleteCommentByUser(Long userId, Long commentId);

    CommentDto updateCommentByUser(Long userId, Long commentId, NewCommentDto updateCommentDto);

    List<CommentDto> getCommentsByUserId(Long userId, Pageable pageable);

    // Public
    List<CommentDto> getCommentsByEventId(Long eventId, Pageable pageable);
}
