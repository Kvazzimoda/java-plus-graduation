package ru.practicum.request.exception;

public class OwnershipMismatchException extends RuntimeException {
    public OwnershipMismatchException(String message) {
        super(message);
    }
}
