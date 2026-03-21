// frontend/src/stores/auth.ts
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api/auth'
import type { LoginRequest, User, LoginResponse } from '@/types/auth'
import { message } from 'ant-design-vue'

export const useAuthStore = defineStore('auth', () => {
  // 状态
  const token = ref<string | null>(localStorage.getItem('token'))
  const user = ref<User | null>(null)
  const loading = ref(false)

  // Getter
  const isAuthenticated = computed(() => !!token.value)
  const userRole = computed(() => user.value?.role)
  const userName = computed(() => user.value?.username)

  // Actions
  const login = async (request: LoginRequest) => {
    try {
      loading.value = true
      const res = await authApi.login(request)

      // request 的响应拦截器已返回 response.data，res 就是后端返回的主体数据
      const responseData: any = res

      // 兼容不同后端字段命名：支持 token 或 access_token
      const tokenStr = responseData.token || responseData.access_token || responseData.accessToken

        if (!tokenStr) { 
          return { success: false, error: '未收到访问令牌' } 
      }

      // 保存token
      token.value = tokenStr
      localStorage.setItem('token', tokenStr)

      // 保存用户信息（兼容 data.user 或 user）
      const respUser = responseData.user || responseData.data?.user || null
      if (respUser) {
        // 规范角色为大写，避免后端大小写差异导致前端权限判断失败
        if (respUser.role && typeof respUser.role === 'string') {
          respUser.role = respUser.role.toUpperCase()
        }
        user.value = respUser
        localStorage.setItem('user', JSON.stringify(user.value))
        // 如果后端返回的用户信息缺少 role 字段，尝试通过 /auth/me 获取完整信息
        if (!respUser.role) {
          await checkAuth()
        }
      } else {
        // 如果登录接口未返回用户信息，尝试通过 /auth/me 获取当前用户信息
        user.value = null
        await checkAuth()
      }

      message.success('登录成功')
      return { success: true, data: responseData }
    } catch (error: any) {
      const errorMsg = error.response?.data?.message || '登录失败'
        // 不在 store 中弹出全局提示，交由调用方（如 Login.vue）展示内联或 toast
      return { success: false, error: errorMsg }
    } finally {
      loading.value = false
    }
  }

  const logout = () => {
    // 清除本地存储
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    sessionStorage.clear()
    
    // 清除状态
    token.value = null
    user.value = null
    
    message.success('已退出登录')
  }

  const checkAuth = async () => {
    if (!token.value) return false

    try {
      // authApi.getCurrentUser() 通过 request 拦截器返回的通常是 response.data
      const res = await authApi.getCurrentUser()
      // 直接将返回的数据作为用户信息保存
      user.value = res || null
      if (user.value && user.value.role && typeof user.value.role === 'string') {
        user.value.role = user.value.role.toUpperCase()
      }
      if (user.value) localStorage.setItem('user', JSON.stringify(user.value))
      return true
    } catch (error) {
      logout()
      return false
    }
  }

  const updateUserInfo = (newUserInfo: Partial<User>) => {
    if (user.value) {
      user.value = { ...user.value, ...newUserInfo }
      localStorage.setItem('user', JSON.stringify(user.value))
    }
  }

  // 初始化时从localStorage恢复用户信息
  const initFromStorage = () => {
    const storedUser = localStorage.getItem('user')
    if (storedUser) {
      try {
        const parsed = JSON.parse(storedUser)
        if (parsed && parsed.role && typeof parsed.role === 'string') parsed.role = parsed.role.toUpperCase()
        user.value = parsed
      } catch {
        user.value = null
      }
    }
  }

  return {
    token,
    user,
    loading,
    isAuthenticated,
    userRole,
    userName,
    login,
    logout,
    checkAuth,
    updateUserInfo,
    initFromStorage
  }
})