package ru.practicum.stats.aggregator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.practicum.stats.aggregator.config.KafkaAggregatorConfig;
import ru.practicum.stats.avro.UserActionAvro;
import ru.practicum.stats.avro.EventSimilarityAvro;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AggregationStarter {
    private final EventsSimilarityService eventsSimilarityService;

    private static final Map<TopicPartition, OffsetAndMetadata> currentOffsets = new HashMap<>();

    private final KafkaAggregatorConfig.ConsumerConfig consumerConfig;
    private final KafkaConsumer<String, UserActionAvro> consumer;

    private final KafkaAggregatorConfig.ProducerConfig producerConfig;
    private final KafkaProducer<String, EventSimilarityAvro> producer;

    @Autowired
    public AggregationStarter(EventsSimilarityService eventsSimilarityService, KafkaAggregatorConfig kafkaConfig) {
        this.eventsSimilarityService = eventsSimilarityService;
        this.consumerConfig = kafkaConfig.getConsumer();
        this.producerConfig = kafkaConfig.getProducer();

        this.consumer = new KafkaConsumer<>(consumerConfig.getProperties());
        this.producer = new KafkaProducer<>(producerConfig.getProperties());

        // регистрируем хук, в котором вызываем метод wakeup.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Сработал хук на завершение JVM. Прерываю работу консьюмера.");
            consumer.wakeup();
        }));
    }

    public void start() {
        log.info("Starting Aggregator service...");
        try {
            consumer.subscribe(List.of(consumerConfig.getTopic()));

            while (true) {
                ConsumerRecords<String, UserActionAvro> records = consumer.poll(consumerConfig.getPollTimeout());
                int count = 0;
                for (ConsumerRecord<String, UserActionAvro> record : records) {
                    log.trace("Обработка сообщения от хаба {} из партиции {} с офсетом {}.",
                            record.key(), record.partition(), record.offset());
                    // обрабатываем очередную запись
                    handleRecord(record.value());
                    // фиксируем оффсеты обработанных записей, если нужно
                    manageOffsets(record, count, consumer);
                    count++;
                }
                producer.flush();
                // фиксируем максимальный оффсет обработанных записей
                consumer.commitAsync();
            }
        } catch (WakeupException ignores) {
            // игнорируем - закрываем консьюмер и продюсер в блоке finally
            log.info("Получен сигнал завершения работы. WakeupException. Aggregator. AggregationStarter");
        } catch (Exception e) {
            log.error("Ошибка во время обработки событий от датчиков", e);
        } finally {
            try {
                // Перед тем, как закрыть продюсер и консьюмер, нужно убедиться,
                // что все сообщения, лежащие в буффере, отправлены и
                // все оффсеты обработанных сообщений зафиксированы

                // здесь нужно вызвать метод продюсера для сброса данных в буффере
                producer.flush();
                // здесь нужно вызвать метод консьюмера для фиксиции смещений
                consumer.commitSync(currentOffsets);

            } finally {
                log.info("Закрываем консьюмер");
                consumer.close();
                log.info("Закрываем продюсер");
                producer.close();
            }
        }
    }

    private static void manageOffsets(ConsumerRecord<String, UserActionAvro> record, int count,
                                      KafkaConsumer<String, UserActionAvro> consumer) {
        // обновляем текущий оффсет для топика-партиции
        currentOffsets.put(
                new TopicPartition(record.topic(), record.partition()),
                new OffsetAndMetadata(record.offset() + 1)
        );

        if (count % 100 == 0) {
            consumer.commitAsync(currentOffsets, (offsets, exception) -> {
                if (exception != null) {
                    log.warn("Ошибка во время фиксации оффсетов: {}", offsets, exception);
                }
            });
        }
    }

    private void handleRecord(UserActionAvro userActionAvro) {
        List<EventSimilarityAvro> similarities = eventsSimilarityService.countSimilarity(userActionAvro);
        for (EventSimilarityAvro similarity : similarities) {
            try {
                log.info("Начинаю отправку сообщений {} в топик {}", similarity, producerConfig.getTopic());
                String key = similarity.getEventA() + "_" + similarity.getEventB();
                ProducerRecord<String, EventSimilarityAvro> record = new ProducerRecord<>(
                        producerConfig.getTopic(),
                        null,
                        similarity.getTimestamp().toEpochMilli(),
                        key,
                        similarity);
                log.info("Отправляю сходство {} -> ключ: {}", similarity, key);
                producer.send(record, (metadata, exception) -> {
                    if (exception != null) {
                        log.error("Ошибка отправки сообщения в топик {}", producerConfig.getTopic(), exception);
                    } else {
                        log.info("Сообщение отправлено в топик {} partition {} offset {}",
                                producerConfig.getTopic(), metadata.partition(), metadata.offset());
                    }
                });
                //producer.flush();
            } catch (Exception e) {
                log.error("Ошибка обработки события", e);
            }
        }
    }
}
