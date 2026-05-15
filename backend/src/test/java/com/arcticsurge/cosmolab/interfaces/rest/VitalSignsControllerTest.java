package com.arcticsurge.cosmolab.interfaces.rest;

import com.arcticsurge.cosmolab.AbstractIntegrationTest;
import com.arcticsurge.cosmolab.domain.composition.CompositionType;
import com.arcticsurge.cosmolab.domain.patient.Gender;
import com.arcticsurge.cosmolab.domain.patient.PatientStatus;
import com.arcticsurge.cosmolab.interfaces.rest.dto.CompositionRequest;
import com.arcticsurge.cosmolab.interfaces.rest.dto.PatientRequest;
import com.arcticsurge.cosmolab.interfaces.rest.dto.VitalSignsRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class VitalSignsControllerTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private static final AtomicInteger COUNTER = new AtomicInteger(1001);
    private UUID ehrId;
    private UUID compositionId;

    private String nextPersonalNumber() {
        return "20040404-" + String.format("%04d", COUNTER.getAndIncrement());
    }

    @BeforeEach
    void setupComposition() throws Exception {
        PatientRequest patReq = new PatientRequest("Vitals", "Patient", nextPersonalNumber(),
                LocalDate.of(1985, 3, 15), Gender.FEMALE, "WARD-2004", PatientStatus.ACTIVE);
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

    private VitalSignsRequest normalVitals() {
        return new VitalSignsRequest(Instant.now(), UUID.randomUUID(),
                120, 80, 72, 16, new BigDecimal("36.8"), new BigDecimal("98.5"), new BigDecimal("72.0"));
    }

    @Test
    void record_validVitals_returns201WithAllFields() throws Exception {
        mockMvc.perform(post("/api/v1/ehr/{ehrId}/compositions/{compositionId}/vitals", ehrId, compositionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(normalVitals())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.compositionId").value(compositionId.toString()))
                .andExpect(jsonPath("$.systolicBp").value(120))
                .andExpect(jsonPath("$.heartRate").value(72))
                .andExpect(jsonPath("$.temperature").value(36.8));
    }

    @Test
    void record_missingRecordedBy_returns400() throws Exception {
        VitalSignsRequest invalid = new VitalSignsRequest(
                Instant.now(), null, 120, 80, 72, 16,
                new BigDecimal("36.8"), new BigDecimal("98.5"), new BigDecimal("72.0"));
        mockMvc.perform(post("/api/v1/ehr/{ehrId}/compositions/{compositionId}/vitals", ehrId, compositionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.recordedBy").isNotEmpty());
    }

    @Test
    void list_afterRecording_returnsVitalSigns() throws Exception {
        mockMvc.perform(post("/api/v1/ehr/{ehrId}/compositions/{compositionId}/vitals", ehrId, compositionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(normalVitals())))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/ehr/{ehrId}/vitals", ehrId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].compositionId").value(compositionId.toString()));
    }

    @Test
    void latest_afterRecording_returns200WithMostRecent() throws Exception {
        mockMvc.perform(post("/api/v1/ehr/{ehrId}/compositions/{compositionId}/vitals", ehrId, compositionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(normalVitals())))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/ehr/{ehrId}/vitals/latest", ehrId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.systolicBp").value(120));
    }

    @Test
    void latest_noVitalsRecorded_returns204() throws Exception {
        // Create a fresh EHR with no vitals
        PatientRequest patReq = new PatientRequest("NoVitals", "Patient", nextPersonalNumber(),
                LocalDate.of(1990, 1, 1), Gender.MALE, "WARD-2004", PatientStatus.ACTIVE);
        MvcResult patResult = mockMvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patReq)))
                .andExpect(status().isCreated()).andReturn();
        UUID freshPatientId = UUID.fromString(JsonPath.read(patResult.getResponse().getContentAsString(), "$.id"));

        MvcResult ehrResult = mockMvc.perform(post("/api/v1/ehr").param("patientId", freshPatientId.toString()))
                .andExpect(status().isCreated()).andReturn();
        UUID freshEhrId = UUID.fromString(JsonPath.read(ehrResult.getResponse().getContentAsString(), "$.ehrId"));

        mockMvc.perform(get("/api/v1/ehr/{ehrId}/vitals/latest", freshEhrId))
                .andExpect(status().isNoContent());
    }
}
