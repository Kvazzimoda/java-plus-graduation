package ru.practicum.ewm.compilation.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import ru.practicum.ewm.event.model.Event;

import java.util.Set;

@Entity
@Table(name = "compilations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Compilation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pinned", nullable = false)
    @Builder.Default
    private Boolean pinned = false;

    @NotBlank(message = "Название подборки не может быть пустым")
    @Size(min = 1, max = 50, message = "Название подборки должно содержать от 1 до 50 символов")
    @Column(name = "title", nullable = false, length = 50)
    private String title;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "compilation_events",
            joinColumns = @JoinColumn(name = "compilation_id"),
            inverseJoinColumns = @JoinColumn(name = "event_id")
    )
    private Set<Event> events;
}
