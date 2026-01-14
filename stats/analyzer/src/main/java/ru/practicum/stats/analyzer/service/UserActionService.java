package ru.practicum.stats.analyzer.service;

import ru.practicum.stats.avro.UserActionAvro;

public interface UserActionService {
    void save(UserActionAvro userActionAvro);
}
