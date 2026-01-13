package ru.practicum.comment.service.interfaces;

import org.springframework.data.domain.Pageable;
import ru.practicum.comment.dto.request.comment.SearchOfCommentByAdminDto;
import ru.practicum.comment.dto.response.comment.CommentDto;

import java.util.List;

public interface CommentAdminService {
    void deleteComment(Long commentId);

    List<CommentDto> getComments(SearchOfCommentByAdminDto searchDto, Pageable pageable);
}
