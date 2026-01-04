package ru.practicum.ewm.request.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.EwmMapper;
import ru.practicum.ewm.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.ewm.request.dto.ParticipationRequestDto;
import ru.practicum.ewm.request.dto.ParticipationRequestResponse;
import ru.practicum.ewm.request.model.ParticipationRequest;
import ru.practicum.ewm.request.model.RequestStatus;
import ru.practicum.ewm.request.repository.ParticipationRequestRepository;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class RequestPrivateService {

    private final ParticipationRequestRepository requestRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final EwmMapper mapper;

    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        log.info("Получение запросов пользователя userId={}", userId);

        return requestRepository.findAllByRequesterId(userId)
                .stream()
                .map(mapper::toParticipationRequestDto)
                .toList();
    }

    @Transactional
    public ParticipationRequestDto addRequest(Long userId, Long eventId) {
        log.info("Создание запроса userId={}, eventId={}", userId, eventId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Нельзя добавить запрос на участие в своём событии");
        }
        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new ConflictException("Нельзя участвовать в неопубликованном событии");
        }
        if (requestRepository.existsByRequesterIdAndEventId(userId, eventId)) {
            throw new ConflictException("Запрос уже существует");
        }
        long confirmed = requestRepository.countConfirmedRequestsByEventId(eventId);
        if (event.getParticipantLimit() != 0 && confirmed >= event.getParticipantLimit()) {
            throw new ConflictException("Лимит участников исчерпан");
        }

        RequestStatus status;
        if (event.getParticipantLimit() == 0 || !event.getRequestModeration()) {
            status = RequestStatus.CONFIRMED;
        } else {
            status = RequestStatus.PENDING;
        }

        ParticipationRequest request = ParticipationRequest.builder()
                .event(event)
                .requester(user)
                .created(LocalDateTime.now())
                .status(status)
                .build();

        ParticipationRequest saved = requestRepository.save(request);
        return mapper.toParticipationRequestDto(saved);
    }

    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        log.info("Отмена запроса userId={}, requestId={}", userId, requestId);

        ParticipationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Запрос с id=" + requestId + " не найден"));

        if (!request.getRequester().getId().equals(userId)) {
            throw new ConflictException("Пользователь не может отменить чужой запрос");
        }

        request.setStatus(RequestStatus.CANCELED);
        ParticipationRequest saved = requestRepository.save(request);
        return mapper.toParticipationRequestDto(saved);
    }

    public List<ParticipationRequestDto> getRequestsForUserEvent(Long userId, Long eventId) {
        log.info("Получение заявок для события userId={}, eventId={}", userId, eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        if (!event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Пользователь не может просматривать чужие заявки");
        }

        return requestRepository.findAllByEventIdAndEventInitiatorId(eventId, userId)
                .stream()
                .map(mapper::toParticipationRequestDto)
                .toList();
    }

    @Transactional
    public EventRequestStatusUpdateResult changeRequestStatus(Long userId, Long eventId, EventRequestStatusUpdateRequest dto) {
        log.info("Изменение статуса заявок userId={}, eventId={}, requestIds={}", userId, eventId, dto.getRequestIds());

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        if (!event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Пользователь не может изменять заявки чужого события");
        }

        List<ParticipationRequest> requests = requestRepository.findAllById(dto.getRequestIds());

        List<ParticipationRequestResponse> confirmed = new ArrayList<>();
        List<ParticipationRequestResponse> rejected = new ArrayList<>();

        long confirmedCount = requestRepository.countConfirmedRequestsByEventId(eventId);
        long limit = event.getParticipantLimit();

        for (ParticipationRequest request : requests) {
            if (!request.getStatus().equals(RequestStatus.PENDING)) {
                throw new ConflictException("Статус можно изменить только у заявок в ожидании");
            }

            if ("CONFIRMED".equalsIgnoreCase(dto.getStatus())) {
                if (limit != 0 && confirmedCount >= limit) {
                    throw new ConflictException("Достигнут лимит одобренных заявок");
                }
                request.setStatus(RequestStatus.CONFIRMED);
                confirmedCount++;
                confirmed.add(mapper.toParticipationRequestResponse(request));
            } else if ("REJECTED".equalsIgnoreCase(dto.getStatus())) {
                request.setStatus(RequestStatus.REJECTED);
                rejected.add(mapper.toParticipationRequestResponse(request));
            } else {
                throw new IllegalArgumentException("Неверный статус: " + dto.getStatus());
            }

            requestRepository.save(request);
        }

        // Если лимит достигнут, отклоняем все оставшиеся PENDING заявки
        if (limit != 0 && confirmedCount >= limit) {
            List<ParticipationRequest> pendingRequests = requestRepository.findAllByEventIdAndEventInitiatorId(eventId, userId)
                    .stream()
                    .filter(r -> r.getStatus().equals(RequestStatus.PENDING))
                    .toList();

            for (ParticipationRequest pending : pendingRequests) {
                pending.setStatus(RequestStatus.REJECTED);
                rejected.add(mapper.toParticipationRequestResponse(pending));
                requestRepository.save(pending);
            }
        }

        EventRequestStatusUpdateResult result = new EventRequestStatusUpdateResult();
        result.setConfirmedRequests(confirmed);
        result.setRejectedRequests(rejected);

        return result;
    }

}

