package com.henashi.inventorycrm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InventoryInDTO(
        @Schema(description = "商品ID")
        @NotNull(message = "商品ID不能为空")
        Long productId,

        @Schema(description = "入库数量")
        @NotNull(message = "入库数量不能为空")
        @Min(value = 1, message = "入库数量必须大于0")
        Integer quantity,

        @Schema(description = "仓库ID")
        Long warehouseId,

        @Schema(description = "入库原因")
        @NotBlank(message = "入库原因不能为空")
        @Size(max = 200, message = "入库原因不能超过200字符")
        String reason,

        @Schema(description = "备注")
        @Size(max = 500, message = "备注不能超过500字符")
        String remark
) {
}
