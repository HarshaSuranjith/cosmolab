package com.arcticsurge.cosmolab.interfaces.rest;

import com.arcticsurge.cosmolab.AbstractIntegrationTest;
import com.arcticsurge.cosmolab.domain.composition.CompositionType;
import com.arcticsurge.cosmolab.domain.patient.Gender;
import com.arcticsurge.cosmolab.domain.patient.PatientStatus;
import com.arcticsurge.cosmolab.interfaces.rest.dto.CompositionRequest;
import com.arcticsurge.cosmolab.interfaces.rest.dto.PatientRequest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class CompositionControllerTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private static final AtomicInteger COUNTER = new AtomicInteger(1001);
    private UUID ehrId;

    private String nextPersonalNumber() {
        return "20030303-" + String.format("%04d", COUNTER.getAndIncrement());
    }

    @BeforeEach
    void setupEhr() throws Exception {
        PatientRequest req = new PatientRequest("Test", "Patient", nextPersonalNumber(),
                LocalDate.of(1985, 3, 15), Gender.MALE, "WARD-2003", PatientStatus.ACTIVE);
        MvcResult patientResult = mockMvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated()).andReturn();
        UUID patientId = UUID.fromString(JsonPath.read(patientResult.getResponse().getContentAsString(), "$.id"));

        MvcResult ehrResult = mockMvc.perform(post("/api/v1/ehr").param("patientId", patientId.toString()))
                .andExpect(status().isCreated()).andReturn();
        ehrId = UUID.fromString(JsonPath.read(ehrResult.getResponse().getContentAsString(), "$.ehrId"));
    }

    private UUID createComposition(CompositionType type) throws Exception {
        CompositionRequest req = new CompositionRequest(type, UUID.randomUUID(), Instant.now(), "Karolinska");
        MvcResult result = mockMvc.perform(post("/api/v1/ehr/{ehrId}/compositions", ehrId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated()).andReturn();
        return UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.id"));
    }

    @Test
    void create_validRequest_returns201WithCompositionId() throws Exception {
        CompositionRequest req = new CompositionRequest(
                CompositionType.ENCOUNTER_NOTE, UUID.randomUUID(), Instant.now(), "Karolinska");
        mockMvc.perform(post("/api/v1/ehr/{ehrId}/compositions", ehrId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.ehrId").value(ehrId.toString()))
                .andExpect(jsonPath("$.type").value("ENCOUNTER_NOTE"))
                .andExpect(jsonPath("$.status").value("COMPLETE"));
    }

    @Test
    void create_missingType_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/ehr/{ehrId}/compositions", ehrId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"facilityName\":\"Hospital\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void list_returnsPagedCompositions() throws Exception {
        createComposition(CompositionType.ENCOUNTER_NOTE);
        mockMvc.perform(get("/api/v1/ehr/{ehrId}/compositions", ehrId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    @Test
    void list_filterByType_returnsOnlyMatchingType() throws Exception {
        createComposition(CompositionType.ADMISSION);
        createComposition(CompositionType.PROGRESS_NOTE);
        mockMvc.perform(get("/api/v1/ehr/{ehrId}/compositions", ehrId)
                        .param("type", "ADMISSION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].type",
                        org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("ADMISSION"))));
    }

    @Test
    void getById_existingComposition_returns200() throws Exception {
        UUID compositionId = createComposition(CompositionType.ENCOUNTER_NOTE);
        mockMvc.perform(get("/api/v1/ehr/{ehrId}/compositions/{id}", ehrId, compositionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(compositionId.toString()));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/ehr/{ehrId}/compositions/{id}", ehrId, UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_changesType_returns200() throws Exception {
        UUID compositionId = createComposition(CompositionType.ENCOUNTER_NOTE);
        CompositionRequest updated = new CompositionRequest(
                CompositionType.PROGRESS_NOTE, UUID.randomUUID(), Instant.now(), "Updated Hospital");
        mockMvc.perform(put("/api/v1/ehr/{ehrId}/compositions/{id}", ehrId, compositionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("PROGRESS_NOTE"))
                .andExpect(jsonPath("$.facilityName").value("Updated Hospital"));
    }
}
