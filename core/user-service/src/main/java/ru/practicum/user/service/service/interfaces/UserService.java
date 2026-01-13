package ru.practicum.user.service.service.interfaces;

import org.springframework.data.domain.Pageable;
import ru.practicum.user.service.dto.request.user.NewUserRequest;
import ru.practicum.user.service.dto.response.user.UserDto;


import java.util.List;

public interface UserService {
    List<UserDto> getUsers(List<Long> ids, Pageable pageable);

    UserDto addUser(NewUserRequest newUserRequest);

    void deleteUser(Long id);

    UserDto getUser(Long id);
}
