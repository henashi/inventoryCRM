package com.henashi.inventorycrm.mapper;

import com.henashi.inventorycrm.dto.SystemConfigCreateDTO;
import com.henashi.inventorycrm.dto.SystemConfigDTO;
import com.henashi.inventorycrm.pojo.SystemConfig;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface SystemConfigMapper {
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    SystemConfig toEntity(SystemConfigDTO systemConfigDTO);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    SystemConfig createToEntity(SystemConfigCreateDTO systemConfigCreateDTO);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    SystemConfigDTO fromEntity(SystemConfig systemConfig);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    SystemConfig partialUpdate(SystemConfigDTO systemConfigDTO, @MappingTarget SystemConfig systemConfig);
}