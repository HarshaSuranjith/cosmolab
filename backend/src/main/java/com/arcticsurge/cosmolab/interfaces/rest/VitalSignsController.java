package com.arcticsurge.cosmolab.interfaces.rest;

import com.arcticsurge.cosmolab.application.observation.VitalSignsService;
import com.arcticsurge.cosmolab.domain.observation.VitalSigns;
import com.arcticsurge.cosmolab.interfaces.rest.dto.VitalSignsRequest;
import com.arcticsurge.cosmolab.interfaces.rest.dto.VitalSignsResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class VitalSignsController {

    private final VitalSignsService vitalSignsService;

    @PostMapping("/api/v1/ehr/{ehrId}/compositions/{compositionId}/vitals")
    @ResponseStatus(HttpStatus.CREATED)
    VitalSignsResponse record(@PathVariable UUID ehrId,
                              @PathVariable UUID compositionId,
                              @Valid @RequestBody VitalSignsRequest request) {
        VitalSigns v = new VitalSigns();
        v.setCompositionId(compositionId);
        v.setRecordedBy(request.recordedBy());
        v.setRecordedAt(request.recordedAt() != null ? request.recordedAt() : Instant.now());
        v.setSystolicBp(request.systolicBp());
        v.setDiastolicBp(request.diastolicBp());
        v.setHeartRate(request.heartRate());
        v.setRespiratoryRate(request.respiratoryRate());
        v.setTemperature(request.temperature());
        v.setOxygenSaturation(request.oxygenSaturation());
        v.setWeight(request.weight());
        return VitalSignsResponse.from(vitalSignsService.record(v));
    }

    @GetMapping("/api/v1/ehr/{ehrId}/vitals")
    List<VitalSignsResponse> list(@PathVariable UUID ehrId,
                                  @RequestParam(required = false) Instant from,
                                  @RequestParam(required = false) Instant to) {
        Instant start = from != null ? from : Instant.EPOCH;
        Instant end = to != null ? to : Instant.now();
        return vitalSignsService.listByEhr(ehrId, start, end).stream()
                .map(VitalSignsResponse::from).toList();
    }

    @GetMapping("/api/v1/ehr/{ehrId}/vitals/latest")
    VitalSignsResponse latest(@PathVariable UUID ehrId) {
        return vitalSignsService.getLatest(ehrId)
                .map(VitalSignsResponse::from)
                .orElse(null);
    }
}
