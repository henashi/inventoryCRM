package com.henashi.inventorycrm.dto;


import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductDTO(
        Long id,
        String name,
        String code,
        String category,
        Integer currentStock,
        Integer safeStock,
        String unit,
        BigDecimal price,
        boolean stockLow,
        boolean outOfStock,
        LocalDateTime createdTime,
        Integer status
) {
}
