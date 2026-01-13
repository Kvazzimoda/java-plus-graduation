package ru.practicum.comment.service.interfaces;

import org.springframework.data.domain.Pageable;
import ru.practicum.comment.dto.response.comment.CommentDto;

import java.util.List;

public interface CommentPublicService {
    List<CommentDto> getCommentsByEventId(Long eventId, Pageable pageable);
}
