import type { RouteRecordRaw } from 'vue-router'
import { getFeatureRoles } from './accessControl'

export const inventoryModuleRoutes: RouteRecordRaw[] = [
  {
    path: '/inventory',
    name: 'InventoryOverview',
    component: () => import('../views/inventory/InventoryOverview.vue'),
    meta: { requiresAuth: true, title: '库存总览', roles: getFeatureRoles('inventory') },
  },
  {
    path: '/inventory/predictions',
    name: 'StockPrediction',
    component: () => import('../views/inventory/StockPrediction.vue'),
    meta: { requiresAuth: true, title: 'AI 库存预测', roles: getFeatureRoles('ai') },
  },
  {
    path: '/inventory/logs',
    name: 'InventoryLogs',
    component: () => import('../views/inventory/InventoryLogList.vue'),
    meta: { requiresAuth: true, title: '库存日志', roles: getFeatureRoles('inventory') },
  },
  {
    path: '/inventory/:id',
    name: 'InventoryDetail',
    component: () => import('../views/inventory/InventoryDetail.vue'),
    meta: { requiresAuth: true, title: '库存详情', roles: getFeatureRoles('inventory') },
  },
]
