package com.henashi.inventorycrm.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record InventoryLogDTO(
        Long id,
        Long productId,
        String productName,
        String productCode,
        String type,
        Integer quantity,
        Integer beforeStock,
        Integer afterStock,
        String reason,
        String operator,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime createdTime
) {
}

