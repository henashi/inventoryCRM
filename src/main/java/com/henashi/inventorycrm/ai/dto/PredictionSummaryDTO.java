package com.henashi.inventorycrm.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 预测概览统计
 */
@Schema(description = "预测概览统计")
public record PredictionSummaryDTO(

        @Schema(description = "已预测商品总数")
        int totalPredicted,

        @Schema(description = "高危商品数（≤7天耗尽）")
        int dangerCount,

        @Schema(description = "预警商品数（8-14天耗尽）")
        int warningCount,

        @Schema(description = "正常商品数")
        int normalCount,

        @Schema(description = "建议补货总量")
        int totalSuggestedRestockQty,

        @Schema(description = "预测执行时间（毫秒）")
        long executionTimeMs

) {
}
