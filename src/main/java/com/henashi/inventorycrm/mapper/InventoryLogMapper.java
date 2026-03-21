package com.henashi.inventorycrm.mapper;

import com.henashi.inventorycrm.dto.InventoryLogCreateDTO;
import com.henashi.inventorycrm.dto.InventoryLogDTO;
import com.henashi.inventorycrm.mapper.reference.ProductReferenceMapper;
import com.henashi.inventorycrm.pojo.InventoryLog;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = MappingConstants.ComponentModel.SPRING,
        uses = {ProductReferenceMapper.class}
)
public interface InventoryLogMapper {
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    InventoryLog toEntity(InventoryLogDTO inventoryLogDTO);

    @Mapping(source = "productId", target = "product", qualifiedByName = "idToProduct")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    InventoryLog createToEntity(InventoryLogCreateDTO inventoryLogCreateDTO);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    InventoryLogCreateDTO createFromEntity(InventoryLog inventoryLog);

    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "product.name", target = "productName")
    @Mapping(source = "product.code", target = "productCode")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    InventoryLogDTO fromEntity(InventoryLog inventoryLog);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    InventoryLog partialUpdate(InventoryLogDTO inventoryLogDTO, @MappingTarget InventoryLog inventoryLog);
}