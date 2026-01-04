package ru.practicum.ewm.location.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocationDto {

    @NotNull(message = "Широта не может быть пустой")
    private Float lat;

    @NotNull(message = "Долгота не может быть пустой")
    private Float lon;
}
