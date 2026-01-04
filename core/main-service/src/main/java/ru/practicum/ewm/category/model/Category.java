package ru.practicum.ewm.category.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Название категории не может быть пустым")
    @Size(min = 1, max = 50, message = "Название категории должно содержать от 1 до 50 символов")
    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;
}
