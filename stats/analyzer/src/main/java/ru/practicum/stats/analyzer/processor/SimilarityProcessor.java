package ru.practicum.stats.analyzer.processor;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.practicum.stats.analyzer.config.KafkaConfig;
import ru.practicum.stats.analyzer.service.SimilarityService;
import ru.practicum.stats.avro.EventSimilarityAvro;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class SimilarityProcessor {
    private static final Map<TopicPartition, OffsetAndMetadata> currentOffsets = new HashMap<>();

    private final KafkaConsumer<String, EventSimilarityAvro> consumer;
    private final List<String> topics;
    private final Duration pollTimeout;
    private final SimilarityService similarityService;

    public SimilarityProcessor(KafkaConfig config, SimilarityService similarityService) {
        final KafkaConfig.ConsumerConfig consumerConfig = config.getConsumers().get(this.getClass().getSimpleName());
        this.consumer = new KafkaConsumer<>(consumerConfig.getProperties());
        this.topics = consumerConfig.getTopics();
        this.pollTimeout = consumerConfig.getPollTimeout();
        this.similarityService = similarityService;

        // регистрируем хук, в котором вызываем метод wakeup.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Сработал хук на завершение JVM. Прерываю работу консьюмера.");
            consumer.wakeup();
        }));
    }

    public void start() {
        try{
            log.trace("Подписываемся на топики {}", topics);
            consumer.subscribe(topics);
            // цикл опроса
            while (true) {
                ConsumerRecords<String, EventSimilarityAvro> records = consumer.poll(pollTimeout);
                int count = 0;
                for (ConsumerRecord<String, EventSimilarityAvro> record : records) {
                    log.trace("Обработка сообщения от хаба {} из партиции {} с офсетом {}.",
                            record.key(), record.partition(), record.offset());
                    // обрабатываем очередную запись
                    similarityService.save(record.value());
                    // фиксируем оффсеты обработанных записей, если нужно
                    manageOffsets(record, count, consumer);
                    count++;
                }
                consumer.commitAsync();
            }
        } catch (WakeupException ignores) {
            // игнорируем - закрываем консьюмер и продюсер в блоке finally
            log.info("Получен сигнал завершения работы. WakeupException. Analyzer. SnapshotProcessor");
        } catch (Exception e) {
            log.error("Ошибка во время обработки событий от хабов", e);
        } finally {
            try {
                // здесь нужно вызвать метод консьюмера для фиксиции смещений
                consumer.commitSync(currentOffsets);
            } finally {
                log.info("Закрываем консьюмер");
                consumer.close();
            }
        }
    }

    private static void manageOffsets(ConsumerRecord<String, EventSimilarityAvro> record, int count,
                                      KafkaConsumer<String, EventSimilarityAvro> consumer) {
        // обновляем текущий оффсет для топика-партиции
        currentOffsets.put(
                new TopicPartition(record.topic(), record.partition()),
                new OffsetAndMetadata(record.offset() + 1)
        );
        if(count % 100 == 0) {
            consumer.commitAsync(currentOffsets, (offsets, exception) -> {
                if(exception != null) {
                    log.warn("Ошибка во время фиксации оффсетов: {}", offsets, exception);
                }
            });
        }
    }
}
