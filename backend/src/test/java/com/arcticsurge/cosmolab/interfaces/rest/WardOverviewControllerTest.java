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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class WardOverviewControllerTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private static final String TEST_WARD = "WARD-2006";
    private static final AtomicInteger COUNTER = new AtomicInteger(1001);

    private String nextPersonalNumber() {
        return "20060606-" + String.format("%04d", COUNTER.getAndIncrement());
    }

    private UUID createActivePatientInWard() throws Exception {
        PatientRequest req = new PatientRequest("Ward", "Patient", nextPersonalNumber(),
                LocalDate.of(1980, 6, 6), Gender.FEMALE, TEST_WARD, PatientStatus.ACTIVE);
        MvcResult result = mockMvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated()).andReturn();
        return UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.id"));
    }

    private UUID createEhr(UUID patientId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/ehr").param("patientId", patientId.toString()))
                .andExpect(status().isCreated()).andReturn();
        return UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.ehrId"));
    }

    private UUID createComposition(UUID ehrId) throws Exception {
        CompositionRequest req = new CompositionRequest(
                CompositionType.ENCOUNTER_NOTE, UUID.randomUUID(), Instant.now(), "Test Hospital");
        MvcResult result = mockMvc.perform(post("/api/v1/ehr/{ehrId}/compositions", ehrId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated()).andReturn();
        return UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.id"));
    }

    @Test
    void overview_emptyWard_returnsZeroPatients() throws Exception {
        mockMvc.perform(get("/api/v1/ward/{wardId}/overview", "EMPTY-WARD-XYZ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wardId").value("EMPTY-WARD-XYZ"))
                .andExpect(jsonPath("$.patientCount").value(0))
                .andExpect(jsonPath("$.patients").isEmpty());
    }

    @Test
    void overview_activePatientWithEhr_returnsPatientSummary() throws Exception {
        UUID patientId = createActivePatientInWard();
        createEhr(patientId);

        mockMvc.perform(get("/api/v1/ward/{wardId}/overview", TEST_WARD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wardId").value(TEST_WARD))
                .andExpect(jsonPath("$.patientCount", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.patients[*].patientId", hasItem(patientId.toString())));
    }

    @Test
    void overview_patientWithAbnormalVitals_returnsFlagsInSummary() throws Exception {
        UUID patientId = createActivePatientInWard();
        UUID ehrId = createEhr(patientId);
        UUID compositionId = createComposition(ehrId);

        // systolicBp=160 > 140 triggers systolicBP:HIGH flag
        VitalSignsRequest abnormal = new VitalSignsRequest(
                Instant.now(), UUID.randomUUID(),
                160, 90, 72, 16,
                new BigDecimal("36.8"), new BigDecimal("98.5"), new BigDecimal("70.0"));
        mockMvc.perform(post("/api/v1/ehr/{ehrId}/compositions/{compositionId}/vitals", ehrId, compositionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(abnormal)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/ward/{wardId}/overview", TEST_WARD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patients[?(@.patientId=='" + patientId + "')].flags",
                        hasItem(hasItem("systolicBP:HIGH"))));
    }

    @Test
    void overview_dischargedPatient_notIncluded() throws Exception {
        // Create and immediately discharge a patient in this ward
        UUID patientId = createActivePatientInWard();
        mockMvc.perform(delete("/api/v1/patients/{id}", patientId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/ward/{wardId}/overview", TEST_WARD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patients[*].patientId", not(hasItem(patientId.toString()))));
    }
}
