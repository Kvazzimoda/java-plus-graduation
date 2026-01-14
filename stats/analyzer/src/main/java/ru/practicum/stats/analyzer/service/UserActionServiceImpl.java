package ru.practicum.stats.analyzer.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.stats.analyzer.mapper.UserActionMapper;
import ru.practicum.stats.analyzer.model.UserAction;
import ru.practicum.stats.analyzer.repository.UserInteractionRepository;
import ru.practicum.stats.avro.UserActionAvro;

@Slf4j
@Service
@AllArgsConstructor
@Transactional
public class UserActionServiceImpl implements UserActionService {

    private final UserInteractionRepository userInteractionRepository;

    @Override
    public void save(UserActionAvro userActionAvro) {
        log.info("Сохраняем действие {} пользователя с id {} для события c id {}",
                userActionAvro.getActionType(),
                userActionAvro.getUserId(),
                userActionAvro.getEventId());
        UserAction newUserAction = UserActionMapper.toEntity(userActionAvro);
        userInteractionRepository
                .findByUserIdAndEventId(newUserAction.getUserId(), newUserAction.getEventId())
                .ifPresentOrElse(
                        existingAction -> updateExistingAction(existingAction, newUserAction),
                        () -> createNewAction(newUserAction)
                );
    }

    private void updateExistingAction(UserAction existingAction, UserAction newAction) {
        // Проверяем, что новый рейтинг выше текущего
        if (newAction.getRating() > existingAction.getRating()) {
            log.debug("Обновляем рейтинг для пользователя {} и события {}: {} -> {}",
                    existingAction.getUserId(),
                    existingAction.getEventId(),
                    existingAction.getRating(),
                    newAction.getRating());

            existingAction.setRating(newAction.getRating());
            existingAction.setTimestamp(newAction.getTimestamp());
            userInteractionRepository.save(existingAction);
        } else {
            log.debug("Рейтинг не обновлен: текущий {} >= нового {} для пользователя {} и события {}",
                    existingAction.getRating(),
                    newAction.getRating(),
                    existingAction.getUserId(),
                    existingAction.getEventId());
        }
    }

    private void createNewAction(UserAction newAction) {
        log.debug("Создаем новую запись взаимодействия для пользователя {} и события {} с рейтингом {}",
                newAction.getUserId(),
                newAction.getEventId(),
                newAction.getRating());

        userInteractionRepository.save(newAction);
    }
}