package ru.practicum.request.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.request.dto.mappers.RequestMapper;
import ru.practicum.request.dto.request.request.RequestStatusUpdateDto;
import ru.practicum.request.dto.response.request.ConfirmedRequestsCountDto;
import ru.practicum.request.dto.response.request.RequestDto;
import ru.practicum.request.exception.ConflictException;
import ru.practicum.request.exception.NotFoundException;
import ru.practicum.request.model.Request;
import ru.practicum.request.repository.RequestRepository;
import ru.practicum.request.service.interfaces.RequestInternalService;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequestInternalServiceImpl implements RequestInternalService {
    private final RequestRepository requestRepository;

    @Override
    public Integer countConfirmedRequestsByEventId(Long eventId) {
        log.debug("Подсчет подтвержденных запросов для события: {}", eventId);
        return requestRepository.countConfirmedRequestsByEventId(eventId);
    }

    @Override
    public List<ConfirmedRequestsCountDto> countConfirmedRequestsByEventIds(List<Long> eventIds) {
        log.debug("Подсчет подтвержденных запросов для событий: {}", eventIds);
        return eventIds.stream()
                .map(eventId -> ConfirmedRequestsCountDto.builder()
                        .eventId(eventId)
                        .count(Long.valueOf(requestRepository.countConfirmedRequestsByEventId(eventId)))
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<RequestDto> getRequestsByEventId(Long eventId) {
        log.debug("Получение запросов для события: {}", eventId);
        List<Request> requests = requestRepository.findAllByEventId(eventId);
        return RequestMapper.toDtoList(requests);
    }

    @Override
    public List<RequestDto> findAllByRequesterId(Long requesterId) {
        log.debug("Получение запросов для пользователя: {}", requesterId);
        List<Request> requests = requestRepository.findAllByRequesterId(requesterId);
        return RequestMapper.toDtoList(requests);
    }

    @Override
    public Boolean existsByRequesterIdAndEventId(Long requesterId, Long eventId) {
        log.debug("Проверка существования запроса для пользователя {} и события {}", requesterId, eventId);
        return requestRepository.existsByRequesterIdAndEventId(requesterId, eventId);
    }

    @Override
    public Integer countByEventId(Long eventId) {
        log.debug("Подсчет всех запросов для события: {}", eventId);
        return requestRepository.countByEventId(eventId);
    }

    @Override
    public List<RequestDto> findAllByIdInAndEventId(List<Long> ids, Long eventId) {
        log.debug("Получение запросов по IDs {} и события {}", ids, eventId);
        List<Request> requests = requestRepository.findAllByIdInAndEventId(ids, eventId);
        return RequestMapper.toDtoList(requests);
    }

    @Override
    @Transactional
    public void updateRequestsStatus(RequestStatusUpdateDto updateDto) {
        log.debug("Обновление статусов запросов: {}", updateDto);

        List<Long> requestIds = updateDto.getRequestIds();
        String status = updateDto.getStatus();

        // Валидация статуса
        Request.RequestStatus requestStatus;
        try {
            requestStatus = Request.RequestStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new ConflictException("Неверный статус: " + status);
        }

        // Получаем запросы
        List<Request> requests = requestRepository.findAllByIdIn(requestIds);

        // Проверяем что все запросы найдены
        if (requests.size() != requestIds.size()) {
            throw new NotFoundException("Некоторые запросы не найдены");
        }

        // Обновляем статусы
        requests.forEach(request -> request.setStatus(requestStatus));

        // Сохраняем
        requestRepository.saveAll(requests);
        log.info("Обновлены статусы {} запросов на {}", requests.size(), status);
    }
}
