package com.arcticsurge.cosmolab.interfaces.rest.mapper;

import com.arcticsurge.cosmolab.domain.observation.VitalSigns;
import com.arcticsurge.cosmolab.interfaces.rest.dto.VitalSignsRequest;
import com.arcticsurge.cosmolab.interfaces.rest.dto.VitalSignsResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface VitalSignsMapper {

    // compositionId comes from the path variable; recordedAt is nullable — null triggers @PrePersist default
    @Mapping(target = "compositionId", source = "compositionId")
    VitalSigns toEntity(VitalSignsRequest request, UUID compositionId);

    VitalSignsResponse toResponse(VitalSigns vitalSigns);
}
