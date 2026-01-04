package ru.practicum.ewm.event.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventState;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    // Для админа - поиск событий по пользователям, состояниям и категориям
    @Query("SELECT e FROM Event e " +
            "WHERE (:users IS NULL OR e.initiator.id IN :users) " +
            "AND (:states IS NULL OR e.state IN :states) " +
            "AND (:categories IS NULL OR e.category.id IN :categories) " +
            "AND (e.eventDate >= :rangeStart) " +
            "AND (e.eventDate <= :rangeEnd)")
    Page<Event> findEventsForAdmin(@Param("users") List<Long> users,
                                   @Param("states") List<EventState> states,
                                   @Param("categories") List<Long> categories,
                                   @Param("rangeStart") LocalDateTime rangeStart,
                                   @Param("rangeEnd") LocalDateTime rangeEnd,
                                   Pageable pageable);

    // Для публичного API - поиск опубликованных событий
    @Query("""
                SELECT e FROM Event e
                WHERE e.state = 'PUBLISHED'
                  AND (:text IS NULL OR :text = ''
                       OR LOWER(e.annotation) LIKE CONCAT('%', LOWER(:text), '%')
                       OR LOWER(e.description) LIKE CONCAT('%', LOWER(:text), '%'))
                  AND (:categories IS NULL OR e.category.id IN :categories)
                  AND (:paid IS NULL OR e.paid = :paid)
                  AND (e.eventDate >= :rangeStart)
                  AND (e.eventDate <= :rangeEnd)
            """)
    Page<Event> findPublishedEvents(@Param("text") String text,
                                    @Param("categories") List<Long> categories,
                                    @Param("paid") Boolean paid,
                                    @Param("rangeStart") LocalDateTime rangeStart,
                                    @Param("rangeEnd") LocalDateTime rangeEnd,
                                    Pageable pageable);

    // События пользователя
    Page<Event> findAllByInitiatorId(Long initiatorId, Pageable pageable);

    // Опубликованное событие по ID
    Optional<Event> findByIdAndState(Long id, EventState state);

    // Проверка существования категории в событиях
    boolean existsByCategoryId(Long categoryId);
}
