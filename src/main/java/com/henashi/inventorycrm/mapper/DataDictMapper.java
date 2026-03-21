package com.henashi.inventorycrm.mapper;

import com.henashi.inventorycrm.dto.DataDictCreateDTO;
import com.henashi.inventorycrm.dto.DataDictDTO;
import com.henashi.inventorycrm.dto.DataDictUpdateDTO;
import com.henashi.inventorycrm.pojo.DataDict;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = MappingConstants.ComponentModel.SPRING
)
public interface DataDictMapper {

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    DataDict toEntity(DataDictDTO dataDictDTO);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    DataDict createToEntity(DataDictCreateDTO dataDictCreateDTO);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    DataDictDTO fromEntity(DataDict dataDict);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    DataDict partialUpdate(DataDictUpdateDTO dataDictDTO, @MappingTarget DataDict dataDict);
}