package com.henashi.inventorycrm.dto;

import com.henashi.inventorycrm.pojo.Gift;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

public record GiftCreateDTO (
        @NotNull
        @Length(max = 100)
        @Schema(description = "礼品名称，必填")
        String name,
        @NotNull
        @Length(max = 50)
        @Schema(description = "礼品编码，必填")
        String code,
        @NotNull
        @Schema(description = "礼品类型，必填")
        Gift.GiftType type,
        @NotNull
        @Schema(description = "关联的产品ID，必填")
        Long productId,
        @Length(max = 200)
        @Schema(description = "礼品描述")
        String description,
        @NotNull
        @Schema(description = "礼品状态，必填")
        Gift.GiftStatus status,
        @NotNull
        @Schema(description = "是否启用领取限制，必填")
        Boolean limitEnabled,
        @Min(1)
        @Schema(description = "每人领取限制数量")
        Integer limitPerPerson,
        @Length(max = 200)
        @Schema(description = "备注")
        String remark)
{
}

