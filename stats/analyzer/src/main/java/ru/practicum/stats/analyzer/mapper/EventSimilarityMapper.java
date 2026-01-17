package ru.practicum.stats.analyzer.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.stats.analyzer.model.EventSimilarity;
import ru.practicum.stats.avro.EventSimilarityAvro;

import java.time.LocalDateTime;
import java.time.ZoneId;

@UtilityClass
public class EventSimilarityMapper {

    public static EventSimilarity toEntity(EventSimilarityAvro avro) {
        if (avro == null) {
            return null;
        }

        // Нормализуем порядок событий: event1 всегда меньше event2
        long event1 = Math.min(avro.getEventA(), avro.getEventB());
        long event2 = Math.max(avro.getEventA(), avro.getEventB());

        return EventSimilarity.builder()
                .event1(event1)
                .event2(event2)
                .similarity(avro.getScore())
                .timestamp(LocalDateTime.ofInstant(avro.getTimestamp(), ZoneId.systemDefault()))
                .build();
    }
}
