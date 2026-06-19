package com.henashi.inventorycrm.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record InventoryChangeDTO(
        @Schema(description = "变更记录ID")
        Long id,

        @Schema(description = "库存ID，默认等于商品ID")
        Long inventoryId,

        @Schema(description = "商品ID")
        Long productId,

        @Schema(description = "商品名称")
        String productName,

        @Schema(description = "变更类型")
        String changeType,

        @Schema(description = "变更数量")
        Integer changeQuantity,

        @Schema(description = "变更前库存")
        Integer beforeQuantity,

        @Schema(description = "变更后库存")
        Integer afterQuantity,

        @Schema(description = "变更原因")
        String reason,

        @Schema(description = "操作人")
        String operator,

        @Schema(description = "备注")
        String remark,

        @Schema(description = "创建时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime createdAt
) {
}
