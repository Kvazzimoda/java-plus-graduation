package ru.practicum.stats.analyzer.service;

import ru.practicum.stats.avro.EventSimilarityAvro;

public interface SimilarityService {
    void save(EventSimilarityAvro eventSimilarityAvro);
}
