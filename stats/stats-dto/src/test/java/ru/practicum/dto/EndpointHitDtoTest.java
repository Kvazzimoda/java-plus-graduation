package ru.practicum.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class EndpointHitDtoTest {

    private ObjectMapper objectMapper;
    private Validator validator;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testValidEndpointHit() {
        EndpointHitDto hit = new EndpointHitDto(
                "ewm-main-service",
                "/events/1",
                "192.163.0.1",
                LocalDateTime.of(2022, 9, 6, 11, 0, 23)
        );

        Set<ConstraintViolation<EndpointHitDto>> violations = validator.validate(hit);

        assertTrue(violations.isEmpty());
        assertEquals("ewm-main-service", hit.getApp());
        assertEquals("/events/1", hit.getUri());
        assertEquals("192.163.0.1", hit.getIp());
        assertNotNull(hit.getTimestamp());
    }

    @Test
    void testInvalidEndpointHit_BlankApp() {
        EndpointHitDto hit = new EndpointHitDto(
                "",
                "/events/1",
                "192.163.0.1",
                LocalDateTime.now()
        );

        Set<ConstraintViolation<EndpointHitDto>> violations = validator.validate(hit);

        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("Идентификатор сервиса"));
    }

    @Test
    void testInvalidEndpointHit_BlankUri() {
        EndpointHitDto hit = new EndpointHitDto(
                "ewm-main-service",
                "",
                "192.163.0.1",
                LocalDateTime.now()
        );

        Set<ConstraintViolation<EndpointHitDto>> violations = validator.validate(hit);

        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("URI"));
    }

    @Test
    void testInvalidEndpointHit_BlankIp() {
        EndpointHitDto hit = new EndpointHitDto(
                "ewm-main-service",
                "/events/1",
                "",
                LocalDateTime.now()
        );

        Set<ConstraintViolation<EndpointHitDto>> violations = validator.validate(hit);

        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("IP-адрес"));
    }

    @Test
    void testInvalidEndpointHit_NullTimestamp() {
        EndpointHitDto hit = new EndpointHitDto(
                "ewm-main-service",
                "/events/1",
                "192.163.0.1",
                null
        );

        Set<ConstraintViolation<EndpointHitDto>> violations = validator.validate(hit);

        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("Время запроса"));
    }

    @Test
    void testJsonSerialization() throws Exception {
        EndpointHitDto hit = new EndpointHitDto(
                "ewm-main-service",
                "/events/1",
                "192.163.0.1",
                LocalDateTime.of(2022, 9, 6, 11, 0, 23)
        );

        String json = objectMapper.writeValueAsString(hit);

        assertTrue(json.contains("\"timestamp\":\"2022-09-06 11:00:23\""));
        assertTrue(json.contains("\"app\":\"ewm-main-service\""));
        assertTrue(json.contains("\"uri\":\"/events/1\""));
        assertTrue(json.contains("\"ip\":\"192.163.0.1\""));
    }

    @Test
    void testJsonDeserialization() throws Exception {
        String json = "{"
                + "\"app\": \"ewm-main-service\","
                + "\"uri\": \"/events/1\","
                + "\"ip\": \"192.163.0.1\","
                + "\"timestamp\": \"2022-09-06 11:00:23\""
                + "}";

        EndpointHitDto hit = objectMapper.readValue(json, EndpointHitDto.class);

        assertEquals("ewm-main-service", hit.getApp());
        assertEquals("/events/1", hit.getUri());
        assertEquals("192.163.0.1", hit.getIp());
        assertEquals(LocalDateTime.of(2022, 9, 6, 11, 0, 23), hit.getTimestamp());
    }

    @Test
    void testNoArgsConstructor() {
        EndpointHitDto hit = new EndpointHitDto();

        assertNotNull(hit);
        assertNull(hit.getApp());
        assertNull(hit.getUri());
        assertNull(hit.getIp());
        assertNull(hit.getTimestamp());
    }

    @Test
    void testEqualsAndHashCode() {
        LocalDateTime timestamp = LocalDateTime.of(2022, 9, 6, 11, 0, 23);
        EndpointHitDto hit1 = new EndpointHitDto("ewm-main-service", "/events/1", "192.163.0.1", timestamp);
        EndpointHitDto hit2 = new EndpointHitDto("ewm-main-service", "/events/1", "192.163.0.1", timestamp);
        EndpointHitDto hit3 = new EndpointHitDto("other-service", "/events/1", "192.163.0.1", timestamp);

        assertEquals(hit1, hit2);
        assertEquals(hit1.hashCode(), hit2.hashCode());
        assertNotEquals(hit1, hit3);
        assertNotEquals(hit1.hashCode(), hit3.hashCode());
    }

    @Test
    void testToString() {
        EndpointHitDto hit = new EndpointHitDto(
                "ewm-main-service",
                "/events/1",
                "192.163.0.1",
                LocalDateTime.of(2022, 9, 6, 11, 0, 23)
        );

        String toString = hit.toString();

        assertTrue(toString.contains("ewm-main-service"));
        assertTrue(toString.contains("/events/1"));
        assertTrue(toString.contains("192.163.0.1"));
    }
}
