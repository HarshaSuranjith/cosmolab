package com.arcticsurge.cosmolab.interfaces.rest.dto;

import com.arcticsurge.cosmolab.domain.ehr.EhrRecord;
import com.arcticsurge.cosmolab.domain.ehr.EhrStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record EhrResponse(
        UUID ehrId,
        UUID subjectId,
        String systemId,
        EhrStatus status,
        LocalDateTime createdAt
) {
    public static EhrResponse from(EhrRecord ehr) {
        return new EhrResponse(ehr.getEhrId(), ehr.getSubjectId(),
                ehr.getSystemId(), ehr.getStatus(), ehr.getCreatedAt());
    }
}
