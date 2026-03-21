package com.henashi.inventorycrm.mapper;

import com.henashi.inventorycrm.dto.GiftCreateDTO;
import com.henashi.inventorycrm.dto.GiftDTO;
import com.henashi.inventorycrm.dto.GiftUpdateDTO;
import com.henashi.inventorycrm.mapper.reference.ProductReferenceMapper;
import com.henashi.inventorycrm.pojo.Gift;
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
        uses = { ProductReferenceMapper.class }
)
public interface GiftMapper {

    @Mapping(source = "productId", target = "product", qualifiedByName = "idToProduct")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Gift toEntity(GiftDTO giftDTO);

    @Mapping(source = "productId", target = "product", qualifiedByName = "idToProduct")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Gift createToEntity(GiftCreateDTO giftCreateDTO);

    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "product.name", target = "productName")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    GiftDTO fromEntity(Gift gift);

    @Mapping(source = "productId", target = "product", qualifiedByName = "idToProduct")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Gift partialUpdate(GiftUpdateDTO giftDTO, @MappingTarget Gift gift);
}