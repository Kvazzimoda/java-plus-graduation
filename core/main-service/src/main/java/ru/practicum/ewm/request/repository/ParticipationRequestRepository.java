package ru.practicum.ewm.request.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.request.model.ParticipationRequest;

import java.util.List;

public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequest, Long> {

    // Запросы пользователя
    List<ParticipationRequest> findAllByRequesterId(Long requesterId);

    // Запросы на событие пользователя
    List<ParticipationRequest> findAllByEventIdAndEventInitiatorId(Long eventId, Long initiatorId);

    // Проверка существования запроса
    boolean existsByRequesterIdAndEventId(Long requesterId, Long eventId);

    // Подсчет подтвержденных запросов для события
    @Query("SELECT COUNT(pr) FROM ParticipationRequest pr " +
            "WHERE pr.event.id = :eventId AND pr.status = 'CONFIRMED'")
    Long countConfirmedRequestsByEventId(@Param("eventId") Long eventId);

    // Подсчет подтвержденных запросов для списка событий
    @Query("SELECT pr.event.id, COUNT(pr) FROM ParticipationRequest pr " +
            "WHERE pr.event.id IN :eventIds AND pr.status = 'CONFIRMED' " +
            "GROUP BY pr.event.id")
    List<Object[]> countConfirmedRequestsByEventIds(@Param("eventIds") List<Long> eventIds);
}
