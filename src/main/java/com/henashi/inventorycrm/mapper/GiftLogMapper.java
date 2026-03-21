package com.henashi.inventorycrm.mapper;

import com.henashi.inventorycrm.dto.GiftLogCreateDTO;
import com.henashi.inventorycrm.dto.GiftLogDTO;
import com.henashi.inventorycrm.dto.GiftLogUpdateDTO;
import com.henashi.inventorycrm.mapper.reference.CustomerReferenceMapper;
import com.henashi.inventorycrm.mapper.reference.GiftReferenceMapper;
import com.henashi.inventorycrm.pojo.GiftLog;
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
        uses = {
                CustomerReferenceMapper.class,
                GiftReferenceMapper.class
        }
)
public interface GiftLogMapper {

    @Mapping(source = "customerId", target = "customer", qualifiedByName = "idToCustomer")
    @Mapping(source = "giftId", target = "gift", qualifiedByName = "idToGift")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    GiftLog toEntity(GiftLogDTO giftLogDTO);

    @Mapping(source = "customer.id", target = "customerId")
    @Mapping(source = "customer.name", target = "customerName")
    @Mapping(source = "gift.id", target = "giftId")
    @Mapping(source = "gift.name", target = "giftName")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    GiftLogDTO fromEntity(GiftLog giftLog);

    @Mapping(source = "customerId", target = "customer", qualifiedByName = "idToCustomer")
    @Mapping(source = "giftId", target = "gift", qualifiedByName = "idToGift")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    GiftLog partialUpdate(GiftLogUpdateDTO giftLogDTO, @MappingTarget GiftLog giftLog);

    @Mapping(source = "customerId", target = "customer", qualifiedByName = "idToCustomer")
    @Mapping(source = "giftId", target = "gift", qualifiedByName = "idToGift")
    @Mapping(source = "operator", target = "operator", defaultValue = "system")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    GiftLog createToEntity(GiftLogCreateDTO giftLogCreateDTO);
}