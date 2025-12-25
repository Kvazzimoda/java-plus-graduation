package ru.practicum.service.model;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class EndpointHitTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void createEndpointHit_withValidData_shouldPassValidation() {
        EndpointHit hit = EndpointHit.builder()
                .id(1L)
                .app("ewm-main-service")
                .uri("/events/1")
                .ip("192.168.1.1")
                .timestamp(LocalDateTime.of(2023, 10, 15, 12, 30, 0))
                .build();

        Set<ConstraintViolation<EndpointHit>> violations = validator.validate(hit);

        assertTrue(violations.isEmpty());
    }

    @Test
    void createEndpointHit_withBlankApp_shouldFailValidation() {
        EndpointHit hit = EndpointHit.builder()
                .app("")
                .uri("/events/1")
                .ip("192.168.1.1")
                .timestamp(LocalDateTime.now())
                .build();

        Set<ConstraintViolation<EndpointHit>> violations = validator.validate(hit);

        assertEquals(1, violations.size());
        ConstraintViolation<EndpointHit> violation = violations.iterator().next();
        assertEquals("Идентификатор сервиса не может быть пустым", violation.getMessage());
    }

    @Test
    void createEndpointHit_withNullTimestamp_shouldFailValidation() {
        EndpointHit hit = EndpointHit.builder()
                .app("ewm-main-service")
                .uri("/events/1")
                .ip("192.168.1.1")
                .timestamp(null)
                .build();

        Set<ConstraintViolation<EndpointHit>> violations = validator.validate(hit);

        assertEquals(1, violations.size());
        ConstraintViolation<EndpointHit> violation = violations.iterator().next();
        assertEquals("Время запроса не может быть пустым", violation.getMessage());
    }

    @Test
    void equals_withDifferentId_shouldReturnFalse() {
        EndpointHit hit1 = EndpointHit.builder().id(1L).build();
        EndpointHit hit2 = EndpointHit.builder().id(2L).build();

        assertNotEquals(hit1, hit2);
    }

    @Test
    void equals_withNull_shouldReturnFalse() {
        EndpointHit hit = EndpointHit.builder().id(1L).build();

        assertNotEquals(hit, null);
    }

    @Test
    void equals_withDifferentClass_shouldReturnFalse() {
        EndpointHit hit = EndpointHit.builder().id(1L).build();
        String other = "не EndpointHit";

        assertNotEquals(hit, other);
    }

    @Test
    void equals_withNullId_shouldReturnFalse() {
        EndpointHit hit1 = EndpointHit.builder().id(null).build();
        EndpointHit hit2 = EndpointHit.builder().id(1L).build();

        assertNotEquals(hit1, hit2);
    }

    @Test
    void equals_withSameObject_shouldReturnTrue() {
        EndpointHit hit = EndpointHit.builder().id(1L).build();

        assertEquals(hit, hit);
    }
}
