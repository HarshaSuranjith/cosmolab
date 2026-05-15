package com.arcticsurge.cosmolab.interfaces.rest;

import com.arcticsurge.cosmolab.application.patient.PatientNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GlobalExceptionHandlerTest {

    private static final UUID FIXED_ID = UUID.fromString("00000000-0000-0000-0000-000000000042");

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new StubController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .defaultResponseCharacterEncoding(java.nio.charset.StandardCharsets.UTF_8)
                .build();
    }

    @Test
    void entityNotFoundException_returns404() throws Exception {
        mockMvc.perform(get("/test/not-found").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void entityNotFoundException_returns404WithDetailMessage() throws Exception {
        mockMvc.perform(get("/test/not-found").accept(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail")
                        .value("Patient 00000000-0000-0000-0000-000000000042 not found"));
    }

    @Test
    void dataIntegrityViolation_returns409() throws Exception {
        mockMvc.perform(get("/test/conflict").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void unexpectedException_returns500WithoutInternalDetail() throws Exception {
        mockMvc.perform(get("/test/error").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                // internal message must not leak to the client
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred"));
    }

    // Minimal stub controller that triggers each exception type
    @RestController
    static class StubController {

        @GetMapping("/test/not-found")
        void notFound() {
            throw new PatientNotFoundException(FIXED_ID);
        }

        @GetMapping("/test/conflict")
        void conflict() {
            throw new DataIntegrityViolationException("unique constraint violated");
        }

        @GetMapping("/test/error")
        void error() {
            throw new RuntimeException("internal DB password leaked here");
        }
    }
}
