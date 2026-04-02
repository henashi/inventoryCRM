package com.henashi.inventorycrm.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record InventoryLogDTO(
        @Schema(description = "日志ID")
        Long id,
        @Schema(description = "商品ID")
        Long productId,
        @Schema(description = "商品名称")
        String productName,
        @Schema(description = "商品编码")
        String productCode,
        @Schema(description = "操作类型")
        String type,
        @Schema(description = "操作数量")
        Integer quantity,
        @Schema(description = "操作前库存")
        Integer beforeStock,
        @Schema(description = "操作后库存")
        Integer afterStock,
        @Schema(description = "操作原因")
        String reason,
        @Schema(description = "操作员")
        String operator,
        @Schema(description = "创建时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime createdTime
) {
}

