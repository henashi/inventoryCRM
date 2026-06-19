package com.henashi.inventorycrm.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record CustomerBatchStatusUpdateResultDTO(
        @Schema(description = "是否成功")
        Boolean success,

        @Schema(description = "成功更新数量")
        Integer updatedCount,

        @Schema(description = "目标状态，0=停用，1=正常")
        Integer targetStatus,

        @Schema(description = "结果消息")
        String message
) {
}
