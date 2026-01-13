package ru.practicum.user.service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.user.service.dto.mappers.UserMapper;
import ru.practicum.user.service.dto.request.user.NewUserRequest;
import ru.practicum.user.service.dto.response.user.UserDto;
import ru.practicum.user.service.exception.NotFoundException;
import ru.practicum.user.service.model.User;
import ru.practicum.user.service.repository.UserRepository;
import ru.practicum.user.service.service.interfaces.UserService;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImp implements UserService {
    private final UserRepository repository;

    //Получение пользователей
    @Override
    public List<UserDto> getUsers(List<Long> ids, Pageable pageable) {
        if (ids == null || ids.isEmpty()) {
            List<UserDto> users = repository.findAll(pageable).stream()
                    .map(UserMapper::toDto)
                    .toList();
            return users;
        }
        List<UserDto> users = repository.findAllById(ids).stream()
                .map(UserMapper::toDto)
                .toList();
        return users;
    }

    //Добавление пользователя
    @Override
    @Transactional
    public UserDto addUser(NewUserRequest newUserRequest) {
        log.info("Добавление нового пользователя " + newUserRequest);
        User user = UserMapper.toEntity(newUserRequest);
        User savedUser = repository.save(user);
        log.info("Пользователь добавлен: {}", savedUser);
        return UserMapper.toDto(savedUser);
    }

    //Удаление пользователя
    @Override
    @Transactional
    public void deleteUser(Long userId) {
        log.info("Попытка удаления пользователя по ID: {}", userId);
        if (!repository.existsById(userId)) {
            throw new NotFoundException("User with id=" + userId + " was not found");
        }
        repository.deleteById(userId);
        log.info("Пользователь с ID: {} удален", userId);
    }

    //Возвращение пользователя
    @Override
    public UserDto getUser(Long userId) {
        log.info("Возвращаем пользователя по ID: {}", userId);
        User user = repository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь c userId " + userId + " не найден"));
        log.info("Пользователь с ID: {} найден и отправлен", userId);
        return UserMapper.toDto(user);
    }
}
