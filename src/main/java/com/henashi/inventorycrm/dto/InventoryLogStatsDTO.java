package com.henashi.inventorycrm.dto;

public record InventoryLogStatsDTO (
     Long inCount,
     Long outCount,
     Long inQuantity,
     Long outQuantity)
{
}