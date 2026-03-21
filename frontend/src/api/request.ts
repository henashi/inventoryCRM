// frontend/src/api/request.ts
import axios from 'axios'
import { message } from 'ant-design-vue'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'

// 创建axios实例
const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    const authStore = useAuthStore()
    const token = authStore.token || localStorage.getItem('token')
    
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    return response.data
  },
  (error) => {
    const authStore = useAuthStore()
    
    if (error.response) {
      const { status, data } = error.response
      
      switch (status) {
        case 401: { // 未授权
          const originalRequest: any = error.config || {}
          const requestUrl: string = originalRequest.url || ''

          // 登录接口返回 401（用户名或密码错误）时，不应该强制登出或重定向到登录页
          // 让 `auth.login` 自己显示用户可读错误，拦截器在这里保持静默以避免重复提示
          if (requestUrl.includes('/auth/login')) {
            break
          }

          // 如果是刷新令牌接口失败，或者当前有 token 且请求被拒绝，说明需要登出
          const existingToken = authStore.token || localStorage.getItem('token')
          if (requestUrl.includes('/auth/refresh-token') || existingToken) {
            message.error(data.message || '登录已过期')
            authStore.logout()
            // 避免导航循环：仅在不在登录页时跳转
            try {
              const cur = router.currentRoute && (router.currentRoute as any).value
              if (!cur || cur.path !== '/login') router.replace('/login')
            } catch {
              router.replace('/login')
            }
          } else {
            // 未登录状态的 401（例如访问受限资源）只给出提示，不强制跳转
            message.error(data.message || '未授权')
          }

          break
        }
        
        case 403: // 禁止访问
          message.error('权限不足')
          console.log('权限不足')
          router.push('/forbidden')
          break
        
        case 404: // 资源不存在
          message.error('请求的资源不存在')
          break
        
        case 500: // 服务器错误
          // message.error('服务器内部错误')
          break
        
        default:
          message.error(data.message || '请求失败')
      }
    } else if (error.request) {
      // 请求发送失败
      message.error('网络连接失败，请检查网络')
    } else {
      // 请求配置失败
      message.error('请求配置错误')
    }
    
    return Promise.reject(error)
  }
)

export default request