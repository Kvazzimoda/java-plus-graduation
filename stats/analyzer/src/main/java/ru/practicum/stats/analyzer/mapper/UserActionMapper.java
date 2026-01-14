package ru.practicum.stats.analyzer.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.stats.analyzer.model.UserAction;
import ru.practicum.stats.avro.ActionTypeAvro;
import ru.practicum.stats.avro.UserActionAvro;

import java.time.LocalDateTime;
import java.time.ZoneId;

@UtilityClass
public class UserActionMapper {
    private static final Double VIEW_RATING = 0.4;
    private static final Double REGISTER_RATING = 0.8;
    private static final Double LIKE_RATING = 1.0;

    public static UserAction toEntity(UserActionAvro avro) {
        if (avro == null) {
            return null;
        }

        return UserAction.builder()
                .userId(avro.getUserId())
                .eventId(avro.getEventId())
                .rating(mapActionTypeToRating(avro.getActionType()))
                .timestamp(LocalDateTime.ofInstant(avro.getTimestamp(), ZoneId.systemDefault()))
                .build();
    }

    private static Double mapActionTypeToRating(ActionTypeAvro actionType) {

        return switch (actionType) {
            case VIEW -> VIEW_RATING;
            case REGISTER -> REGISTER_RATING;
            case LIKE -> LIKE_RATING;
        };
    }
}
