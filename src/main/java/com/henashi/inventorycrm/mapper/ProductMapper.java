package com.henashi.inventorycrm.mapper;

import com.henashi.inventorycrm.dto.ProductCreateDTO;
import com.henashi.inventorycrm.dto.ProductDTO;
import com.henashi.inventorycrm.dto.ProductUpdateDTO;
import com.henashi.inventorycrm.pojo.Product;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = MappingConstants.ComponentModel.SPRING
)
public interface ProductMapper {

    Product toEntity(ProductDTO productDTO);

    @Mapping(source = "currentStock", target = "currentStock", defaultValue = "0")
    @Mapping(source = "safeStock", target = "safeStock", defaultValue = "10")
    @Mapping(source = "price", target = "price", defaultValue = "0")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Product createToEntity(ProductCreateDTO productCreateDTO);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Product updateToEntity(ProductUpdateDTO productUpdateDTO);

    @Mapping(source = "status", target = "status", qualifiedByName = "stringStatusToInteger")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    ProductDTO fromEntity(Product product);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Product partialUpdate(ProductUpdateDTO productDTO, @MappingTarget Product product);

    @Named("stringStatusToInteger")
    default Integer stringStatusToInteger(String status) {
        if (status == null || status.isBlank()) {
            return 1;
        }
        return "0".equals(status.trim()) ? 0 : 1;
    }
}
