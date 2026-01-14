package ru.practicum.main.dto.request.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewCommentDto {
    @NotBlank(message = "Текст комментария обязателен для заполнения")
    @Size(min = 5, max = 5000, message = "Длина должна быть от 5 до 5000 символов")
    private String text;
}
