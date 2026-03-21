package com.henashi.inventorycrm.enums;
/**
 * 操作类型枚举
 */
public enum OperationType {
    STOCK_IN,      // 入库
    STOCK_OUT,     // 出库
    STOCK_ADJUST,  // 库存调整
    STOCK_TRANSFER, // 库存调拨
    STOCK_CORRECTION // 库存修正
}
