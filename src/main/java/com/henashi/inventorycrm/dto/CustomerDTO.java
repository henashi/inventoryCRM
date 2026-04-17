package com.henashi.inventorycrm.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

// 客户DTO
public record CustomerDTO (
        @Schema(description = "客户ID")
        Long id,

        @Schema(description = "客户名称")
        String name,

        @Schema(description = "客户手机号")
        String phone,

        @Schema(description = "客户邮箱")
        String email,

        @Schema(description = "客户联系方式")
        String contactInfo,

        @Schema(description = "客户地址")
        String address,

        @Schema(description = "客户生日")
        LocalDate birthday,

        @Schema(description = "客户性别：0-女，1-男")
        Integer gender,

        @Schema(description = "礼品等级，0=无礼品，1=初次邀新礼品，2=第二次邀新礼品，3=第三次邀新礼品")
        Integer giftLevel,

        @Schema(description = "客户类型，0=普通客户，1=会员")
        Integer type,

        @Schema(description = "邀新客户数量")
        Integer referralCount,

        @Schema(description = "推荐人名称")
        String referrerName,

        @Schema(description = "推荐人ID")
        Long referrerId,

        @Schema(description = "注册日期")
        LocalDate registeredAt,

        @Schema(description = "备注")
        String remark,

        @Schema(description = "客户状态，0=正常，1=停用")
        Integer status)
{
}
