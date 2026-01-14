package ru.practicum.stats.analyzer.service;

import ru.practicum.stats.proto.InteractionsCountRequestProto;
import ru.practicum.stats.proto.RecommendedEventProto;
import ru.practicum.stats.proto.SimilarEventsRequestProto;
import ru.practicum.stats.proto.UserPredictionsRequestProto;

import java.util.stream.Stream;

public interface RecommendationsService {

    Stream<RecommendedEventProto> getRecommendationsForUser(UserPredictionsRequestProto request);

    Stream<RecommendedEventProto> getSimilarEvents(SimilarEventsRequestProto request);

    Stream<RecommendedEventProto> getInteractionsCount(InteractionsCountRequestProto request);
}
