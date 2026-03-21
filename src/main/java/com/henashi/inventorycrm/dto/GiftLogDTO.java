package com.henashi.inventorycrm.dto;

import com.henashi.inventorycrm.pojo.GiftLog;

import java.time.LocalDateTime;

public record GiftLogDTO(
        Long id,
        Long customerId,
        Long giftId,
        Integer quantity,
        String customerName,
        String giftName,
        LocalDateTime issueAt,
        LocalDateTime createdTime,
        LocalDateTime updatedTime,
        String issueNotes,
        GiftLog.GiftLogStatus status,
        String operator,
        String remark
) {
}
