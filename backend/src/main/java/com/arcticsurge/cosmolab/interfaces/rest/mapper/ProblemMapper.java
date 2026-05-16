package com.arcticsurge.cosmolab.interfaces.rest.mapper;

import com.arcticsurge.cosmolab.domain.evaluation.ProblemDiagnosis;
import com.arcticsurge.cosmolab.interfaces.rest.dto.ProblemRequest;
import com.arcticsurge.cosmolab.interfaces.rest.dto.ProblemResponse;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface ProblemMapper {

    // ehrId from path variable; status/resolvedDate/recordedAt set by @PrePersist
    @Mapping(target = "ehrId", source = "ehrId")
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "resolvedDate", ignore = true)
    @Mapping(target = "recordedAt", ignore = true)
    ProblemDiagnosis toEntity(ProblemRequest request, UUID ehrId);

    // Updates only displayName and severity — all identity/lifecycle fields are immutable
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ehrId", ignore = true)
    @Mapping(target = "compositionId", ignore = true)
    @Mapping(target = "icd10Code", ignore = true)
    @Mapping(target = "recordedBy", ignore = true)
    @Mapping(target = "onsetDate", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "resolvedDate", ignore = true)
    @Mapping(target = "recordedAt", ignore = true)
    void merge(ProblemRequest source, @MappingTarget ProblemDiagnosis target);

    ProblemResponse toResponse(ProblemDiagnosis entry);
}
