package com.henashi.inventorycrm.dto;

import com.henashi.inventorycrm.pojo.Gift;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

import java.time.LocalDateTime;

public record GiftCreateDTO (
        @NotNull
        @Length(max = 100)
        String name,
        @NotNull
        @Length(max = 50)
        String code,
        @NotNull
        Gift.GiftType type,
        @NotNull
        Long productId,
        @Length(max = 200)
        String description,
        @NotNull
        Gift.GiftStatus status,
        @NotNull
        Boolean limitEnabled,
        Integer limitPerPerson,
        LocalDateTime createdTime,
        LocalDateTime updatedTime,
        @Length(max = 200)
        String remark)
{
}
