import { createRouter, createWebHistory, type RouteLocationNormalized } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { resolveHomePath } from './accessControl'
import { appRoutes } from './routes'

const router = createRouter({
  history: createWebHistory(),
  routes: appRoutes
})

router.beforeEach((to: RouteLocationNormalized, from, next) => {
  const authStore = useAuthStore()
  const isAuthenticated = authStore.isAuthenticated
  const userRole = authStore.userRole

  if (to.meta.title) {
    document.title = `${to.meta.title} - 库存CRM`
  }

  if (to.meta.requiresAuth) {
    if (!isAuthenticated) {
      next({
        path: '/login',
        query: { redirect: to.fullPath }
      })
      return
    }

    if (to.meta.roles) {
      const requiredRoles = to.meta.roles as string[]
      if (userRole && requiredRoles.includes(userRole)) {
        next()
      } else {
        next({ name: 'Forbidden' })
      }
      return
    }

    next()
    return
  }

  if (to.path === '/login' && isAuthenticated) {
    next(resolveHomePath(userRole))
    return
  }

  next()
})

export default router
