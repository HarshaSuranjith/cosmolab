package com.arcticsurge.cosmolab.interfaces.rest;

import com.arcticsurge.cosmolab.AbstractIntegrationTest;
import com.arcticsurge.cosmolab.domain.composition.CompositionType;
import com.arcticsurge.cosmolab.domain.evaluation.ProblemStatus;
import com.arcticsurge.cosmolab.domain.evaluation.Severity;
import com.arcticsurge.cosmolab.domain.patient.Gender;
import com.arcticsurge.cosmolab.domain.patient.PatientStatus;
import com.arcticsurge.cosmolab.interfaces.rest.dto.CompositionRequest;
import com.arcticsurge.cosmolab.interfaces.rest.dto.PatientRequest;
import com.arcticsurge.cosmolab.interfaces.rest.dto.ProblemRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class ProblemListControllerTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private static final AtomicInteger COUNTER = new AtomicInteger(1001);
    private UUID ehrId;
    private UUID compositionId;

    private String nextPersonalNumber() {
        return "20050505-" + String.format("%04d", COUNTER.getAndIncrement());
    }

    @BeforeEach
    void setupComposition() throws Exception {
        PatientRequest patReq = new PatientRequest("Problem", "Patient", nextPersonalNumber(),
                LocalDate.of(1970, 5, 5), Gender.MALE, "WARD-2005", PatientStatus.ACTIVE);
        MvcResult patientResult = mockMvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patReq)))
                .andExpect(status().isCreated()).andReturn();
        UUID patientId = UUID.fromString(JsonPath.read(patientResult.getResponse().getContentAsString(), "$.id"));

        MvcResult ehrResult = mockMvc.perform(post("/api/v1/ehr").param("patientId", patientId.toString()))
                .andExpect(status().isCreated()).andReturn();
        ehrId = UUID.fromString(JsonPath.read(ehrResult.getResponse().getContentAsString(), "$.ehrId"));

        CompositionRequest compReq = new CompositionRequest(
                CompositionType.ENCOUNTER_NOTE, UUID.randomUUID(), Instant.now(), "Test Hospital");
        MvcResult compResult = mockMvc.perform(post("/api/v1/ehr/{ehrId}/compositions", ehrId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(compReq)))
                .andExpect(status().isCreated()).andReturn();
        compositionId = UUID.fromString(JsonPath.read(compResult.getResponse().getContentAsString(), "$.id"));
    }

    private UUID createProblem(String icd10Code, Severity severity) throws Exception {
        ProblemRequest req = new ProblemRequest(
                compositionId, icd10Code, "Test diagnosis: " + icd10Code, severity,
                UUID.randomUUID(), LocalDate.of(2024, 1, 1));
        MvcResult result = mockMvc.perform(post("/api/v1/ehr/{ehrId}/problems", ehrId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated()).andReturn();
        return UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.id"));
    }

    @Test
    void create_validProblem_returns201WithActiveStatus() throws Exception {
        ProblemRequest req = new ProblemRequest(
                compositionId, "J18.9", "Community-acquired pneumonia", Severity.MODERATE,
                UUID.randomUUID(), LocalDate.of(2024, 1, 1));
        mockMvc.perform(post("/api/v1/ehr/{ehrId}/problems", ehrId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.icd10Code").value("J18.9"))
                .andExpect(jsonPath("$.severity").value("MODERATE"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.ehrId").value(ehrId.toString()));
    }

    @Test
    void create_missingIcd10Code_returns400() throws Exception {
        ProblemRequest invalid = new ProblemRequest(
                compositionId, "", "Some diagnosis", Severity.MILD, UUID.randomUUID(), null);
        mockMvc.perform(post("/api/v1/ehr/{ehrId}/problems", ehrId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.icd10Code").isNotEmpty());
    }

    @Test
    void list_noStatusFilter_returnsAllProblems() throws Exception {
        createProblem("A00.0", Severity.MILD);
        createProblem("B01.0", Severity.SEVERE);
        mockMvc.perform(get("/api/v1/ehr/{ehrId}/problems", ehrId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(2)));
    }

    @Test
    void list_statusFilterActive_returnsOnlyActiveProblems() throws Exception {
        createProblem("C00.0", Severity.MODERATE);
        mockMvc.perform(get("/api/v1/ehr/{ehrId}/problems", ehrId).param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].status", everyItem(is("ACTIVE"))));
    }

    @Test
    void getById_existingProblem_returns200() throws Exception {
        UUID problemId = createProblem("D00.0", Severity.MILD);
        mockMvc.perform(get("/api/v1/ehr/{ehrId}/problems/{id}", ehrId, problemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(problemId.toString()));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/ehr/{ehrId}/problems/{id}", ehrId, UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_changesSeverity_returns200() throws Exception {
        UUID problemId = createProblem("E00.0", Severity.MILD);
        ProblemRequest updated = new ProblemRequest(
                compositionId, "E00.0", "Updated diagnosis", Severity.SEVERE, UUID.randomUUID(), null);
        mockMvc.perform(put("/api/v1/ehr/{ehrId}/problems/{id}", ehrId, problemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.severity").value("SEVERE"))
                .andExpect(jsonPath("$.displayName").value("Updated diagnosis"));
    }
}
