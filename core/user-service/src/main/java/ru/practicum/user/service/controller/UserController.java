package ru.practicum.user.service.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.user.service.dto.request.user.NewUserRequest;
import ru.practicum.user.service.dto.response.user.UserDto;
import ru.practicum.user.service.service.interfaces.UserService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@Validated
public class UserController {
    private final UserService service;

    //Получение пользователей
    @GetMapping("/admin/users")
    public List<UserDto> getUsers(@RequestParam(required = false) List<Long> ids,
                                  @PositiveOrZero @RequestParam(name = "from", defaultValue = "0") Integer from,
                                  @Positive @RequestParam(name = "size", defaultValue = "10") Integer size) {
        Pageable pageable = PageRequest.of(from, size);
        return service.getUsers(ids, pageable);
    }

    //Добавление пользователя
    @PostMapping("/admin/users")
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto addUser(@Valid @RequestBody NewUserRequest newUserRequest) {
        return service.addUser(newUserRequest);
    }

    //Удаление пользователя
    @DeleteMapping("/admin/users/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long userId) {
        service.deleteUser(userId);
    }

    //Возвращение пользователя по его id
    @GetMapping("/admin/users/{id}")
    public UserDto getUser(@PathVariable("id") Long userId) {
        return service.getUser(userId);
    }

}
