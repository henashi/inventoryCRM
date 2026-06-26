package com.henashi.inventorycrm.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record InventoryLogStatsDTO (
     @Schema(description = "入库次数")
     Long inCount,
     @Schema(description = "出库次数")
     Long outCount,
     @Schema(description = "入库总数量")
     Long inQuantity,
     @Schema(description = "出库总数量")
     Long outQuantity,
     @Schema(description = "总操作次数")
     Long totalOperations,
     @Schema(description = "成功次数")
     Long successCount,
     @Schema(description = "失败次数")
     Long failureCount,
     @Schema(description = "成功率")
     Double successRate,
     @Schema(description = "平均耗时(ms)")
     Long avgCostTime
)
{
}