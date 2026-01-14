package ru.practicum.core.service.service;

import org.springframework.data.domain.Pageable;
import ru.practicum.core.dto.UserDto;
import ru.practicum.core.service.dto.request.user.NewUserRequest;


import java.util.List;

public interface UserService {
    List<UserDto> getUsers(List<Long> ids, Pageable pageable);

    UserDto addUser(NewUserRequest newUserRequest);

    void deleteUser(Long id);

    UserDto getUser(Long id);
}
