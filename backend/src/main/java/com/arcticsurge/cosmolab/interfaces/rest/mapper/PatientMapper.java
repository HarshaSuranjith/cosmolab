package com.arcticsurge.cosmolab.interfaces.rest.mapper;

import com.arcticsurge.cosmolab.domain.patient.Patient;
import com.arcticsurge.cosmolab.interfaces.rest.dto.PatientRequest;
import com.arcticsurge.cosmolab.interfaces.rest.dto.PatientResponse;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface PatientMapper {

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Patient toEntity(PatientRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "personalNumber", ignore = true)
    @Mapping(target = "dateOfBirth", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void merge(PatientRequest source, @MappingTarget Patient target);

    PatientResponse toResponse(Patient patient);
}
