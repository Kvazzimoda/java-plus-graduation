package ru.practicum.comment.client.user;

import org.springframework.stereotype.Component;
import ru.practicum.comment.dto.response.user.UserDto;

import java.util.List;

@Component
public class UserServiceClientFallback implements UserClient {
    @Override
    public UserDto getUserById(Long id) {
        throw new RuntimeException("Fallback response: User service is unavailable");
    }

    @Override
    public List<UserDto> getUsers(List<Long> ids) {
        throw new RuntimeException("Fallback response: User service is unavailable");
    }
}
