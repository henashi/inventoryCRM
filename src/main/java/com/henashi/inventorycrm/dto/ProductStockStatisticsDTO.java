package com.henashi.inventorycrm.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public record ProductStockStatisticsDTO(
        @Schema(description = "商品总数")
        Long totalProducts,

        @Schema(description = "在售商品数")
        Long activeProducts,

        @Schema(description = "低库存商品数")
        Long lowStockProducts,

        @Schema(description = "缺货商品数")
        Long outOfStockProducts,

        @Schema(description = "库存总量")
        Long totalStockQuantity,

        @Schema(description = "库存总价值，按 currentStock * price 汇总")
        BigDecimal totalStockValue
) {
}
