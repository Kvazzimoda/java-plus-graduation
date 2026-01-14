package ru.practicum.stats.collector.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.practicum.stats.avro.UserActionAvro;
import ru.practicum.stats.proto.UserActionProto;

import java.time.Instant;

@Slf4j
@Service
public class UserActionHandlerImpl implements UserActionHandler {

    protected final KafkaEventProducer kafkaEventProducer;
    protected final String topic;

    public UserActionHandlerImpl(KafkaEventProducer kafkaEventProducer,
                                 @Value("${kafka.topic.user-action}") String topic) {
        this.kafkaEventProducer = kafkaEventProducer;
        this.topic = topic;
        log.info("UserActionHandlerImpl initialized with topic: {}", topic);
    }

    @Override
    public void handle(UserActionProto event) {
        if (event == null) {
            log.error("Received null event");
            return;
        }

        try {
            UserActionAvro avroEvent = mapToAvro(event);
            Producer<String, SpecificRecordBase> producer = kafkaEventProducer.getProducer();
            log.info("Начинаю отправку сообщений {} в топик {}", avroEvent, topic);

            ProducerRecord<String, SpecificRecordBase> record = new ProducerRecord<>(topic, avroEvent);
            producer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    log.error("Ошибка отправки сообщения в топик {}", topic, exception);
                } else {
                    log.info("Сообщение отправлено в топик {} partition {} offset {}",
                            topic, metadata.partition(), metadata.offset());
                }
            });
            producer.flush();
            log.info("Event processed successfully: userId={}, eventId={}, actionType={}",
                    event.getUserId(), event.getEventId(), event.getActionType());
        } catch (Exception e) {
            log.error("Ошибка обработки события", e);
        }
    }

    protected UserActionAvro mapToAvro(UserActionProto event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }

        // Получаем timestamp, если он есть
        Instant timestamp = event.hasTimestamp()
                ? convertTimestampToInstant(event.getTimestamp())
                : Instant.now();

        return UserActionAvro.newBuilder()
                .setUserId(event.getUserId())
                .setEventId(event.getEventId())
                .setActionType(ActionTypeConverter.convert(event.getActionType()))
                .setTimestamp(timestamp)
                .build();
    }

    // Метод для конвертации protobuf Timestamp в Instant
    private Instant convertTimestampToInstant(com.google.protobuf.Timestamp timestamp) {
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
    }
}
