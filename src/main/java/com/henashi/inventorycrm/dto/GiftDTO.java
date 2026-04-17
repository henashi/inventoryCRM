package com.henashi.inventorycrm.dto;

import com.henashi.inventorycrm.pojo.Gift;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for Gift entity.
 */
public record GiftDTO (
    @Schema(description = "礼品ID")
    Long id,

    @Schema(description = "礼品名称")
    String name,

    @Schema(description = "礼品描述")
    String description,

    @Schema(description = "礼品库存数量")
    Integer stock,

    @Schema(description = "礼品类型")
    Gift.GiftType type,

    @Schema(description = "关联的商品ID")
    Long productId,

    @Schema(description = "关联的商品名称")
    String productName,

    @Schema(description = "礼品状态")
    Gift.GiftStatus giftStatus,

    @Schema(description = "是否启用领取限制")
    Boolean limitEnabled,

    @Schema(description = "每人领取限制数量")
    Integer limitPerPerson,

    @Schema(description = "创建时间")
    LocalDateTime createdTime,

    @Schema(description = "状态更新时间")
    LocalDateTime statusUpdatedTime,

    @Schema(description = "数据更新时间")
    LocalDateTime contentUpdatedTime,

    @Schema(description = "备注")
    String remark)
{
}
