package ru.practicum.stats.dto.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EndpointHitDto {
    @NotBlank(message = "Поле с наименованием приложения не может быть пустым!")
    private String app;
    @NotBlank(message = "Поле с адресом не может быть пустым!")
    private String uri;
    @NotBlank(message = "Поле с айпи адресом не может быть пустым!")
    private String ip;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
}
