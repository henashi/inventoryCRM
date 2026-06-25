package com.henashi.inventorycrm.mapper;

import com.henashi.inventorycrm.dto.OrderItemCreateDTO;
import com.henashi.inventorycrm.dto.OrderItemDTO;
import com.henashi.inventorycrm.pojo.OrderItem;
import com.henashi.inventorycrm.mapper.reference.ProductReferenceMapper;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = MappingConstants.ComponentModel.SPRING,
        uses = {ProductReferenceMapper.class}
)
public interface OrderItemMapper {

    @Mapping(source = "customer.id", target = "customerId")
    @Mapping(source = "customer.name", target = "customerName")
    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "product.name", target = "productName")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    OrderItemDTO toDTO(OrderItem orderItem);

    @Mapping(source = "productId", target = "product", qualifiedByName = "idToProduct")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    OrderItem createToEntity(OrderItemCreateDTO dto);
}
