package com.henashi.inventorycrm.dto;

import com.henashi.inventorycrm.pojo.InventoryLog;
import io.swagger.v3.oas.annotations.media.Schema;

public record InventoryLogTypeStatsDTO (
        @Schema(description = "操作类型")
        InventoryLog.LogType type,
        @Schema(description = "总数量")
        Long quantityCount,
        @Schema(description = "总次数")
        Long count)
{
}

