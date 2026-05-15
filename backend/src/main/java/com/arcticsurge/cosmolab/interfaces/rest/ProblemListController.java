package com.arcticsurge.cosmolab.interfaces.rest;

import com.arcticsurge.cosmolab.application.evaluation.ProblemListService;
import com.arcticsurge.cosmolab.domain.evaluation.ProblemStatus;
import com.arcticsurge.cosmolab.interfaces.rest.dto.ProblemRequest;
import com.arcticsurge.cosmolab.interfaces.rest.dto.ProblemResponse;
import com.arcticsurge.cosmolab.interfaces.rest.mapper.ProblemMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ehr/{ehrId}/problems")
@RequiredArgsConstructor
@Tag(name = "Problem List", description = "Problem list entries — openEHR EVALUATION archetype. ICD-10 coded diagnoses with severity and status lifecycle (ACTIVE → RESOLVED / REFUTED).")
public class ProblemListController {

    private final ProblemListService problemListService;
    private final ProblemMapper problemMapper;

    @Operation(summary = "Add a problem to the patient's problem list")
    @ApiResponse(responseCode = "201", description = "Problem entry created")
    @ApiResponse(responseCode = "400", description = "Validation failed",
                 content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "EHR not found",
                 content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ProblemResponse create(@PathVariable UUID ehrId, @Valid @RequestBody ProblemRequest request) {
        return problemMapper.toResponse(
                problemListService.create(problemMapper.toEntity(request, ehrId)));
    }

    @Operation(summary = "List problems for an EHR",
               description = "Returns all problem entries, optionally filtered by status. Filter by ACTIVE for the ward dashboard view.")
    @ApiResponse(responseCode = "200", description = "Problem list returned")
    @ApiResponse(responseCode = "404", description = "EHR not found",
                 content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping
    List<ProblemResponse> list(
            @PathVariable UUID ehrId,
            @Parameter(description = "Filter by problem status — omit to return all statuses")
            @RequestParam(required = false) ProblemStatus status) {
        return problemListService.listByEhr(ehrId, status).stream()
                .map(problemMapper::toResponse).toList();
    }

    @Operation(summary = "Get a problem entry by ID")
    @ApiResponse(responseCode = "200", description = "Problem entry found")
    @ApiResponse(responseCode = "404", description = "Problem entry not found",
                 content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/{id}")
    ProblemResponse getById(@PathVariable UUID ehrId, @PathVariable UUID id) {
        return problemMapper.toResponse(problemListService.getById(id));
    }

    @Operation(summary = "Update a problem entry",
               description = "Updates display name and severity. Set status to RESOLVED or REFUTED to close the problem.")
    @ApiResponse(responseCode = "200", description = "Problem entry updated")
    @ApiResponse(responseCode = "400", description = "Validation failed",
                 content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Problem entry not found",
                 content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PutMapping("/{id}")
    ProblemResponse update(@PathVariable UUID ehrId, @PathVariable UUID id,
                           @Valid @RequestBody ProblemRequest request) {
        return problemMapper.toResponse(problemListService.update(id, request));
    }
}
