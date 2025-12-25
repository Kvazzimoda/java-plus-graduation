package ru.practicum.ewm.user.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.EwmMapper;
import ru.practicum.ewm.user.dto.NewUserRequest;
import ru.practicum.ewm.user.dto.UserDto;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EwmMapper mapper;

    @InjectMocks
    private UserService userService;

    @Test
    void createUser_Success_ShouldReturnUserDto() {
        NewUserRequest request = new NewUserRequest("test@example.com", "Test User");
        User user = User.builder().id(1L).email("test@example.com").name("Test User").build();
        UserDto userDto = new UserDto(1L, "Test User", "test@example.com");

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(mapper.toUser(request)).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);
        when(mapper.toUserDto(user)).thenReturn(userDto);

        UserDto result = userService.createUser(request);

        assertEquals(userDto, result);
        verify(userRepository).save(user);
    }

    @Test
    void deleteUser_UserNotFound_ShouldThrowNotFoundException() {
        Long userId = 1L;

        when(userRepository.existsById(userId)).thenReturn(false);

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> userService.deleteUser(userId));

        assertEquals("Пользователь с ID 1 не найден", exception.getMessage());
        verify(userRepository, never()).deleteById(any());
    }
}
