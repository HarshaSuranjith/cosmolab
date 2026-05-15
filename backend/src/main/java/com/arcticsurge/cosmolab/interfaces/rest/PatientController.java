package com.arcticsurge.cosmolab.interfaces.rest;

import com.arcticsurge.cosmolab.application.patient.PatientService;
import com.arcticsurge.cosmolab.domain.patient.PatientStatus;
import com.arcticsurge.cosmolab.interfaces.rest.dto.PagedResponse;
import com.arcticsurge.cosmolab.interfaces.rest.dto.PatientRequest;
import com.arcticsurge.cosmolab.interfaces.rest.dto.PatientResponse;
import com.arcticsurge.cosmolab.interfaces.rest.mapper.PatientMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
@Tag(name = "Patients", description = "Patient demographics — register, search, update, discharge")
public class PatientController {

    private final PatientService patientService;
    private final PatientMapper patientMapper;

    @Operation(summary = "List / search patients",
               description = "Paginated list with optional ward, status, and name-fragment filters. Sorted by last name then first name.")
    @ApiResponse(responseCode = "200", description = "Paginated patient list returned successfully")
    @GetMapping
    PagedResponse<PatientResponse> list(
            @Parameter(description = "Filter by ward name, e.g. ICU")
            @RequestParam(required = false) String ward,
            @Parameter(description = "Filter by admission status")
            @RequestParam(required = false) PatientStatus status,
            @Parameter(description = "Name fragment matched against first and last name")
            @RequestParam(required = false) String search,
            @Parameter(description = "Zero-based page index") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, size, Sort.by("lastName", "firstName"));
        return PagedResponse.of(patientService.search(ward, status, search, pageable)
                .map(patientMapper::toResponse));
    }

    @Operation(summary = "Get patient by ID")
    @ApiResponse(responseCode = "200", description = "Patient found")
    @ApiResponse(responseCode = "404", description = "Patient not found",
                 content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/{id}")
    PatientResponse getById(@PathVariable UUID id) {
        return patientMapper.toResponse(patientService.getById(id));
    }

    @Operation(summary = "Register a new patient",
               description = "Creates a patient record. Personal number (personnummer) must be unique across the system.")
    @ApiResponse(responseCode = "201", description = "Patient registered")
    @ApiResponse(responseCode = "400", description = "Validation failed — fieldErrors map in response body",
                 content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Personal number already exists",
                 content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    PatientResponse create(@Valid @RequestBody PatientRequest request) {
        return patientMapper.toResponse(patientService.create(patientMapper.toEntity(request)));
    }

    @Operation(summary = "Update patient demographics")
    @ApiResponse(responseCode = "200", description = "Patient updated")
    @ApiResponse(responseCode = "400", description = "Validation failed",
                 content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Patient not found",
                 content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PutMapping("/{id}")
    PatientResponse update(@PathVariable UUID id, @Valid @RequestBody PatientRequest request) {
        return patientMapper.toResponse(patientService.update(id, request));
    }

    @Operation(summary = "Discharge a patient",
               description = "Soft-delete: sets status to DISCHARGED. Record is retained in full for the audit trail.")
    @ApiResponse(responseCode = "204", description = "Patient discharged")
    @ApiResponse(responseCode = "404", description = "Patient not found",
                 content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void discharge(@PathVariable UUID id) {
        patientService.discharge(id);
    }
}
