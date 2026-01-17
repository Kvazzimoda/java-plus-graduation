package ru.practicum.stats.analyzer.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.stats.analyzer.mapper.EventSimilarityMapper;
import ru.practicum.stats.analyzer.model.EventSimilarity;
import ru.practicum.stats.analyzer.repository.SimilarityRepository;
import ru.practicum.stats.avro.EventSimilarityAvro;

@Slf4j
@Service
@AllArgsConstructor
@Transactional
public class SimilarityServiceImpl implements SimilarityService{

    private final SimilarityRepository similarityRepository;

    @Override
    public void save(EventSimilarityAvro eventSimilarityAvro) {
        log.info("Сохранение сходства событий А {} и Б {}: {}",
                eventSimilarityAvro.getEventA(),
                eventSimilarityAvro.getEventB(),
                eventSimilarityAvro.getScore());
        EventSimilarity newSimilarity = EventSimilarityMapper.toEntity(eventSimilarityAvro);
        if (newSimilarity == null) {
            log.error("Не удалось конвертировать EventSimilarityAvro в Entity: {}", eventSimilarityAvro);
            return;
        }

        // Проверяем, что событие не связано с самим собой
        if (newSimilarity.getEvent1().equals(newSimilarity.getEvent2())) {
            log.warn("Попытка сохранить сходство события {} с самим собой. Пропускаем.",
                    newSimilarity.getEvent1());
            return;
        }

        // Ищем существующую запись / сохраняем
        similarityRepository
                .findByEvent1AndEvent2(newSimilarity.getEvent1(), newSimilarity.getEvent2())
                .ifPresentOrElse(
                        existingSimilarity -> updateExistingSimilarity(existingSimilarity, newSimilarity),
                        () -> createNewSimilarity(newSimilarity)
                );
    }

    private void updateExistingSimilarity(EventSimilarity existing, EventSimilarity updated) {
        // Обновляем score и timestamp
        log.debug("Обновляем сходство событий {} и {}: {} -> {}",
                existing.getEvent1(),
                existing.getEvent2(),
                existing.getSimilarity(),
                updated.getSimilarity());

        existing.setSimilarity(updated.getSimilarity());
        existing.setTimestamp(updated.getTimestamp());
        similarityRepository.save(existing);
    }

    private void createNewSimilarity(EventSimilarity newSimilarity) {
        log.debug("Создаем новую связь между событиями {} и {} со сходством {}",
                newSimilarity.getEvent1(),
                newSimilarity.getEvent2(),
                newSimilarity.getSimilarity());

        similarityRepository.save(newSimilarity);
    }
}
