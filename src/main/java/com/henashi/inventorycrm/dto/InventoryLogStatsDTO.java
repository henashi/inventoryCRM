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
     Long outQuantity)
{
}