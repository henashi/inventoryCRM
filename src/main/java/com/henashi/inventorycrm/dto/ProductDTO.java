package com.henashi.inventorycrm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductDTO(
        @Schema(description = "主键ID")
        Long id,

        @Schema(description = "商品名称")
        String name,

        @Schema(description = "商品编码")
        String code,

        @Schema(description = "商品分类")
        String category,

        @Schema(description = "当前库存")
        Integer currentStock,

        @Schema(description = "安全库存")
        Integer safeStock,

        @Schema(description = "单位")
        String unit,

        @Schema(description = "单价")
        BigDecimal price,

        @Schema(description = "是否库存低于安全库存")
        boolean stockLow,

        @Schema(description = "是否缺货")
        boolean outOfStock,

        @Schema(description = "创建时间")
        LocalDateTime createdTime,

        @Schema(description = "状态：0-禁用，1-启用")
        Integer status
) {
}
