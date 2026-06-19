import axios from 'axios'
import type { AxiosRequestConfig, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { message } from 'ant-design-vue'
import router from '@/router'
import { useAuthStore } from '@/stores/auth'
import type { LoginResponse } from '@/types/auth'

type TypedRequest = {
  get<T = any>(url: string, config?: AxiosRequestConfig): Promise<T>
  delete<T = any>(url: string, config?: AxiosRequestConfig): Promise<T>
  post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T>
  put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T>
  patch<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T>
  interceptors: typeof axios.interceptors
}

type RetryableRequestConfig = InternalAxiosRequestConfig & {
  _retry?: boolean
}

const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'
let refreshPromise: Promise<string> | null = null

const request = axios.create({
  baseURL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

const resolveAccessToken = (responseData: LoginResponse) => {
  return responseData.token || responseData.access_token || responseData.accessToken || ''
}

const redirectToLogin = async (errorMessage?: string) => {
  const authStore = useAuthStore()
  await authStore.logout({ notify: false, remote: false })

  if (errorMessage) {
    message.error(errorMessage)
  }

  const currentRoute = router.currentRoute.value
  if (currentRoute.path !== '/login') {
    await router.replace({
      path: '/login',
      query: currentRoute.fullPath && currentRoute.fullPath !== '/login'
        ? { redirect: currentRoute.fullPath }
        : undefined,
    })
  }
}

const refreshAccessToken = async () => {
  const authStore = useAuthStore()
  const currentRefreshToken = authStore.refreshToken || localStorage.getItem('refreshToken')

  if (!currentRefreshToken) {
    throw new Error('missing refresh token')
  }

  if (!refreshPromise) {
    refreshPromise = axios.post<LoginResponse>(`${baseURL}/auth/refresh-token`, {
      refreshToken: currentRefreshToken,
    }, {
      headers: {
        'Content-Type': 'application/json',
      },
    }).then((response) => {
      const nextAccessToken = resolveAccessToken(response.data)

      if (!nextAccessToken) {
        throw new Error('missing access token')
      }

      authStore.setSession(response.data)
      return nextAccessToken
    }).finally(() => {
      refreshPromise = null
    })
  }

  return refreshPromise
}

request.interceptors.request.use(
  (config) => {
    const authStore = useAuthStore()
    const accessToken = authStore.token || localStorage.getItem('token')

    if (accessToken) {
      config.headers = config.headers || {}
      config.headers.Authorization = `Bearer ${accessToken}`
    }

    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

request.interceptors.response.use(
  <T>(response: AxiosResponse<T>) => {
    return response.data
  },
  async (error) => {
    const authStore = useAuthStore()

    if (error.response) {
      const { status, data } = error.response
      const originalRequest = (error.config || {}) as RetryableRequestConfig
      const requestUrl = originalRequest.url || ''
      const hasRefreshToken = !!(authStore.refreshToken || localStorage.getItem('refreshToken'))

      switch (status) {
        case 401: {
          if (requestUrl.includes('/auth/login')) {
            break
          }

          const canRetryWithRefresh = hasRefreshToken && !requestUrl.includes('/auth/refresh-token') && !originalRequest._retry

          if (canRetryWithRefresh) {
            try {
              originalRequest._retry = true
              const nextAccessToken = await refreshAccessToken()
              originalRequest.headers = originalRequest.headers || {}
              originalRequest.headers.Authorization = `Bearer ${nextAccessToken}`
              return request(originalRequest)
            } catch {
              await redirectToLogin(data.message || '登录已过期')
              return Promise.reject(error)
            }
          }

          await redirectToLogin(data.message || (hasRefreshToken ? '登录已过期' : '未授权'))
          break
        }

        case 403:
          message.error('权限不足')
          router.push('/forbidden')
          break

        case 404:
          message.error('请求的资源不存在')
          break

        case 500:
          break

        default:
          message.error(data.message || '请求失败')
      }
    } else if (error.request) {
      message.error('网络连接失败，请检查网络')
    } else {
      message.error('请求配置错误')
    }

    return Promise.reject(error)
  }
)

export default request as unknown as TypedRequest
