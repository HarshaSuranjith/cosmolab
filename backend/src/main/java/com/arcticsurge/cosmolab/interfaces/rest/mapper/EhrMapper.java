package com.arcticsurge.cosmolab.interfaces.rest.mapper;

import com.arcticsurge.cosmolab.domain.ehr.EhrRecord;
import com.arcticsurge.cosmolab.interfaces.rest.dto.EhrResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EhrMapper {

    EhrResponse toResponse(EhrRecord ehr);
}
