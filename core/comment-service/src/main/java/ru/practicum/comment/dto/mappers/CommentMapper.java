package ru.practicum.comment.dto.mappers;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.practicum.comment.dto.request.comment.NewCommentDto;
import ru.practicum.comment.dto.response.comment.CommentDto;
import ru.practicum.comment.dto.response.user.UserDto;
import ru.practicum.comment.model.Comment;

import static ru.practicum.comment.dto.mappers.UserMapper.toUserShortDto;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CommentMapper {

    public static Comment toEntity(NewCommentDto newCommentDto) {
        return Comment.builder()
                .id(0L)
                .text(newCommentDto.getText())
                .build();
    }

    public static CommentDto toDto(Comment comment, UserDto userDto) {
        return CommentDto.builder()
                .id(comment.getId())
                .user(toUserShortDto(userDto))
                .text(comment.getText())
                .createdOn(comment.getCreatedOn())
                .updatedOn(comment.getUpdatedOn())
                .build();
    }
}
