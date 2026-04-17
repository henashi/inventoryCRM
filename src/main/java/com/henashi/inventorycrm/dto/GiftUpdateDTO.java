package com.henashi.inventorycrm.dto;

import com.henashi.inventorycrm.pojo.Gift;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

public record GiftUpdateDTO (
        @Schema(description = "礼品ID")
        Long id,
        @Schema(description = "礼品名称")
        @NotNull
        @Length(max = 100)
        String name,
        @NotNull
        @Length(max = 50)
        @Schema(description = "礼品编码")
        String code,
        @NotNull
        @Schema(description = "礼品类型")
        Gift.GiftType type,
        @NotNull
        @Schema(description = "关联的产品ID")
        Long productId,
        @NotNull
        @Schema(description = "礼品状态")
        Gift.GiftStatus status,
        @NotNull
        @Schema(description = "是否启用领取限制")
        Boolean limitEnabled,
        @Schema(description = "每人领取限制数量")
        Integer limitPerPerson,
        @Length(max = 200)
        @Schema(description = "备注")
        String remark,
        @Length(max = 200)
        @Schema(description = "礼品描述")
        String description)
{
}
