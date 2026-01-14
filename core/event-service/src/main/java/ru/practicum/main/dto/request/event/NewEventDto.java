package ru.practicum.main.dto.request.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.main.dto.Location;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewEventDto {
    @NotBlank(message = "Поле annotation должно быть заполнено")
    @Size(min = 20, max = 2000, message = "Длина должна быть от 20 до 2000 символов")
    private String annotation;

    @NotNull(message = "Поле category не может быть null")
    @Positive(message = "ID категории должен быть на позитиве :)")
    private Long category;

    @NotBlank(message = "Поле description должно быть заполнено")
    @Size(min = 20, max = 7000, message = "Длина должна быть от 20 до 7000 символов")
    private String description;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @NotNull(message = "Поле eventDate не может быть null")
    @Future(message = "Дата события должна быть в будущем")
    private LocalDateTime eventDate;

    @NotNull(message = "Локация не может быть null")
    private Location location;

    private Boolean paid = false;

    @PositiveOrZero(message = "Лимит участников должен быть больше или равен 0")
    private Integer participantLimit = 0;

    private Boolean requestModeration = true;

    @NotBlank(message = "Поле title не может быть пустым")
    @Size(min = 3, max = 120, message = "Длина должна быть от 3 до 120 символов")
    private String title;
}
