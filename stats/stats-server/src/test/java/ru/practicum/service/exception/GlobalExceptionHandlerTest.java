package ru.practicum.service.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    @Mock
    private MethodArgumentNotValidException methodArgumentNotValidException;

    @Mock
    private BindingResult bindingResult;

    @Mock
    private FieldError fieldError;

    @BeforeEach
    void setUp() {
        globalExceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void handleValidationException_withFieldError_shouldReturnFieldErrorMessage() {
        when(methodArgumentNotValidException.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
        when(fieldError.getField()).thenReturn("app");
        when(fieldError.getDefaultMessage()).thenReturn("не может быть пустым");

        GlobalExceptionHandler.ErrorResponse response = globalExceptionHandler
                .handleValidationException(methodArgumentNotValidException);

        assertEquals("app: не может быть пустым", response.error());
    }

    @Test
    void handleValidationException_withNoFieldErrors_shouldReturnDefaultMessage() {
        when(methodArgumentNotValidException.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of());

        GlobalExceptionHandler.ErrorResponse response = globalExceptionHandler
                .handleValidationException(methodArgumentNotValidException);

        assertEquals("Ошибка валидации", response.error());
    }

    @Test
    void handleValidationException_withMultipleFieldErrors_shouldReturnFirstError() {
        FieldError secondFieldError = new FieldError("object", "uri", "не может быть пустым");
        when(methodArgumentNotValidException.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError, secondFieldError));
        when(fieldError.getField()).thenReturn("app");
        when(fieldError.getDefaultMessage()).thenReturn("не может быть пустым");

        GlobalExceptionHandler.ErrorResponse response = globalExceptionHandler
                .handleValidationException(methodArgumentNotValidException);

        assertEquals("app: не может быть пустым", response.error());
    }

    @Test
    void handleIllegalArgumentException_shouldReturnExceptionMessage() {
        IllegalArgumentException exception = new IllegalArgumentException("Время запроса не может быть в будущем");

        GlobalExceptionHandler.ErrorResponse response = globalExceptionHandler
                .handleIllegalArgumentException(exception);

        assertEquals("Время запроса не может быть в будущем", response.error());
    }

    @Test
    void handleIllegalArgumentException_withNullMessage_shouldReturnNullMessage() {
        IllegalArgumentException exception = new IllegalArgumentException();

        GlobalExceptionHandler.ErrorResponse response = globalExceptionHandler
                .handleIllegalArgumentException(exception);

        assertEquals(null, response.error());
    }

    @Test
    void handleDataIntegrityViolation_withNullRootCause_shouldReturnGenericMessage() {
        DataIntegrityViolationException exception = new DataIntegrityViolationException("Тестовое сообщение");

        GlobalExceptionHandler.ErrorResponse response = globalExceptionHandler
                .handleDataIntegrityViolation(exception);

        assertEquals("Ошибка целостности данных", response.error());
    }

    @Test
    void handleDataIntegrityViolation_withNullInRootCause_shouldReturnNullFieldsMessage() {
        RuntimeException rootCause = new RuntimeException("Column 'app' cannot be null");
        DataIntegrityViolationException exception = new DataIntegrityViolationException("Тестовое сообщение", rootCause);

        GlobalExceptionHandler.ErrorResponse response = globalExceptionHandler
                .handleDataIntegrityViolation(exception);

        assertEquals("Обязательные поля не могут быть пустыми", response.error());
    }

    @Test
    void handleDataIntegrityViolation_withUniqueInRootCause_shouldReturnUniqueMessage() {
        RuntimeException rootCause = new RuntimeException("Duplicate entry for unique constraint");
        DataIntegrityViolationException exception = new DataIntegrityViolationException("Тестовое сообщение", rootCause);

        GlobalExceptionHandler.ErrorResponse response = globalExceptionHandler
                .handleDataIntegrityViolation(exception);

        assertEquals("Запись с такими данными уже существует", response.error());
    }

    @Test
    void handleDataIntegrityViolation_withOtherRootCause_shouldReturnGenericMessage() {
        RuntimeException rootCause = new RuntimeException("Какая-то другая ошибка базы данных");
        DataIntegrityViolationException exception = new DataIntegrityViolationException("Тестовое сообщение", rootCause);

        GlobalExceptionHandler.ErrorResponse response = globalExceptionHandler
                .handleDataIntegrityViolation(exception);

        assertEquals("Ошибка целостности данных", response.error());
    }

    @Test
    void errorResponse_shouldCreateCorrectRecord() {
        String errorMessage = "Тестовое сообщение об ошибке";

        GlobalExceptionHandler.ErrorResponse response = new GlobalExceptionHandler.ErrorResponse(errorMessage);

        assertEquals(errorMessage, response.error());
    }

    @Test
    void errorResponse_withNullMessage_shouldHandleNull() {
        String errorMessage = null;

        GlobalExceptionHandler.ErrorResponse response = new GlobalExceptionHandler.ErrorResponse(errorMessage);

        assertEquals(null, response.error());
    }

    @Test
    void errorResponse_equality_shouldWorkCorrectly() {
        GlobalExceptionHandler.ErrorResponse response1 = new GlobalExceptionHandler.ErrorResponse("тест");
        GlobalExceptionHandler.ErrorResponse response2 = new GlobalExceptionHandler.ErrorResponse("тест");
        GlobalExceptionHandler.ErrorResponse response3 = new GlobalExceptionHandler.ErrorResponse("другое");

        assertEquals(response1, response2);
        assertEquals(response1.hashCode(), response2.hashCode());
        assertEquals(response1.toString(), response2.toString());

        assertEquals(false, response1.equals(response3));
    }
}
