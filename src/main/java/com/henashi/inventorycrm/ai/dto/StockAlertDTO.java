package com.henashi.inventorycrm.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * 高危预警商品 DTO
 */
@Schema(description = "库存预警信息")
public record StockAlertDTO(

        @Schema(description = "商品ID")
        Long productId,

        @Schema(description = "商品编码")
        String productCode,

        @Schema(description = "商品名称")
        String productName,

        @Schema(description = "当前库存")
        Integer currentStock,

        @Schema(description = "安全库存")
        Integer safeStock,

        @Schema(description = "近7日均出库量")
        BigDecimal avgDailyOut,

        @Schema(description = "预计耗尽天数")
        Integer estimatedDaysToEmpty,

        @Schema(description = "建议补货量")
        Integer suggestedRestockQty,

        @Schema(description = "预警级别: DANGER / WARNING")
        String alertLevel,

        @Schema(description = "预警原因")
        String alertReason

) {
}
