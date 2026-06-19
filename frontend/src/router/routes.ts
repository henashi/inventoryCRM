import type { RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { getFeatureRoles, resolveHomePath } from './accessControl'
import { inventoryModuleRoutes } from './inventoryModuleRoutes'

export const appRoutes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: () => resolveHomePath(useAuthStore().userRole),
    meta: { requiresAuth: true }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/auth/Login.vue'),
    meta: { requiresAuth: false, title: '登录' }
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('../views/dashboard/Dashboard.vue'),
    meta: { requiresAuth: true, title: '仪表板', roles: getFeatureRoles('dashboard') }
  },
  {
    path: '/account',
    name: 'AccountCenter',
    component: () => import('../views/account/AccountCenter.vue'),
    meta: { requiresAuth: true, title: '个人中心', roles: getFeatureRoles('account') }
  },
  {
    path: '/customers',
    name: 'CustomerList',
    component: () => import('../views/customer/CustomerList.vue'),
    meta: { requiresAuth: true, title: '客户管理', roles: getFeatureRoles('customers') }
  },
  {
    path: '/customers/:id',
    name: 'CustomerDetail',
    component: () => import('../views/customer/CustomerDetail.vue'),
    meta: { requiresAuth: true, title: '客户详情', roles: getFeatureRoles('customers') }
  },
  {
    path: '/products/:code?',
    name: 'ProductList',
    component: () => import('../views/product/ProductList.vue'),
    meta: { requiresAuth: true, title: '商品管理', roles: getFeatureRoles('products'), mode: 'list-or-detail' }
  },
  ...inventoryModuleRoutes,
  {
    path: '/gifts',
    name: 'GiftList',
    component: () => import('../views/gift/GiftList.vue'),
    meta: { requiresAuth: true, title: '礼品管理', roles: getFeatureRoles('gifts') }
  },
  {
    path: '/gift-logs',
    name: 'GiftLogs',
    component: () => import('../views/giftLog/GiftLogList.vue'),
    meta: { requiresAuth: true, title: '礼品发放', roles: getFeatureRoles('gift-logs') }
  },
  {
    path: '/data-dicts',
    name: 'DataDicts',
    component: () => import('../views/dataDict/DataDict.vue'),
    meta: { requiresAuth: true, title: '配置管理', roles: getFeatureRoles('data-dicts') }
  },
  {
    path: '/admin',
    name: 'Admin',
    component: () => import('../views/admin/Admin.vue'),
    meta: { requiresAuth: true, title: '系统管理', roles: getFeatureRoles('admin') }
  },
  {
    path: '/forbidden',
    name: 'Forbidden',
    component: () => import('../views/error/403.vue'),
    meta: { requiresAuth: false, title: '禁止访问' }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('../views/error/404.vue'),
    meta: { requiresAuth: false }
  }
]
