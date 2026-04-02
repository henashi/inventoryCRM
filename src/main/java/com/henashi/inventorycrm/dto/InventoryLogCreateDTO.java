package com.henashi.inventorycrm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record InventoryLogCreateDTO(
        // 必填信息
        @NotNull(message = "商品ID不能为空")
        @Schema(description = "商品ID")
        Long productId,

        @NotBlank(message = "操作类型不能为空")
        @Pattern(regexp = "IN|OUT|ADJUST|CREATE", message = "操作类型必须为IN/OUT/ADJUST/CREATE")
        @Schema(description = "库存操作类型")
        String type,

        @NotNull(message = "数量不能为空")
        @Min(value = 1, message = "数量必须大于0")
        @Schema(description = "操作数量")
        Integer quantity,

        @NotBlank(message = "原因不能为空")
        @Size(max = 200, message = "原因不能超过200字符")
        @Schema(description = "操作原因")
        String reason,

        // 操作人
        @NotBlank(message = "操作人不能为空")
        @Size(max = 50, message = "操作人不能超过50字符")
        @Schema(description = "操作人")
        String operator,

        // 可选信息
        @Size(max = 500, message = "备注不能超过500字符")
        @Schema(description = "备注")
        String remark
) {
}
