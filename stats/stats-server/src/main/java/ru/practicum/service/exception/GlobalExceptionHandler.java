package ru.practicum.service.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidationException(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .findFirst()
                .orElse("Ошибка валидации");

        log.warn("Ошибка валидации: {}", errorMessage, ex);
        return new ErrorResponse(errorMessage);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Некорректные аргументы: {}", ex.getMessage(), ex);
        return new ErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        String rootMsg = ex.getRootCause() != null ?
                ex.getRootCause().getMessage() :
                "Ошибка при сохранении данных";

        String userMsg = rootMsg.contains("null") ?
                "Обязательные поля не могут быть пустыми" :
                rootMsg.contains("unique") ?
                        "Запись с такими данными уже существует" :
                        "Ошибка целостности данных";

        log.error("Ошибка целостности данных: {}", rootMsg, ex);
        return new ErrorResponse(userMsg);
    }

    public record ErrorResponse(String error) {
    }
}