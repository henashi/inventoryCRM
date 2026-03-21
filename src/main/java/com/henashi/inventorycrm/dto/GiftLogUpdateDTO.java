package com.henashi.inventorycrm.dto;

import com.henashi.inventorycrm.pojo.GiftLog;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.Length;

public record GiftLogUpdateDTO(
        // 必填信息
        @NotNull(message = "客户ID不能为空")
        Long customerId,

        @NotNull(message = "礼品ID不能为空")
        Long giftId,

        @Size(max = 50, message = "操作人不能超过50字符")
        String operator,

        @Size(max = 200, message = "备注不能超过200字符")
        String remark,

        @Min(1)
        Integer quantity,

        @Length(max = 50)
        String issueNotes,

        GiftLog.GiftLogStatus status
) {
}
