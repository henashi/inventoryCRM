package com.henashi.inventorycrm.constants;

/**
 * 权限标识常量
 * 与前端 frontend/src/constants/permissions.ts 保持同步
 */
public final class Permissions {

    private Permissions() {}

    // ===== 仪表盘 =====
    public static final String DASHBOARD_VIEW = "dashboard:view";

    // ===== 客户管理 =====
    public static final String CUSTOMERS_VIEW = "customers:view";
    public static final String CUSTOMERS_CREATE = "customers:create";
    public static final String CUSTOMERS_EDIT = "customers:edit";
    public static final String CUSTOMERS_DELETE = "customers:delete";
    public static final String CUSTOMERS_IMPORT = "customers:import";
    public static final String CUSTOMERS_EXPORT = "customers:export";

    // ===== 商品管理 =====
    public static final String PRODUCTS_VIEW = "products:view";
    public static final String PRODUCTS_CREATE = "products:create";
    public static final String PRODUCTS_EDIT = "products:edit";
    public static final String PRODUCTS_DELETE = "products:delete";
    public static final String PRODUCTS_IMPORT = "products:import";
    public static final String PRODUCTS_EXPORT = "products:export";
    public static final String PRODUCTS_ENABLE = "products:enable";

    // ===== 库存管理 =====
    public static final String INVENTORY_VIEW = "inventory:view";
    public static final String INVENTORY_STOCK_IN = "inventory:stockIn";
    public static final String INVENTORY_STOCK_OUT = "inventory:stockOut";
    public static final String INVENTORY_ADJUST = "inventory:adjust";
    public static final String INVENTORY_EXPORT = "inventory:export";

    // ===== 订单管理 =====
    public static final String ORDERS_VIEW = "orders:view";
    public static final String ORDERS_CREATE = "orders:create";
    public static final String ORDERS_DELETE = "orders:delete";

    // ===== 礼品管理 =====
    public static final String GIFTS_VIEW = "gifts:view";
    public static final String GIFTS_CREATE = "gifts:create";
    public static final String GIFTS_EDIT = "gifts:edit";
    public static final String GIFTS_DELETE = "gifts:delete";

    // ===== 礼品发放日志 =====
    public static final String GIFT_LOGS_VIEW = "giftLogs:view";
    public static final String GIFT_LOGS_DELETE = "giftLogs:delete";

    // ===== 配置管理 =====
    public static final String DATA_DICTS_VIEW = "dataDicts:view";
    public static final String DATA_DICTS_MANAGE = "dataDicts:manage";

    // ===== 用户管理 =====
    public static final String USERS_VIEW = "users:view";
    public static final String USERS_MANAGE = "users:manage";

    // ===== 系统日志 =====
    public static final String OPERATION_LOGS_VIEW = "operationLogs:view";

    // ===== AI 功能 =====
    public static final String AI_SCORING = "ai:scoring";
    public static final String AI_RECOMMENDATION = "ai:recommendation";
    public static final String AI_ASSISTANT = "ai:assistant";
    public static final String AI_PREDICTION = "ai:prediction";
}
