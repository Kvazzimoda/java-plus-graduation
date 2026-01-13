package ru.practicum.comment.exception;

public class OwnershipMismatchException extends RuntimeException {
    public OwnershipMismatchException(String message) {
        super(message);
    }
}
