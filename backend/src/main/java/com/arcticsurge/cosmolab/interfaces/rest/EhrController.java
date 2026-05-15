package com.arcticsurge.cosmolab.interfaces.rest;

import com.arcticsurge.cosmolab.application.ehr.EhrService;
import com.arcticsurge.cosmolab.interfaces.rest.dto.EhrResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ehr")
@RequiredArgsConstructor
@Tag(name = "EHR", description = "Electronic Health Record root — one EHR per patient, openEHR EHR_ID")
public class EhrController {

    private final EhrService ehrService;

    @Operation(summary = "Create an EHR for a patient",
               description = "Each patient has exactly one EHR (enforced by unique constraint on subject_id).")
    @ApiResponse(responseCode = "201", description = "EHR created")
    @ApiResponse(responseCode = "404", description = "Patient not found",
                 content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "EHR already exists for this patient",
                 content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    EhrResponse create(@RequestParam UUID patientId) {
        return EhrResponse.from(ehrService.create(patientId));
    }

    @Operation(summary = "Get EHR by EHR ID")
    @ApiResponse(responseCode = "200", description = "EHR found")
    @ApiResponse(responseCode = "404", description = "EHR not found",
                 content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/{ehrId}")
    EhrResponse getById(@PathVariable UUID ehrId) {
        return EhrResponse.from(ehrService.getById(ehrId));
    }

    @Operation(summary = "Get EHR by patient ID")
    @ApiResponse(responseCode = "200", description = "EHR found")
    @ApiResponse(responseCode = "404", description = "No EHR exists for this patient",
                 content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/subject/{patientId}")
    EhrResponse getByPatient(@PathVariable UUID patientId) {
        return EhrResponse.from(ehrService.getByPatientId(patientId));
    }
}
