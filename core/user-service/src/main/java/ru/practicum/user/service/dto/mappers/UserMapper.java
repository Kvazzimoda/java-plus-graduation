package ru.practicum.user.service.dto.mappers;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.practicum.user.service.dto.request.user.NewUserRequest;
import ru.practicum.user.service.dto.response.user.UserDto;
import ru.practicum.user.service.dto.response.user.UserShortDto;
import ru.practicum.user.service.model.User;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UserMapper {
    public static User toEntity(NewUserRequest newUserRequest) {
        return User.builder()
                .id(0L)
                .name(newUserRequest.getName())
                .email(newUserRequest.getEmail())
                .build();
    }

    public static UserDto toDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }

    public static UserShortDto toUserShortDto(User user) {
        return UserShortDto.builder()
                .id(user.getId())
                .name(user.getName())
                .build();
    }
}
