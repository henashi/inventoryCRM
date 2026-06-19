package com.henashi.inventorycrm.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record CustomerSearchOptionDTO(
        @Schema(description = "客户ID")
        Long id,

        @Schema(description = "客户名称")
        String name,

        @Schema(description = "客户手机号")
        String phone,

        @Schema(description = "客户邮箱")
        String email,

        @Schema(description = "礼品等级")
        Integer giftLevel,

        @Schema(description = "客户状态，0=停用，1=正常")
        Integer status
) {
}
