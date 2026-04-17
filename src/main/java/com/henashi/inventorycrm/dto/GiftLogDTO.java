package com.henashi.inventorycrm.dto;

import com.henashi.inventorycrm.pojo.GiftLog;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record GiftLogDTO(
        @Schema(description = "日志ID")
        Long id,

        @Schema(description = "客户ID")
        Long customerId,

        @Schema(description = "礼品ID")
        Long giftId,

        @Schema(description = "操作类型")
        String operationType,

        @Schema(description = "操作数量")
        Integer quantity,

        @Schema(description = "客户名称")
        String customerName,

        @Schema(description = "礼品名称")
        String giftName,

        @Schema(description = "发放时间")
        LocalDateTime issueAt,

        @Schema(description = "创建时间")
        LocalDateTime createdTime,

        @Schema(description = "状态更新时间")
        LocalDateTime statusUpdatedTime,

        @Schema(description = "数据更新时间")
        LocalDateTime contentUpdatedTime,

        @Schema(description = "发放备注")
        String issueNotes,

        @Schema(description = "状态")
        GiftLog.GiftLogStatus status,

        @Schema(description = "操作员")
        String operator,

        @Schema(description = "备注")
        String remark
) {
}

