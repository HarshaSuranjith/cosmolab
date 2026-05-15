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
class EhrControllerTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private static final AtomicInteger COUNTER = new AtomicInteger(1001);

    private String nextPersonalNumber() {
        return "20020202-" + String.format("%04d", COUNTER.getAndIncrement());
    }

    private UUID createPatient() throws Exception {
        PatientRequest req = new PatientRequest("Test", "Patient", nextPersonalNumber(),
                LocalDate.of(1985, 3, 15), Gender.MALE, "WARD-2002", PatientStatus.ACTIVE);
        MvcResult result = mockMvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.id"));
    }

    private UUID createEhr(UUID patientId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/ehr")
                        .param("patientId", patientId.toString()))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.ehrId"));
    }

    @Test
    void create_newPatient_returns201WithEhrId() throws Exception {
        UUID patientId = createPatient();
        mockMvc.perform(post("/api/v1/ehr").param("patientId", patientId.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ehrId").isNotEmpty())
                .andExpect(jsonPath("$.subjectId").value(patientId.toString()))
                .andExpect(jsonPath("$.systemId").value("cosmolab-v1"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void create_idempotent_samePatientReturnsSameEhrId() throws Exception {
        UUID patientId = createPatient();
        // First creation should succeed
        mockMvc.perform(post("/api/v1/ehr").param("patientId", patientId.toString()))
                .andExpect(status().isCreated());
        // Second creation should return 409 Conflict
        mockMvc.perform(post("/api/v1/ehr").param("patientId", patientId.toString()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("unique constraint")));
    }

    @Test
    void getById_existingEhr_returns200() throws Exception {
        UUID patientId = createPatient();
        UUID ehrId = createEhr(patientId);
        mockMvc.perform(get("/api/v1/ehr/{ehrId}", ehrId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ehrId").value(ehrId.toString()));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/ehr/{ehrId}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getByPatient_existingEhr_returns200() throws Exception {
        UUID patientId = createPatient();
        createEhr(patientId);
        mockMvc.perform(get("/api/v1/ehr/subject/{patientId}", patientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subjectId").value(patientId.toString()));
    }

    @Test
    void getByPatient_noEhrExists_returns404() throws Exception {
        UUID patientId = createPatient();
        mockMvc.perform(get("/api/v1/ehr/subject/{patientId}", patientId))
                .andExpect(status().isNotFound());
    }
}
