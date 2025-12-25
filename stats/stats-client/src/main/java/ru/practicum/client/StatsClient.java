package ru.practicum.client;

import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStats;

import java.util.Collection;
import java.util.List;

public interface StatsClient {

    void hit(EndpointHitDto endpointHitDto);

    Collection<ViewStats> getStat(String start, String end, List<String> urls, Boolean unique);
}
