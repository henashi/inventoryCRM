package com.henashi.inventorycrm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.Length;

public record GiftLogCreateDTO(
        // 必填信息
        @NotNull(message = "客户ID不能为空")
        @Schema(description = "客户ID")
        Long customerId,

        @NotNull(message = "礼品ID不能为空")
        @Schema(description = "礼品ID")
        Long giftId,

        @Size(max = 50, message = "操作人不能超过50字符")
        @Schema(description = "操作人")
        String operator,

        @Size(max = 200, message = "备注不能超过200字符")
        @Schema(description = "备注")
        String remark,

        @Min(1)
        @Schema(description = "操作数量")
        Integer quantity,

        @Length(max = 50)
        @Schema(description = "发放备注")
        String issueNotes
) {
}
