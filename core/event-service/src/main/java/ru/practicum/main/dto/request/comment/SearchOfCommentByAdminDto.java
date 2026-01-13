package ru.practicum.main.dto.request.comment;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class SearchOfCommentByAdminDto {
    private List<Long> users;
    private List<Long> events;
    private LocalDateTime rangeStart;
    private LocalDateTime rangeEnd;
}