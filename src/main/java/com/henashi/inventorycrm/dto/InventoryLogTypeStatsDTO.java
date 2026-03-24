package com.henashi.inventorycrm.dto;

import com.henashi.inventorycrm.pojo.InventoryLog;

public record InventoryLogTypeStatsDTO (
        InventoryLog.LogType type,
        Long quantityCount,
        Long count)
{
}

