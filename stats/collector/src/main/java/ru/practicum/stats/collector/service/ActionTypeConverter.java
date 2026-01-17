package ru.practicum.stats.collector.service;

import ru.practicum.stats.avro.ActionTypeAvro;
import ru.practicum.stats.proto.ActionTypeProto;

public class ActionTypeConverter {

    public static ActionTypeAvro convert(ActionTypeProto proto) {
        if (proto == null) {
            return null;
        }

        switch (proto) {
            case ACTION_VIEW:
                return ActionTypeAvro.VIEW;
            case ACTION_REGISTER:
                return ActionTypeAvro.REGISTER;
            case ACTION_LIKE:
                return ActionTypeAvro.LIKE;
            default:
                throw new IllegalArgumentException("Unknown ActionTypeProto: " + proto);
        }
    }
}
