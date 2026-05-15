package com.arcticsurge.cosmolab.interfaces.rest;

import com.arcticsurge.cosmolab.application.ehr.EhrService;
import com.arcticsurge.cosmolab.interfaces.rest.dto.EhrResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ehr")
@RequiredArgsConstructor
public class EhrController {

    private final EhrService ehrService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    EhrResponse create(@RequestParam UUID patientId) {
        return EhrResponse.from(ehrService.create(patientId));
    }

    @GetMapping("/{ehrId}")
    EhrResponse getById(@PathVariable UUID ehrId) {
        return EhrResponse.from(ehrService.getById(ehrId));
    }

    @GetMapping("/subject/{patientId}")
    EhrResponse getByPatient(@PathVariable UUID patientId) {
        return EhrResponse.from(ehrService.getByPatientId(patientId));
    }
}
