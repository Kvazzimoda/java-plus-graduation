package ru.practicum.server.dao;

import ru.practicum.stats.dto.dto.ViewStatsDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.server.model.EndpointHit;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EndpointHitRepository extends JpaRepository<EndpointHit, Integer> {

    @Query("""
            SELECT new ru.practicum.stats.dto.dto.ViewStatsDto(e.app, e.uri, COUNT(DISTINCT e.ip))
            FROM EndpointHit e
            WHERE e.uri IN :uris AND e.timestamp BETWEEN :start AND :end
            GROUP BY e.app, e.uri
            ORDER BY COUNT(DISTINCT e.ip) DESC
            """)
    List<ViewStatsDto> findUniqueStatsByUris(@Param("uris") List<String> uris, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);


    @Query("""
            SELECT new ru.practicum.stats.dto.dto.ViewStatsDto(e.app, e.uri, COUNT(e.ip))
            FROM EndpointHit e
            WHERE e.uri IN :uris AND e.timestamp BETWEEN :start AND :end
            GROUP BY e.app, e.uri
            ORDER BY COUNT(e.ip) DESC
            """)
    List<ViewStatsDto> findAllStatsByUris(@Param("uris") List<String> uris, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("""
            SELECT new ru.practicum.stats.dto.dto.ViewStatsDto(e.app, e.uri, COUNT(e.ip))
            FROM EndpointHit e
            WHERE e.timestamp BETWEEN :start AND :end
            GROUP BY e.app, e.uri
            ORDER BY COUNT(e.ip) DESC
            """)
    List<ViewStatsDto> findAllStats(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("""
            SELECT new ru.practicum.stats.dto.dto.ViewStatsDto(e.app, e.uri, COUNT(DISTINCT e.ip))
            FROM EndpointHit e
            WHERE e.timestamp BETWEEN :start AND :end
            GROUP BY e.app, e.uri
            ORDER BY COUNT(DISTINCT e.ip) DESC
            """)
    List<ViewStatsDto> findAllUniqueStats(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT e FROM EndpointHit e WHERE e.ip = :ip AND e.uri = :uri")
    Optional<EndpointHit> findByIpAndUri(String ip, String uri);
}
