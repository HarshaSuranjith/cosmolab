package com.arcticsurge.cosmolab.interfaces.rest.dto;

import com.arcticsurge.cosmolab.domain.ehr.EhrRecord;
import com.arcticsurge.cosmolab.domain.ehr.EhrStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Electronic Health Record root — openEHR EHR container")
public record EhrResponse(
        @Schema(description = "EHR root identifier (openEHR EHR_ID)") UUID ehrId,
        @Schema(description = "Patient this EHR belongs to (subject of care)") UUID subjectId,
        @Schema(description = "Issuing system identifier", example = "cosmolab-v1") String systemId,
        @Schema(description = "EHR status") EhrStatus status,
        @Schema(description = "EHR creation timestamp") LocalDateTime createdAt
) {
    public static EhrResponse from(EhrRecord ehr) {
        return new EhrResponse(ehr.getEhrId(), ehr.getSubjectId(),
                ehr.getSystemId(), ehr.getStatus(), ehr.getCreatedAt());
    }
}
