package com.henashi.inventorycrm.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.henashi.inventorycrm.annotation.PhoneNumber;
import com.henashi.inventorycrm.constants.RegexPatterns;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CustomerUpdateDTO(
        @Schema(description = "客户ID")
        Long id,

        @Size(min = 2, max = 50, message = "姓名长度必须在2-50字符之间")
        @Schema(description = "客户名称")
        String name,

        @PhoneNumber
        @Schema(description = "客户联系方式")
        String phone,

        @Size(max = 200, message = "地址长度不能超过200字符")
        @Schema(description = "客户地址")
        String address,

        @Schema(description = "客户邮箱")
        @Pattern(regexp = RegexPatterns.OPTIONAL_EMAIL, message = "邮箱格式不正确")
        String email,

        @Schema(description = "客户类型，0=普通客户，1=会员")
        Integer type,

        @Schema(description = "注册日期")
        LocalDate registeredAt,

        @Schema(description = "推荐人ID")
        Long referrerId,

        @Schema(description = "礼品等级，0=无礼品，1=初次邀新礼品，2=第二次邀新礼品，3=第三次邀新礼品")
        Integer giftLevel,

        @Schema(description = "礼品接收时间")
        LocalDateTime giftReceivedAt,

        @Schema(description = "备注")
        String remark,

        @Schema(description = "客户状态，0=正常，1=停用")
        Integer status,

        @Schema(description = "客户性别：0-女，1-男")
        Integer gender,

        @JsonFormat(pattern = "yyyy-MM-dd")
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        @Schema(description = "客户生日")
        LocalDate birthday
) {
}
