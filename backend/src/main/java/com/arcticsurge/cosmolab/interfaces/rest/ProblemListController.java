package com.arcticsurge.cosmolab.interfaces.rest;

import com.arcticsurge.cosmolab.application.evaluation.ProblemListService;
import com.arcticsurge.cosmolab.domain.evaluation.ProblemListEntry;
import com.arcticsurge.cosmolab.domain.evaluation.ProblemStatus;
import com.arcticsurge.cosmolab.interfaces.rest.dto.ProblemRequest;
import com.arcticsurge.cosmolab.interfaces.rest.dto.ProblemResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ehr/{ehrId}/problems")
@RequiredArgsConstructor
public class ProblemListController {

    private final ProblemListService problemListService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ProblemResponse create(@PathVariable UUID ehrId, @Valid @RequestBody ProblemRequest request) {
        ProblemListEntry entry = new ProblemListEntry();
        entry.setEhrId(ehrId);
        entry.setCompositionId(request.compositionId());
        entry.setIcd10Code(request.icd10Code());
        entry.setDisplayName(request.displayName());
        entry.setSeverity(request.severity());
        entry.setRecordedBy(request.recordedBy());
        entry.setOnsetDate(request.onsetDate());
        return ProblemResponse.from(problemListService.create(entry));
    }

    @GetMapping
    List<ProblemResponse> list(@PathVariable UUID ehrId,
                               @RequestParam(required = false) ProblemStatus status) {
        return problemListService.listByEhr(ehrId, status).stream()
                .map(ProblemResponse::from).toList();
    }

    @GetMapping("/{id}")
    ProblemResponse getById(@PathVariable UUID ehrId, @PathVariable UUID id) {
        return ProblemResponse.from(problemListService.getById(id));
    }

    @PutMapping("/{id}")
    ProblemResponse update(@PathVariable UUID ehrId, @PathVariable UUID id,
                           @Valid @RequestBody ProblemRequest request) {
        ProblemListEntry updated = new ProblemListEntry();
        updated.setDisplayName(request.displayName());
        updated.setSeverity(request.severity());
        return ProblemResponse.from(problemListService.update(id, updated));
    }
}
