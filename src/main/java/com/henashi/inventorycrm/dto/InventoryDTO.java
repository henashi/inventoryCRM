package com.henashi.inventorycrm.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record InventoryDTO(
        @Schema(description = "库存视图ID，默认等于商品ID")
        Long id,

        @Schema(description = "商品ID")
        Long productId,

        @Schema(description = "商品编码")
        String productCode,

        @Schema(description = "商品名称")
        String productName,

        @Schema(description = "商品分类")
        String category,

        @Schema(description = "仓库ID")
        Long warehouseId,

        @Schema(description = "仓库名称")
        String warehouseName,

        @Schema(description = "当前库存")
        Integer currentStock,

        @Schema(description = "安全库存")
        Integer safeStock,

        @Schema(description = "最大库存")
        Integer maxStock,

        @Schema(description = "单位")
        String unit,

        @Schema(description = "状态：0-停用，1-正常")
        Integer status,

        @Schema(description = "最近更新时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime lastUpdateTime,

        @Schema(description = "是否低库存")
        Boolean lowStock,

        @Schema(description = "是否缺货")
        Boolean outOfStock,

        @Schema(description = "预警原因")
        String alertReason
) {
}
