package com.henashi.inventorycrm.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ProductSearchOptionDTO(
        @Schema(description = "商品ID")
        Long id,

        @Schema(description = "商品名称")
        String name,

        @Schema(description = "商品编码")
        String code,

        @Schema(description = "商品分类")
        String category,

        @Schema(description = "当前库存")
        Integer currentStock,

        @Schema(description = "单位")
        String unit,

        @Schema(description = "商品状态，0=停售，1=在售")
        Integer status
) {
}
