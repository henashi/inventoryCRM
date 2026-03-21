package com.henashi.inventorycrm.mapper;

import com.henashi.inventorycrm.dto.CustomerCreateDTO;
import com.henashi.inventorycrm.dto.CustomerDTO;
import com.henashi.inventorycrm.dto.CustomerUpdateDTO;
import com.henashi.inventorycrm.mapper.reference.CustomerReferenceMapper;
import com.henashi.inventorycrm.pojo.Customer;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
        uses = CustomerReferenceMapper.class,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = MappingConstants.ComponentModel.SPRING)
public interface CustomerMapper {
    Customer toEntity(CustomerDTO customerDTO);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(source = "referrer.id", target = "referrerId")
    @Mapping(source = "referrer.name", target = "referrerName")
    CustomerDTO fromEntity(Customer customer);

    @Mapping(source = "referrerId", target = "referrer", qualifiedByName = "idToCustomer")
    @Mapping(
            source = "registeredAt", target = "registeredAt",
            qualifiedByName = "dateDefaultNow"
    )
    @Mapping(source = "type", target = "type", defaultValue = "1")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Customer createToEntity(CustomerCreateDTO customerCreateDTO);

    @Mapping(source = "referrerId", target = "referrer", qualifiedByName = "idToCustomer")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Customer partialUpdate(CustomerUpdateDTO customerDTO, @MappingTarget Customer customer);
}