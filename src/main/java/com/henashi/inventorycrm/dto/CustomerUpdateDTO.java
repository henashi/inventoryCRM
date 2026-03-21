package com.henashi.inventorycrm.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.henashi.inventorycrm.annotation.PhoneNumber;
import com.henashi.inventorycrm.constants.RegexPatterns;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CustomerUpdateDTO(
        @Size(min = 2, max = 50, message = "姓名长度必须在2-50字符之间")
        String name,

        @PhoneNumber
        String phone,

        @Size(max = 200, message = "地址长度不能超过200字符")
        String address,

        @Pattern(regexp = RegexPatterns.OPTIONAL_EMAIL, message = "邮箱格式不正确")
        String email,

        Integer type,

        LocalDate registeredAt,

        Long referrerId,

        Integer giftLevel,

        LocalDateTime giftReceivedAt,

        String remark,

        Integer status,

        Integer gender,

        @JsonFormat(pattern = "yyyy-MM-dd")
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        LocalDate birthday
) {
}