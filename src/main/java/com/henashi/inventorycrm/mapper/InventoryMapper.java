package com.henashi.inventorycrm.mapper;

import com.henashi.inventorycrm.dto.InventoryChangeDTO;
import com.henashi.inventorycrm.dto.InventoryDTO;
import com.henashi.inventorycrm.pojo.InventoryLog;
import com.henashi.inventorycrm.pojo.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.time.LocalDateTime;

@Mapper(
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = MappingConstants.ComponentModel.SPRING
)
public interface InventoryMapper {

    @Mapping(source = "id", target = "id")
    @Mapping(source = "id", target = "productId")
    @Mapping(source = "code", target = "productCode")
    @Mapping(source = "name", target = "productName")
    @Mapping(source = "category", target = "category")
    @Mapping(target = "warehouseId", ignore = true)
    @Mapping(target = "warehouseName", ignore = true)
    @Mapping(target = "maxStock", expression = "java(resolveMaxStock(product))")
    @Mapping(target = "status", expression = "java(resolveStatus(product.getStatus()))")
    @Mapping(target = "lastUpdateTime", expression = "java(resolveLastUpdateTime(product))")
    @Mapping(target = "lowStock", expression = "java(product.isStockLow())")
    @Mapping(target = "outOfStock", expression = "java(product.isOutOfStock())")
    @Mapping(target = "alertReason", ignore = true)
    InventoryDTO toInventoryDTO(Product product);

    @Mapping(source = "id", target = "id")
    @Mapping(source = "product.id", target = "inventoryId")
    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "product.name", target = "productName")
    @Mapping(target = "changeType", expression = "java(resolveChangeType(inventoryLog.getType()))")
    @Mapping(source = "quantity", target = "changeQuantity")
    @Mapping(source = "beforeStock", target = "beforeQuantity")
    @Mapping(source = "afterStock", target = "afterQuantity")
    @Mapping(source = "reason", target = "reason")
    @Mapping(source = "operator", target = "operator")
    @Mapping(source = "remark", target = "remark")
    @Mapping(source = "createdTime", target = "createdAt")
    InventoryChangeDTO toInventoryChangeDTO(InventoryLog inventoryLog);

    default Integer resolveStatus(String status) {
        if (status == null || status.isBlank()) {
            return 1;
        }
        return "0".equals(status.trim()) ? 0 : 1;
    }

    default LocalDateTime resolveLastUpdateTime(Product product) {
        if (product.getContentUpdatedTime() != null) {
            return product.getContentUpdatedTime();
        }
        if (product.getStatusUpdatedTime() != null) {
            return product.getStatusUpdatedTime();
        }
        return product.getCreatedTime();
    }

    default Integer resolveMaxStock(Product product) {
        return null;
    }

    default String resolveChangeType(InventoryLog.LogType type) {
        if (type == null) {
            return "adjust";
        }
        return switch (type) {
            case IN, CREATE -> "in";
            case OUT -> "out";
            case ADJUST, PARAM -> "adjust";
        };
    }
}
