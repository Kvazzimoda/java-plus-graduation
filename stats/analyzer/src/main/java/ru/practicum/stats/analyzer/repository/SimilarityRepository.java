package ru.practicum.stats.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.stats.analyzer.model.EventSimilarity;

import java.util.List;
import java.util.Optional;

public interface SimilarityRepository extends JpaRepository<EventSimilarity, Long> {

    Optional<EventSimilarity> findByEvent1AndEvent2(Long event1, Long event2);

    @Query("SELECT es FROM EventSimilarity es WHERE es.event1 = :eventId OR es.event2 = :eventId")
    List<EventSimilarity> findAllByEventId(@Param("eventId") Long eventId);
}
