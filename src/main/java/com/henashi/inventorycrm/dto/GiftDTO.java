package com.henashi.inventorycrm.dto;

import com.henashi.inventorycrm.pojo.Gift;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for Gift entity.
 */
public record GiftDTO (
    Long id,
    String name,
    String code,
    Gift.GiftType type,
    Long productId,
    String productName,
    String description,
    Gift.GiftStatus status,
    Boolean limitEnabled,
    Integer limitPerPerson,
    LocalDateTime createdTime,
    LocalDateTime updatedTime,
    String remark)
{
}
