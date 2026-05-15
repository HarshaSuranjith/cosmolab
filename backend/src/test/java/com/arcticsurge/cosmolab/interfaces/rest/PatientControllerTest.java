package com.arcticsurge.cosmolab.interfaces.rest;

import com.arcticsurge.cosmolab.AbstractIntegrationTest;
import com.arcticsurge.cosmolab.domain.patient.Gender;
import com.arcticsurge.cosmolab.domain.patient.PatientStatus;
import com.arcticsurge.cosmolab.interfaces.rest.dto.PatientRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class PatientControllerTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private static final AtomicInteger COUNTER = new AtomicInteger(1001);
    private static final String TEST_WARD = "WARD-2001";

    private String nextPersonalNumber() {
        return "20010101-" + String.format("%04d", COUNTER.getAndIncrement());
    }

    private UUID createPatient(String personalNumber) throws Exception {
        PatientRequest req = new PatientRequest("Anna", "Lindström", personalNumber,
                LocalDate.of(1985, 3, 15), Gender.FEMALE, TEST_WARD, PatientStatus.ACTIVE);
        MvcResult result = mockMvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.id"));
    }

    @Test
    void list_returnsPagedResponse() throws Exception {
        mockMvc.perform(get("/api/v1/patients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.page").value(0));
    }

    @Test
    void list_filterByWard_returnsOnlyMatchingPatients() throws Exception {
        createPatient(nextPersonalNumber());
        mockMvc.perform(get("/api/v1/patients").param("ward", TEST_WARD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].ward").value(TEST_WARD));
    }

    @Test
    void create_validRequest_returns201WithPatientId() throws Exception {
        PatientRequest req = new PatientRequest("Erik", "Svensson", nextPersonalNumber(),
                LocalDate.of(1990, 6, 20), Gender.MALE, TEST_WARD, PatientStatus.ACTIVE);
        mockMvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.firstName").value("Erik"))
                .andExpect(jsonPath("$.lastName").value("Svensson"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void create_blankFirstName_returns400WithFieldError() throws Exception {
        PatientRequest req = new PatientRequest("", "Svensson", nextPersonalNumber(),
                LocalDate.of(1990, 6, 20), Gender.MALE, TEST_WARD, PatientStatus.ACTIVE);
        mockMvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.firstName").isNotEmpty());
    }

    @Test
    void create_invalidPersonalNumberFormat_returns400() throws Exception {
        PatientRequest req = new PatientRequest("Erik", "Svensson", "NOT-VALID",
                LocalDate.of(1990, 6, 20), Gender.MALE, TEST_WARD, PatientStatus.ACTIVE);
        mockMvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.personalNumber").isNotEmpty());
    }

    @Test
    void create_duplicatePersonalNumber_returns409() throws Exception {
        String personalNumber = nextPersonalNumber();
        createPatient(personalNumber);
        PatientRequest dup = new PatientRequest("Karin", "Johansson", personalNumber,
                LocalDate.of(1975, 1, 1), Gender.FEMALE, TEST_WARD, PatientStatus.ACTIVE);
        mockMvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dup)))
                .andExpect(status().isConflict());
    }

    @Test
    void getById_existingPatient_returns200() throws Exception {
        UUID id = createPatient(nextPersonalNumber());
        mockMvc.perform(get("/api/v1/patients/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/patients/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_existingPatient_returns200WithUpdatedName() throws Exception {
        UUID id = createPatient(nextPersonalNumber());
        PatientRequest updated = new PatientRequest("UpdatedFirst", "UpdatedLast", nextPersonalNumber(),
                LocalDate.of(1985, 3, 15), Gender.FEMALE, "WARD-UPDATED", PatientStatus.ACTIVE);
        mockMvc.perform(put("/api/v1/patients/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("UpdatedFirst"))
                .andExpect(jsonPath("$.ward").value("WARD-UPDATED"));
    }

    @Test
    void discharge_existingPatient_returns204() throws Exception {
        UUID id = createPatient(nextPersonalNumber());
        mockMvc.perform(delete("/api/v1/patients/{id}", id))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/patients/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISCHARGED"));
    }
}
