import type { RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { getFeatureRoles, resolveHomePath } from './accessControl'
export const appRoutes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('../layouts/BasicLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        redirect: () => resolveHomePath(useAuthStore().userRole),
      },
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/dashboard/Dashboard.vue'),
        meta: { requiresAuth: true, title: '仪表盘' },
      },
      {
        path: 'customers',
        name: 'CustomerList',
        component: () => import('../views/customer/CustomerList.vue'),
        meta: { requiresAuth: true, title: '客户管理', roles: getFeatureRoles('customers') },
      },
      {
        path: 'customers/:id',
        name: 'CustomerDetail',
        component: () => import('../views/customer/CustomerDetail.vue'),
        meta: { requiresAuth: true, title: '客户详情', roles: getFeatureRoles('customers') },
      },
      {
        path: 'products',
        name: 'ProductList',
        component: () => import('../views/product/ProductList.vue'),
        meta: {
          requiresAuth: true,
          title: '商品管理',
          roles: getFeatureRoles('products'),
          mode: 'list-or-detail',
        },
      },
      {
        path: 'orders',
        name: 'OrderList',
        component: () => import('../views/order/OrderList.vue'),
        meta: { requiresAuth: true, title: '订单管理', roles: getFeatureRoles('products') },
      },
      {
        path: 'inventory',
        name: 'InventoryOverview',
        component: () => import('../views/inventory/InventoryOverview.vue'),
        meta: { requiresAuth: true, title: '库存总览', roles: getFeatureRoles('inventory') },
      },
      {
        path: 'inventory/predictions',
        name: 'StockPrediction',
        component: () => import('../views/inventory/StockPrediction.vue'),
        meta: { requiresAuth: true, title: 'AI 库存预测', roles: getFeatureRoles('ai') },
      },
      {
        path: 'inventory/logs',
        name: 'InventoryLogs',
        component: () => import('../views/inventory/InventoryLogList.vue'),
        meta: { requiresAuth: true, title: '库存操作日志', roles: getFeatureRoles('inventory') },
      },
      {
        path: 'inventory/:id',
        name: 'InventoryDetail',
        component: () => import('../views/inventory/InventoryDetail.vue'),
        meta: { requiresAuth: true, title: '库存详情', roles: getFeatureRoles('inventory') },
      },
      {
        path: 'ai/customers/scores',
        name: 'CustomerScoring',
        component: () => import('../views/customer/CustomerScoring.vue'),
        meta: { requiresAuth: true, title: '客户评分', roles: getFeatureRoles('ai') },
      },
      {
        path: 'ai/customers/gift-recommendations',
        name: 'GiftRecommendation',
        component: () => import('../views/gift/GiftRecommendation.vue'),
        meta: { requiresAuth: true, title: 'AI 礼品推荐', roles: getFeatureRoles('ai') },
      },
      {
        path: 'ai/assistant',
        name: 'AiAssistant',
        component: () => import('../views/ai/AiAssistant.vue'),
        meta: { requiresAuth: true, title: 'AI 运营助手', roles: getFeatureRoles('ai') },
      },
      {
        path: 'admin',
        redirect: '/data-dicts',
      },
      {
        path: 'roles',
        name: 'RoleList',
        component: () => import('../views/admin/RoleList.vue'),
        meta: { requiresAuth: true, title: '角色管理', roles: getFeatureRoles('admin') },
      },
      {
        path: 'permission-defs',
        name: 'PermissionDefList',
        component: () => import('../views/admin/PermissionDefList.vue'),
        meta: { requiresAuth: true, title: '权限管理', roles: getFeatureRoles('admin') },
      },
      {
        path: 'roles/:roleId/permissions/:roleName',
        name: 'RolePermission',
        component: () => import('../views/admin/RolePermission.vue'),
        meta: { requiresAuth: true, title: '角色权限配置', roles: getFeatureRoles('admin') },
      },
      {
        path: 'users',
        name: 'UserManagement',
        component: () => import('../views/admin/UserManagement.vue'),
        meta: { requiresAuth: true, title: '用户管理', roles: getFeatureRoles('admin') },
      },
      {
        path: 'data-dicts',
        name: 'DataDict',
        component: () => import('../views/dataDict/DataDict.vue'),
        meta: { requiresAuth: true, title: '配置管理', roles: getFeatureRoles('data-dicts') },
      },
      {
        path: 'gifts',
        name: 'GiftList',
        component: () => import('../views/gift/GiftList.vue'),
        meta: { requiresAuth: true, title: '礼品管理', roles: getFeatureRoles('gifts') },
      },
      {
        path: 'gift-logs',
        name: 'GiftLogs',
        component: () => import('../views/giftLog/GiftLogList.vue'),
        meta: { requiresAuth: true, title: '礼品发放日志', roles: getFeatureRoles('gift-logs') },
      },
      {
        path: 'account',
        name: 'AccountCenter',
        component: () => import('../views/account/AccountCenter.vue'),
        meta: { requiresAuth: true, title: '个人中心', roles: getFeatureRoles('account') },
      },
      {
        path: 'operation-logs',
        name: 'OperationLogs',
        component: () => import('../views/operationLog/OperationLogList.vue'),
        meta: { requiresAuth: true, title: '系统日志', roles: getFeatureRoles('operation-logs') },
      },
    ],
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/auth/Login.vue'),
    meta: {
      requiresAuth: false,
      title: '登录',
      roles: getFeatureRoles('login' as any),
      layout: 'blank',
    },
  },
]
