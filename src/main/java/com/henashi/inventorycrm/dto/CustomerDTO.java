package com.henashi.inventorycrm.dto;

import java.time.LocalDate;

// 客户DTO
public record CustomerDTO(
        Long id,
        String name,
        String phone,
        String email,
        LocalDate birthday,
        Integer gender,
        String address,
        Integer giftLevel,
        Integer type,
        Integer referralCount,
        String referrerName,
        Long referrerId,
        LocalDate registeredAt,
        String remark,
        Integer status)
{
}