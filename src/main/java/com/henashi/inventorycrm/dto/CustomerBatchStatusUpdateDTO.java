package com.henashi.inventorycrm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CustomerBatchStatusUpdateDTO(
        @Schema(description = "客户ID列表")
        @NotEmpty(message = "客户ID列表不能为空")
        List<@NotNull @Min(value = 1, message = "客户ID必须大于0") Long> ids,

        @Schema(description = "客户状态，0=停用，1=正常")
        @NotNull(message = "客户状态不能为空")
        @Min(value = 0, message = "客户状态不合法")
        @Max(value = 1, message = "客户状态不合法")
        Integer status
) {
}
