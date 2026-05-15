package com.arcticsurge.cosmolab.interfaces.rest;

import com.arcticsurge.cosmolab.application.composition.CompositionService;
import com.arcticsurge.cosmolab.domain.composition.Composition;
import com.arcticsurge.cosmolab.domain.composition.CompositionType;
import com.arcticsurge.cosmolab.interfaces.rest.dto.CompositionRequest;
import com.arcticsurge.cosmolab.interfaces.rest.dto.CompositionResponse;
import com.arcticsurge.cosmolab.interfaces.rest.dto.PagedResponse;
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
@RequestMapping("/api/v1/ehr/{ehrId}/compositions")
@RequiredArgsConstructor
@Tag(name = "Compositions", description = "Clinical document containers — openEHR COMPOSITION archetype. Each composition holds entries (observations or evaluations).")
public class CompositionController {

    private final CompositionService compositionService;

    @Operation(summary = "Create a composition",
               description = "Creates a clinical document under the given EHR. The type determines which entry archetypes can be nested inside.")
    @ApiResponse(responseCode = "201", description = "Composition created")
    @ApiResponse(responseCode = "400", description = "Validation failed",
                 content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "EHR not found",
                 content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    CompositionResponse create(@PathVariable UUID ehrId, @Valid @RequestBody CompositionRequest request) {
        Composition c = new Composition();
        c.setEhrId(ehrId);
        c.setType(request.type());
        c.setAuthorId(request.authorId());
        c.setStartTime(request.startTime());
        c.setFacilityName(request.facilityName());
        return CompositionResponse.from(compositionService.create(c));
    }

    @Operation(summary = "List compositions for an EHR",
               description = "Paginated, sorted by commit time descending. Optionally filter by composition type.")
    @ApiResponse(responseCode = "200", description = "Composition list returned")
    @ApiResponse(responseCode = "404", description = "EHR not found",
                 content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping
    PagedResponse<CompositionResponse> list(
            @PathVariable UUID ehrId,
            @Parameter(description = "Filter by composition type") @RequestParam(required = false) CompositionType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "commitTime"));
        return PagedResponse.of(compositionService.listByEhr(ehrId, type, pageable)
                .map(CompositionResponse::from));
    }

    @Operation(summary = "Get a composition by ID")
    @ApiResponse(responseCode = "200", description = "Composition found")
    @ApiResponse(responseCode = "404", description = "Composition or EHR not found",
                 content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/{id}")
    CompositionResponse getById(@PathVariable UUID ehrId, @PathVariable UUID id) {
        return CompositionResponse.from(compositionService.getById(id, ehrId));
    }

    @Operation(summary = "Update a composition")
    @ApiResponse(responseCode = "200", description = "Composition updated")
    @ApiResponse(responseCode = "400", description = "Validation failed",
                 content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Composition or EHR not found",
                 content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PutMapping("/{id}")
    CompositionResponse update(@PathVariable UUID ehrId, @PathVariable UUID id,
                               @Valid @RequestBody CompositionRequest request) {
        Composition updated = new Composition();
        updated.setType(request.type());
        updated.setStartTime(request.startTime());
        updated.setFacilityName(request.facilityName());
        return CompositionResponse.from(compositionService.update(id, ehrId, updated));
    }
}
