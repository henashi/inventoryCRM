/**
 * 权限标识常量
 * 与后端 Permissions.java 保持同步
 */
export const PERMISSIONS = {
  // ===== 仪表盘 =====
  DASHBOARD_VIEW: 'dashboard:view',

  // ===== 客户管理 =====
  CUSTOMERS_VIEW: 'customers:view',
  CUSTOMERS_CREATE: 'customers:create',
  CUSTOMERS_EDIT: 'customers:edit',
  CUSTOMERS_DELETE: 'customers:delete',
  CUSTOMERS_IMPORT: 'customers:import',
  CUSTOMERS_EXPORT: 'customers:export',

  // ===== 商品管理 =====
  PRODUCTS_VIEW: 'products:view',
  PRODUCTS_CREATE: 'products:create',
  PRODUCTS_EDIT: 'products:edit',
  PRODUCTS_DELETE: 'products:delete',
  PRODUCTS_IMPORT: 'products:import',
  PRODUCTS_EXPORT: 'products:export',
  PRODUCTS_ENABLE: 'products:enable',

  // ===== 库存管理 =====
  INVENTORY_VIEW: 'inventory:view',
  INVENTORY_STOCK_IN: 'inventory:stockIn',
  INVENTORY_STOCK_OUT: 'inventory:stockOut',
  INVENTORY_ADJUST: 'inventory:adjust',
  INVENTORY_EXPORT: 'inventory:export',

  // ===== 订单管理 =====
  ORDERS_VIEW: 'orders:view',
  ORDERS_CREATE: 'orders:create',
  ORDERS_DELETE: 'orders:delete',

  // ===== 礼品管理 =====
  GIFTS_VIEW: 'gifts:view',
  GIFTS_CREATE: 'gifts:create',
  GIFTS_EDIT: 'gifts:edit',
  GIFTS_DELETE: 'gifts:delete',

  // ===== 礼品发放日志 =====
  GIFT_LOGS_VIEW: 'giftLogs:view',
  GIFT_LOGS_DELETE: 'giftLogs:delete',

  // ===== 配置管理 =====
  DATA_DICTS_VIEW: 'dataDicts:view',
  DATA_DICTS_MANAGE: 'dataDicts:manage',

  // ===== 用户管理 =====
  USERS_VIEW: 'users:view',
  USERS_MANAGE: 'users:manage',

  // ===== 系统日志 =====
  OPERATION_LOGS_VIEW: 'operationLogs:view',

  // ===== AI 功能 =====
  AI_SCORING: 'ai:scoring',
  AI_RECOMMENDATION: 'ai:recommendation',
  AI_ASSISTANT: 'ai:assistant',
  AI_PREDICTION: 'ai:prediction',
} as const

export type PermissionKey = (typeof PERMISSIONS)[keyof typeof PERMISSIONS]
