package ru.practicum.stats.analyzer.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.stats.analyzer.mapper.RecommendationsMapper;
import ru.practicum.stats.analyzer.model.EventSimilarity;
import ru.practicum.stats.analyzer.model.UserAction;
import ru.practicum.stats.analyzer.repository.SimilarityRepository;
import ru.practicum.stats.analyzer.repository.UserInteractionRepository;
import ru.practicum.stats.proto.InteractionsCountRequestProto;
import ru.practicum.stats.proto.RecommendedEventProto;
import ru.practicum.stats.proto.SimilarEventsRequestProto;
import ru.practicum.stats.proto.UserPredictionsRequestProto;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@AllArgsConstructor
@Transactional(readOnly = true)
public class RecommendationsServiceImpl implements RecommendationsService{

    private final UserInteractionRepository userInteractionRepository;
    private final SimilarityRepository similarityRepository;

    // Параметры алгоритма
    private static final int K_NEIGHBORS = 10; // Количество ближайших соседей для предсказания

    @Override
    public Stream<RecommendedEventProto> getRecommendationsForUser(UserPredictionsRequestProto request) {
        log.info("Получение рекомендаций для пользователя {} (max_results: {})",
                request.getUserId(), request.getMaxResults());

        Long userId = request.getUserId();
        int maxResults = (int) request.getMaxResults();

        if (maxResults <= 0) {
            log.warn("Запрошено недопустимое количество результатов: {}", maxResults);
            return Stream.empty();
        }

        // 1. Получить все взаимодействия пользователя
        List<UserAction> userInteractions = userInteractionRepository.findAllByUserId(userId);

        if (userInteractions.isEmpty()) {
            log.info("Пользователь {} не имеет взаимодействий, рекомендации невозможны", userId);
            return Stream.empty();
        }

        // Сортируем по времени (новые сначала)
        List<UserAction> sortedInteractions = RecommendationsMapper.sortByTimestampDesc(userInteractions);

        // 2. События, с которыми пользователь уже взаимодействовал
        Set<Long> alreadyInteractedEventIds = sortedInteractions.stream()
                .map(UserAction::getEventId)
                .collect(Collectors.toSet());

        // 3. Собираем все потенциально похожие события
        Map<Long, Double> candidateEvents = new HashMap<>();

        for (UserAction interaction : sortedInteractions) {
            Long eventId = interaction.getEventId();
            List<EventSimilarity> similarities = similarityRepository.findAllByEventId(eventId);

            for (EventSimilarity similarity : similarities) {
                Long candidateEventId = similarity.getEvent1().equals(eventId)
                        ? similarity.getEvent2()
                        : similarity.getEvent1();

                // Пропускаем события, с которыми пользователь уже взаимодействовал
                if (alreadyInteractedEventIds.contains(candidateEventId)) {
                    continue;
                }

                // Используем максимальное значение сходства для кандидата
                double currentScore = candidateEvents.getOrDefault(candidateEventId, 0.0);
                candidateEvents.put(candidateEventId, Math.max(currentScore, similarity.getSimilarity()));
            }
        }

        if (candidateEvents.isEmpty()) {
            log.info("Для пользователя {} не найдено кандидатов для рекомендаций", userId);
            return Stream.empty();
        }

        // 4. Для каждого кандидата вычисляем предсказанную оценку
        return candidateEvents.entrySet().stream()
                .parallel() // Параллельная обработка для производительности
                .map(entry -> {
                    Long candidateEventId = entry.getKey();
                    Double predictedScore = calculatePredictedScore(userId, candidateEventId, alreadyInteractedEventIds);

                    if (predictedScore != null) {
                        return RecommendationsMapper.toRecommendedEventProto(candidateEventId, predictedScore);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(RecommendedEventProto::getScore).reversed())
                .limit(maxResults);
    }


    @Override
    public Stream<RecommendedEventProto> getSimilarEvents(SimilarEventsRequestProto request) {
        log.info("Поиск похожих событий для события {} (user_id: {}, max_results: {})",
                request.getEventId(), request.getUserId(), request.getMaxResults());

        Long eventId = request.getEventId();
        long userId = request.getUserId();
        int maxResults = (int) request.getMaxResults();

        if (maxResults <= 0) {
            log.warn("Запрошено недопустимое количество результатов: {}", maxResults);
            return Stream.empty();
        }

        // 1. Получить все похожие мероприятия
        List<EventSimilarity> similarities = similarityRepository.findAllByEventId(eventId);

        if (similarities.isEmpty()) {
            log.info("Для события {} не найдено похожих событий", eventId);
            return Stream.empty();
        }

        if (userId == 0) {
            // Если пользователь не указан, возвращаем все похожие события
            return similarities.stream()
                    .sorted(Comparator.comparing(EventSimilarity::getSimilarity).reversed())
                    .limit(maxResults)
                    .map(similarity -> RecommendationsMapper.toRecommendedEventProto(similarity, eventId))
                    .filter(obj -> true);
        }

        // 2. Получить события, с которыми пользователь уже взаимодействовал
        Set<Long> userInteractedEventIds = userInteractionRepository.findAllByUserId(userId).stream()
                .map(UserAction::getEventId)
                .collect(Collectors.toSet());

        // 3. Фильтруем и сортируем
        return similarities.stream()
                .filter(similarity -> {
                    Long otherEventId = similarity.getEvent1().equals(eventId)
                            ? similarity.getEvent2()
                            : similarity.getEvent1();
                    return !userInteractedEventIds.contains(otherEventId);
                })
                .sorted(Comparator.comparing(EventSimilarity::getSimilarity).reversed())
                .limit(maxResults)
                .map(similarity -> RecommendationsMapper.toRecommendedEventProto(similarity, eventId))
                .filter(obj -> true);
    }

    @Override
    public Stream<RecommendedEventProto> getInteractionsCount(InteractionsCountRequestProto request) {
        log.info("Получение суммы взаимодействий для {} событий", request.getEventIdCount());

        // Обрабатываем все запрошенные события
        return request.getEventIdList().stream()
                .distinct()
                .map(eventId -> {
                    List<UserAction> interactions = userInteractionRepository.findAllByEventId(eventId);

                    if (interactions.isEmpty()) {
                        return RecommendationsMapper.toRecommendedEventProto(eventId, 0.0);
                    }

                    // Группируем по пользователям и берем максимальный рейтинг для каждого
                    double totalScore = interactions.stream()
                            .collect(Collectors.groupingBy(
                                    UserAction::getUserId,
                                    Collectors.collectingAndThen(
                                            Collectors.maxBy(Comparator.comparing(UserAction::getRating)),
                                            opt -> opt.map(UserAction::getRating).orElse(0.0)
                                    )
                            ))
                            .values()
                            .stream()
                            .mapToDouble(Double::doubleValue)
                            .sum();

                    return RecommendationsMapper.toRecommendedEventProto(eventId, totalScore);
                })
                .sorted(Comparator.comparing(RecommendedEventProto::getScore).reversed());
    }

    /**
     * Вычисляет предсказанную оценку для кандидата на основе K ближайших соседей
     */
    private Double calculatePredictedScore(Long userId, Long candidateEventId, Set<Long> userInteractedEventIds) {
        // 1. Получаем все сходства для кандидата
        List<EventSimilarity> allSimilarities = similarityRepository.findAllByEventId(candidateEventId);

        // 2. Фильтруем только те события, с которыми пользователь взаимодействовал
        List<EventSimilarity> neighborSimilarities = allSimilarities.stream()
                .filter(similarity -> {
                    Long similarEventId = similarity.getEvent1().equals(candidateEventId)
                            ? similarity.getEvent2()
                            : similarity.getEvent1();
                    return userInteractedEventIds.contains(similarEventId);
                })
                .sorted(Comparator.comparing(EventSimilarity::getSimilarity).reversed())
                .limit(K_NEIGHBORS)
                .toList();

        if (neighborSimilarities.isEmpty()) {
            // Если нет соседей, возвращаем null (кандидат не подходит)
            return null;
        }

        // 3. Вычисляем взвешенную оценку
        double weightedSum = 0.0;
        double similaritySum = 0.0;

        for (EventSimilarity similarity : neighborSimilarities) {
            Long similarEventId = similarity.getEvent1().equals(candidateEventId)
                    ? similarity.getEvent2()
                    : similarity.getEvent1();

            Optional<UserAction> userAction = userInteractionRepository.findByUserIdAndEventId(userId, similarEventId);

            if (userAction.isPresent()) {
                double rating = userAction.get().getRating();
                double similarityScore = similarity.getSimilarity();

                weightedSum += rating * similarityScore;
                similaritySum += similarityScore;
            }
        }

        if (similaritySum == 0) {
            return 0.0;
        }

        return weightedSum / similaritySum;
    }
}
