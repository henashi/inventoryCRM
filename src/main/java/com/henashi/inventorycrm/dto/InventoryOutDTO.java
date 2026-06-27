package com.henashi.inventorycrm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InventoryOutDTO(
        @Schema(description = "商品ID")
        @NotNull(message = "商品ID不能为空")
        Long productId,

        @Schema(description = "出库数量")
        @NotNull(message = "出库数量不能为空")
        @Min(value = 1, message = "出库数量必须大于0")
        Integer quantity,

        @Schema(description = "仓库ID")
        Long warehouseId,

        @Schema(description = "出库原因")
        @Size(max = 200, message = "出库原因不能超过200字符")
        String reason,

        @Schema(description = "备注")
        @Size(max = 500, message = "备注不能超过500字符")
        String remark
) {
}
