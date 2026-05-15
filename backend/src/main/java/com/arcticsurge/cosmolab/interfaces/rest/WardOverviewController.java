package com.arcticsurge.cosmolab.interfaces.rest;

import com.arcticsurge.cosmolab.application.ward.WardOverviewService;
import com.arcticsurge.cosmolab.interfaces.rest.dto.WardOverviewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ward")
@RequiredArgsConstructor
public class WardOverviewController {

    private final WardOverviewService wardOverviewService;

    @GetMapping("/{wardId}/overview")
    WardOverviewResponse overview(@PathVariable String wardId) {
        return WardOverviewResponse.from(wardId, wardOverviewService.getOverview(wardId));
    }
}
