package com.arcticsurge.cosmolab.interfaces.rest.dto;

import com.arcticsurge.cosmolab.domain.ehr.EhrStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Electronic Health Record root — openEHR EHR container")
public record EhrResponse(
        @Schema(description = "EHR root identifier (openEHR EHR_ID)") UUID ehrId,
        @Schema(description = "Patient this EHR belongs to (subject of care)") UUID subjectId,
        @Schema(description = "Issuing system identifier", example = "cosmolab-v1") String systemId,
        @Schema(description = "EHR status") EhrStatus status,
        @Schema(description = "EHR creation timestamp (UTC)") Instant createdAt
) {}
