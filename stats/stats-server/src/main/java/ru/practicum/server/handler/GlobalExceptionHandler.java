package ru.practicum.server.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalArgument(final IllegalArgumentException exception) {
        log.warn("illegal argument", exception);
        return new ErrorResponse("illegal argument", exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ErrorResponse handleException(final Exception exception) {
        log.warn("неизвестная ошибка", exception);
        return new ErrorResponse("неизвестная ошибка", exception.getMessage());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMissingParams(MissingServletRequestParameterException e) {
        String name = e.getParameterName();
        log.warn("Required parameter '" + name + "' is missing", e);
        return new ErrorResponse("Bad Request", "Required request parameter '" + name + "' is not present");
    }
}
