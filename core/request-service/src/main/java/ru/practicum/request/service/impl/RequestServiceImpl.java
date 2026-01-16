package ru.practicum.request.service.impl;

import com.google.protobuf.Timestamp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import org.springframework.transaction.support.TransactionTemplate;
import ru.practicum.core.client.EventClient;
import ru.practicum.core.dto.EventDto;
import ru.practicum.request.dto.mappers.RequestMapper;
import ru.practicum.request.dto.response.request.ParticipationRequestDto;

import ru.practicum.request.exception.ConflictException;
import ru.practicum.request.exception.NotFoundException;
import ru.practicum.request.exception.OwnershipMismatchException;
import ru.practicum.request.model.Request;
import ru.practicum.request.repository.RequestRepository;
import ru.practicum.request.service.RequestService;
import ru.practicum.core.client.UserClient;
import ru.practicum.core.dto.UserDto;
import ru.practicum.stats.client.CollectorClient;
import ru.practicum.stats.proto.ActionTypeProto;
import ru.practicum.stats.proto.UserActionProto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class RequestServiceImpl implements RequestService {

    private final TransactionTemplate transactionTemplate;
    private final UserClient userClient;
    private final EventClient eventClient;
    private final RequestRepository requestRepository;
    private final CollectorClient collectorClient;

    @Override
    public List<ParticipationRequestDto> getRequestsByRequesterId(Long userId) {
        return RequestMapper.toParticipationRequestsDto(requestRepository.findAllByRequesterId(userId));
    }

    @Override
    public ParticipationRequestDto cancel(Long userId, Long requestId) {
        getUserById(userId);
        Request request = getRequestById(requestId);
        if (!request.getRequesterId().equals(userId)) {
            throw new OwnershipMismatchException("пользователь " + userId + " не отправлял запрос " + request.getId());
        }
        request.setStatus(Request.RequestStatus.CANCELED);
        return RequestMapper.toParticipationRequestDto(requestRepository.save(request));
    }

    @Override
    public ParticipationRequestDto addRequest(Long userId, Long eventId) {
        if (eventId == null) {
            throw new IllegalArgumentException("параметр eventId обязателен");
        }

        EventDto event = getEventById(eventId);
        UserDto user = getUserById(userId);

        if (requestRepository.existsByRequesterIdAndEventId(userId, eventId)) {
            throw new ConflictException("запрос на участие в событии " + eventId + " уже создан");
        }
        if (event.getInitiatorId().equals(userId)) {
            throw new ConflictException(userId + " является инициатором события");
        }
        if (event.getState() != EventDto.EventState.PUBLISHED) {
            throw new ConflictException("нельзя участвовать в неопубликованном событии");
        }
        if (!event.getRequestModeration()) {
            int confirmedRequests = requestRepository.countConfirmedRequestsByEventId(eventId);
            if (confirmedRequests >= event.getParticipantLimit() && event.getParticipantLimit() != 0) {
                throw new ConflictException("Достигнут лимит подтверждённых участников");
            }
        }

        // Определяем статус ДО транзакции как финальную переменную
        final Request.RequestStatus finalRequestStatus;
        if (event.getParticipantLimit() == 0) {
            finalRequestStatus = Request.RequestStatus.CONFIRMED;
        } else {
            finalRequestStatus = event.getRequestModeration()
                    ? Request.RequestStatus.PENDING
                    : Request.RequestStatus.CONFIRMED;
        }

        // Создаем объект и сохраняем в транзакции
        Request request = transactionTemplate.execute(transactionStatus -> {
            Request newRequest = Request.builder()
                    .requesterId(user.getId())
                    .eventId(eventId)
                    .created(LocalDateTime.now())
                    .status(finalRequestStatus)  // используем финальную переменную
                    .build();

            return requestRepository.save(newRequest);
        });

        // Статистика
        try {
            collectorClient.sendUserAction(
                    createUserAction(eventId, userId, ActionTypeProto.ACTION_REGISTER)
            );
        } catch (Exception e) {
            log.warn("Не удалось отправить статистику, продолжаем без неё", e);
        }

        return RequestMapper.toParticipationRequestDto(request);
    }

    private UserDto getUserById(Long userId) {
        //Получаем пользователя через клиент
        try {
            UserDto user = userClient.getUserById(userId);
            log.debug("Existing User received from user-service: {}", user);
            return user;
        } catch (Exception e) {
            log.debug("Failed to get user from user-service: {}", e.getMessage());
            throw new NotFoundException("Пользователь c userId " + userId + " не найден");
        }
    }

    private EventDto getEventById(Long eventId) {
        try {
            EventDto event = eventClient.getEventById(eventId);
            log.debug("Existing Event received from event-service: {}", event);
            return event;
        } catch (Exception e) {
            log.debug("Failed to get event from event-service: {}", e.getMessage());
            throw new NotFoundException("Событие c eventId " + eventId + " не найдено");
        }
    }

    private Request getRequestById(Long requestId) {
        return requestRepository.findById(requestId).orElseThrow(() ->
                new NotFoundException("запрос с id " + requestId + " не найден"));
    }

    UserActionProto createUserAction(Long eventId, Long userId, ActionTypeProto typeProto) {
        Instant timestamp = Instant.now();
        return UserActionProto.newBuilder()
                .setUserId(userId)
                .setEventId(eventId)
                .setActionType(typeProto)
                .setTimestamp(Timestamp.newBuilder()
                        .setSeconds(timestamp.getEpochSecond())
                        .setNanos(timestamp.getNano())
                        .build())
                .build();
    }


}
