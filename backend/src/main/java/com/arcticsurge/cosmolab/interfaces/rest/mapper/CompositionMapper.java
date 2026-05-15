package com.arcticsurge.cosmolab.interfaces.rest.mapper;

import com.arcticsurge.cosmolab.domain.composition.Composition;
import com.arcticsurge.cosmolab.interfaces.rest.dto.CompositionRequest;
import com.arcticsurge.cosmolab.interfaces.rest.dto.CompositionResponse;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface CompositionMapper {

    // ehrId is supplied from the path variable; commitTime and status are set by @PrePersist
    @Mapping(target = "ehrId", source = "ehrId")
    @Mapping(target = "commitTime", ignore = true)
    @Mapping(target = "status", ignore = true)
    Composition toEntity(CompositionRequest request, UUID ehrId);

    // Updates only type, startTime, facilityName — ehrId, authorId, commitTime, status are immutable
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ehrId", ignore = true)
    @Mapping(target = "authorId", ignore = true)
    @Mapping(target = "commitTime", ignore = true)
    @Mapping(target = "status", ignore = true)
    void merge(CompositionRequest source, @MappingTarget Composition target);

    CompositionResponse toResponse(Composition composition);
}
