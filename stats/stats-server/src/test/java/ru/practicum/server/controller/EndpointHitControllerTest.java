package ru.practicum.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import ru.practicum.stats.dto.dto.EndpointHitDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import ru.practicum.server.service.EndpointHitService;

import java.time.LocalDateTime;

@WebMvcTest(EndpointHitController.class)
public class EndpointHitControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EndpointHitService endpointHitService;

    private EndpointHitDto endpointHitDto;

    @BeforeEach
    void setUp() {
        endpointHitDto = EndpointHitDto
                .builder()
                .uri("/test/uri")
                .app("test-application")
                .ip("1111.0000.1111.0000")
                .timestamp(LocalDateTime.of(1999, 1, 1, 11, 11))
                .build();
    }

    @Test
    void saveEndpointHit() throws Exception {
        Mockito.when(endpointHitService.save(endpointHitDto)).thenReturn(endpointHitDto);

        mockMvc.perform(MockMvcRequestBuilders
                        .post("/hit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(endpointHitDto))
                )
                .andExpect(MockMvcResultMatchers.status().isCreated());
    }
}
