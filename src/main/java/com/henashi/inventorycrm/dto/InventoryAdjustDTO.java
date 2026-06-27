package com.henashi.inventorycrm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record InventoryAdjustDTO(
        @Schema(description = "盘点后的实际库存")
        @NotNull(message = "实际库存不能为空")
        @PositiveOrZero(message = "实际库存不能为负数")
        Integer actualQuantity,

        @Schema(description = "调整原因")
        @Size(max = 200, message = "调整原因不能超过200字符")
        String reason,

        @Schema(description = "备注")
        @Size(max = 500, message = "备注不能超过500字符")
        String remark
) {
}
