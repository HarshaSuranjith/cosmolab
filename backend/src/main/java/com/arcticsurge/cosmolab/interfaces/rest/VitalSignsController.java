package com.arcticsurge.cosmolab.interfaces.rest;

import com.arcticsurge.cosmolab.application.observation.VitalSignsService;
import com.arcticsurge.cosmolab.interfaces.rest.dto.VitalSignsRequest;
import com.arcticsurge.cosmolab.interfaces.rest.dto.VitalSignsResponse;
import com.arcticsurge.cosmolab.interfaces.rest.mapper.VitalSignsMapper;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Vital Signs", description = "Vital signs observations — openEHR OBSERVATION archetype. Recorded under a composition within an EHR.")
public class VitalSignsController {

    private final VitalSignsService vitalSignsService;
    private final VitalSignsMapper vitalSignsMapper;

    @Operation(summary = "Record vital signs",
               description = "Creates a vital signs observation under the given composition. recordedAt defaults to the current timestamp if omitted.")
    @ApiResponse(responseCode = "201", description = "Vital signs recorded")
    @ApiResponse(responseCode = "400", description = "Validation failed",
                 content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "EHR or composition not found",
                 content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/api/v1/ehr/{ehrId}/compositions/{compositionId}/vitals")
    @ResponseStatus(HttpStatus.CREATED)
    VitalSignsResponse record(
            @Parameter(description = "EHR identifier") @PathVariable UUID ehrId,
            @Parameter(description = "Composition identifier") @PathVariable UUID compositionId,
            @Valid @RequestBody VitalSignsRequest request) {
        return vitalSignsMapper.toResponse(
                vitalSignsService.record(vitalSignsMapper.toEntity(request, compositionId)));
    }

    @Operation(summary = "List vital signs for an EHR",
               description = "Returns all observations within the given time range, ordered by recorded time descending.")
    @ApiResponse(responseCode = "200", description = "Vital signs list returned")
    @ApiResponse(responseCode = "404", description = "EHR not found",
                 content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/api/v1/ehr/{ehrId}/vitals")
    List<VitalSignsResponse> list(
            @PathVariable UUID ehrId,
            @Parameter(description = "Start of time range (ISO 8601 Instant), defaults to epoch")
            @RequestParam(required = false) Instant from,
            @Parameter(description = "End of time range (ISO 8601 Instant), defaults to now")
            @RequestParam(required = false) Instant to) {
        Instant start = from != null ? from : Instant.EPOCH;
        Instant end = to != null ? to : Instant.now();
        return vitalSignsService.listByEhr(ehrId, start, end).stream()
                .map(vitalSignsMapper::toResponse).toList();
    }

    @Operation(summary = "Get latest vital signs for an EHR",
               description = "Returns the most recent observation. Returns 204 No Content if no observations have been recorded.")
    @ApiResponse(responseCode = "200", description = "Latest vitals returned")
    @ApiResponse(responseCode = "204", description = "No observations recorded yet")
    @ApiResponse(responseCode = "404", description = "EHR not found",
                 content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/api/v1/ehr/{ehrId}/vitals/latest")
    ResponseEntity<VitalSignsResponse> latest(@PathVariable UUID ehrId) {
        return vitalSignsService.getLatest(ehrId)
                .map(vitalSignsMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.<VitalSignsResponse>noContent().build());
    }
}
