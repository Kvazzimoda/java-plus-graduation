package ru.practicum.stats.collector.service;

import ru.practicum.stats.proto.UserActionProto;

public interface UserActionHandler {
    void handle(UserActionProto event);
}
