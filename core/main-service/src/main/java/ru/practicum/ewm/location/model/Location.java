package ru.practicum.ewm.location.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(name = "locations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Широта не может быть пустой")
    @Column(name = "lat", nullable = false)
    private Float lat;

    @NotNull(message = "Долгота не может быть пустой")
    @Column(name = "lon", nullable = false)
    private Float lon;
}
