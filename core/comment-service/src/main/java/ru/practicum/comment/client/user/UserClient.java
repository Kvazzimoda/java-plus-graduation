package ru.practicum.comment.client.user;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.comment.dto.response.user.UserDto;

import java.util.List;

@FeignClient(name = "user-service", fallback = UserServiceClientFallback.class)
public interface UserClient {

    @GetMapping("/admin/users/{id}")
    UserDto getUserById(@PathVariable("id") Long id);

    @GetMapping("/admin/users")
    List<UserDto> getUsers(@RequestParam(name = "ids", required = false) List<Long> ids);
}
