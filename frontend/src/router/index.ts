import { createRouter, createWebHistory, type RouteLocationNormalized } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/dashboard',
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
      meta: { requiresAuth: true, title: '仪表板', roles: ['ADMIN', 'USER'] }
    },
    {
      path: '/customers',
      name: 'CustomerList',
      component: () => import('../views/customer/CustomerList.vue'),
      meta: { requiresAuth: true, title: '客户管理', roles: ['ADMIN', 'MANAGER'] }
    },
    {
      path: '/customers/:id',
      name: 'CustomerDetail',
      component: () => import('../views/customer/CustomerDetail.vue'),
      meta: { requiresAuth: true, title: '客户详情' }
    },
    {
      path: '/products/:code?',
      name: 'ProductList',
      component: () => import('../views/product/ProductList.vue'),
      meta: { requiresAuth: true, title: '商品管理', roles: ['ADMIN', 'USER'], mode: 'list-or-detail' }
    },
    {
      path: '/inventory',
      name: 'InventoryLogs',
      component: () => import('../views/inventory/InventoryLogList.vue'),
      meta: { requiresAuth: true, title: '库存日志', roles: ['ADMIN', 'USER'] }
    },
    {
      path: '/gifts',
      name: 'GiftList',
      component: () => import('../views/gift/GiftList.vue'),
      meta: { requiresAuth: true, title: '礼品管理', roles: ['ADMIN', 'USER'] }
    },
    {
      path: '/gift-logs',
      name: 'GiftLogs',
      component: () => import('../views/giftLog/GiftLogList.vue'),
    },
    {
      path: '/data-dicts',
      name: 'DataDicts',
      component: () => import('../views/dataDict/DataDict.vue')
    },
    {
      path: '/admin',
      name: 'Admin',
      component: () => import('../views/admin/Admin.vue'),
      meta: { requiresAuth: true, title: '系统管理', roles: ['ADMIN'] }
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
})

// 全局路由守卫
router.beforeEach((to: RouteLocationNormalized, from, next) => {
  const authStore = useAuthStore()
  const isAuthenticated = authStore.isAuthenticated
  const userRole = authStore.user?.role

  // 设置页面标题
  if (to.meta.title) {
    document.title = `${to.meta.title} - 库存CRM`
  }

  // 如果路由需要认证
  if (to.meta.requiresAuth) {
    if (!isAuthenticated) {
      // 未登录，重定向到登录页
      next({
        path: '/login',
        query: { redirect: to.fullPath }  // 保存原始路径
      })
    } else if (to.meta.roles) {
      // 检查角色权限
      const requiredRoles = to.meta.roles as string[]
      if (userRole && requiredRoles.includes(userRole)) {
        next()
      } else {
        // 权限不足
        next({ name: 'Forbidden' })
      }
    } else {
      next()
    }
  } else {
    // 公开页面
    if (to.path === '/login' && isAuthenticated) {
      // 已登录用户访问登录页，重定向到首页
      next('/dashboard')
    } else {
      next()
    }
  }
})

export default router
