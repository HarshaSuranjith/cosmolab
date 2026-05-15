package com.arcticsurge.cosmolab.interfaces.rest;

import com.arcticsurge.cosmolab.application.patient.PatientService;
import com.arcticsurge.cosmolab.domain.patient.Patient;
import com.arcticsurge.cosmolab.domain.patient.PatientStatus;
import com.arcticsurge.cosmolab.interfaces.rest.dto.PagedResponse;
import com.arcticsurge.cosmolab.interfaces.rest.dto.PatientRequest;
import com.arcticsurge.cosmolab.interfaces.rest.dto.PatientResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;

    @GetMapping
    PagedResponse<PatientResponse> list(
            @RequestParam(required = false) String ward,
            @RequestParam(required = false) PatientStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, size, Sort.by("lastName", "firstName"));
        return PagedResponse.of(patientService.search(ward, status, search, pageable)
                .map(PatientResponse::from));
    }

    @GetMapping("/{id}")
    PatientResponse getById(@PathVariable UUID id) {
        return PatientResponse.from(patientService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    PatientResponse create(@Valid @RequestBody PatientRequest request) {
        Patient patient = toEntity(request);
        return PatientResponse.from(patientService.create(patient));
    }

    @PutMapping("/{id}")
    PatientResponse update(@PathVariable UUID id, @Valid @RequestBody PatientRequest request) {
        return PatientResponse.from(patientService.update(id, toEntity(request)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void discharge(@PathVariable UUID id) {
        patientService.discharge(id);
    }

    private Patient toEntity(PatientRequest r) {
        Patient p = new Patient();
        p.setFirstName(r.firstName());
        p.setLastName(r.lastName());
        p.setPersonalNumber(r.personalNumber());
        p.setDateOfBirth(r.dateOfBirth());
        p.setGender(r.gender());
        p.setWard(r.ward());
        p.setStatus(r.status());
        return p;
    }
}
