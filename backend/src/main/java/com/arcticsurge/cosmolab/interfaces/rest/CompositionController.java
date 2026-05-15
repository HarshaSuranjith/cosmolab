package com.arcticsurge.cosmolab.interfaces.rest;

import com.arcticsurge.cosmolab.application.composition.CompositionService;
import com.arcticsurge.cosmolab.domain.composition.Composition;
import com.arcticsurge.cosmolab.domain.composition.CompositionType;
import com.arcticsurge.cosmolab.interfaces.rest.dto.CompositionRequest;
import com.arcticsurge.cosmolab.interfaces.rest.dto.CompositionResponse;
import com.arcticsurge.cosmolab.interfaces.rest.dto.PagedResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ehr/{ehrId}/compositions")
@RequiredArgsConstructor
public class CompositionController {

    private final CompositionService compositionService;

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

    @GetMapping
    PagedResponse<CompositionResponse> list(
            @PathVariable UUID ehrId,
            @RequestParam(required = false) CompositionType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "commitTime"));
        return PagedResponse.of(compositionService.listByEhr(ehrId, type, pageable)
                .map(CompositionResponse::from));
    }

    @GetMapping("/{id}")
    CompositionResponse getById(@PathVariable UUID ehrId, @PathVariable UUID id) {
        return CompositionResponse.from(compositionService.getById(id, ehrId));
    }

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
