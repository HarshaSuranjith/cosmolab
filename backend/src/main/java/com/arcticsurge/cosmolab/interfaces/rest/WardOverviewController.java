package com.arcticsurge.cosmolab.interfaces.rest;

import com.arcticsurge.cosmolab.application.ward.WardOverviewService;
import com.arcticsurge.cosmolab.interfaces.rest.dto.WardOverviewResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ward")
@RequiredArgsConstructor
@Tag(name = "Ward Overview", description = "Aggregated ward dashboard — primary JMeter / Gatling / k6 load test target. Executes a 4-table join with window function for latest vitals.")
public class WardOverviewController {

    private final WardOverviewService wardOverviewService;

    @Operation(summary = "Get ward overview",
               description = "Returns all active patients in the ward with their latest vital signs, " +
                       "active problem count, and abnormality flags. " +
                       "Primary load test endpoint — N+1 risk analysed and addressed in Sprint 7.")
    @ApiResponse(responseCode = "200", description = "Ward overview returned successfully")
    @GetMapping("/{wardId}/overview")
    WardOverviewResponse overview(
            @Parameter(description = "Ward identifier, e.g. ICU, CARDIOLOGY, NEUROLOGY")
            @PathVariable String wardId) {
        return WardOverviewResponse.from(wardId, wardOverviewService.getOverview(wardId));
    }
}
