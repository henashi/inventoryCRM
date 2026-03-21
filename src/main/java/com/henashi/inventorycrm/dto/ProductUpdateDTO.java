package com.henashi.inventorycrm.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ProductUpdateDTO(
        // 基本信息
        @NotBlank(message = "商品名称不能为空")
        @Size(min = 2, max = 100, message = "商品名称长度2-100字符")
        String name,

        // 库存信息
        @NotNull(message = "当前库存不能为空")
        @PositiveOrZero(message = "当前库存不能为负数")
        Integer currentStock,

        @NotNull(message = "安全库存不能为空")
        @PositiveOrZero(message = "安全库存不能为负数")
        Integer safeStock,

        // 单位价格
        @NotBlank(message = "单位不能为空")
        @Size(max = 20, message = "单位不能超过20字符")
        String unit,

        @NotNull(message = "价格不能为空")
        @DecimalMin(value = "0.00", message = "价格不能为负数")
        @DecimalMax(value = "9999999.99", message = "价格不能超过9999999.99")
        BigDecimal price,

        // 描述
        @Size(max = 1000, message = "商品描述不能超过1000字符")
        String description,

        // 备注
        @Size(max = 500, message = "备注不能超过500字符")
        String remark,

        Integer status
) {
}
