package com.henashi.inventorycrm.dto;

public record InventoryLogStats(
     Long inCount,
     Long outCount,
     Long inQuantity,
     Long outQuantity)
{
}
