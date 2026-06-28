import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { message } from 'ant-design-vue'
import { authApi } from '@/api/auth'
import type {
  ChangePasswordRequest,
  LoginRequest,
  LoginResponse,
  UpdateProfileRequest,
  User,
} from '@/types/auth'

type LogoutOptions = {
  notify?: boolean
  remote?: boolean
}

const ACCESS_TOKEN_KEY = 'token'
const REFRESH_TOKEN_KEY = 'refreshToken'
const USER_KEY = 'user'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem(ACCESS_TOKEN_KEY))
  const refreshToken = ref<string | null>(localStorage.getItem(REFRESH_TOKEN_KEY))
  const user = ref<User | null>(null)
  const loading = ref(false)

  const isAuthenticated = computed(() => !!token.value)
  const userRole = computed(() => user.value?.role)
  const userName = computed(() => user.value?.realName || user.value?.username)

  const normalizeUser = (input: User | null): User | null => {
    if (!input) {
      return null
    }

    return {
      ...input,
      realName: input.realName || input.username,
      email: input.email || '',
      role: (input.role || 'USER').toUpperCase() as User['role'],
    }
  }

  const setAccessToken = (nextToken: string | null) => {
    token.value = nextToken
    if (nextToken) {
      localStorage.setItem(ACCESS_TOKEN_KEY, nextToken)
    } else {
      localStorage.removeItem(ACCESS_TOKEN_KEY)
    }
  }

  const setRefreshToken = (nextRefreshToken: string | null) => {
    refreshToken.value = nextRefreshToken
    if (nextRefreshToken) {
      localStorage.setItem(REFRESH_TOKEN_KEY, nextRefreshToken)
    } else {
      localStorage.removeItem(REFRESH_TOKEN_KEY)
    }
  }

  const persistUser = (nextUser: User | null) => {
    user.value = normalizeUser(nextUser)
    if (user.value) {
      localStorage.setItem(USER_KEY, JSON.stringify(user.value))
    } else {
      localStorage.removeItem(USER_KEY)
    }
  }

  const setSession = (
    responseData: Pick<
      LoginResponse,
      'token' | 'access_token' | 'accessToken' | 'refreshToken' | 'refresh_token' | 'user' | 'data'
    >,
  ) => {
    const nextAccessToken =
      responseData.token || responseData.access_token || responseData.accessToken || null
    const nextRefreshToken = responseData.refreshToken || responseData.refresh_token || null

    setAccessToken(nextAccessToken)
    setRefreshToken(nextRefreshToken)

    const respUser = responseData.user || responseData.data?.user || null
    if (respUser) {
      persistUser(respUser)
    }
  }

  const clearAuthState = (notify = true) => {
    setAccessToken(null)
    setRefreshToken(null)
    persistUser(null)
    sessionStorage.clear()

    if (notify) {
      message.success('已退出登录')
    }
  }

  const login = async (request: LoginRequest) => {
    try {
      loading.value = true
      const responseData: LoginResponse = await authApi.login(request)
      const tokenStr = responseData.token || responseData.access_token || responseData.accessToken

      if (!tokenStr) {
        return { success: false, error: '未收到访问令牌' }
      }

      setSession(responseData)

      if (!user.value) {
        await checkAuth()
      }

      message.success('登录成功')
      return { success: true, data: responseData }
    } catch (err) {
      const errorMsg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message || '登录失败'
      return { success: false, error: errorMsg }
    } finally {
      loading.value = false
    }
  }

  const logout = async (options: LogoutOptions = {}) => {
    const { notify = true, remote = true } = options

    try {
      if (remote && (token.value || localStorage.getItem(ACCESS_TOKEN_KEY))) {
        await authApi.logout()
      }
    } catch {
    } finally {
      clearAuthState(notify)
    }
  }

  const checkAuth = async () => {
    if (!token.value) return false

    try {
      const res = await authApi.getCurrentUser()
      persistUser(res || null)
      return true
    } catch {
      await logout({ notify: false, remote: false })
      return false
    }
  }

  const changePassword = async (request: ChangePasswordRequest) => {
    try {
      await authApi.changePassword(request)
      clearAuthState(false)
      message.success('密码已修改，请重新登录')
      return { success: true as const }
    } catch (err) {
      return {
        success: false as const,
        error: (err as { response?: { data?: { message?: string } } })?.response?.data?.message || '修改密码失败',
      }
    }
  }

  const updateProfile = async (request: UpdateProfileRequest) => {
    try {
      const nextUser = await authApi.updateProfile(request)
      persistUser(nextUser)
      message.success('个人资料已更新')
      return { success: true as const, data: nextUser }
    } catch (err) {
      return {
        success: false as const,
        error: (err as { response?: { data?: { message?: string } } })?.response?.data?.message || '更新个人资料失败',
      }
    }
  }

  const updateUserInfo = (newUserInfo: Partial<User>) => {
    if (user.value) {
      persistUser({ ...user.value, ...newUserInfo })
    }
  }

  const initFromStorage = () => {
    setAccessToken(localStorage.getItem(ACCESS_TOKEN_KEY))
    setRefreshToken(localStorage.getItem(REFRESH_TOKEN_KEY))

    const storedUser = localStorage.getItem(USER_KEY)
    if (storedUser) {
      try {
        persistUser(JSON.parse(storedUser))
      } catch {
        persistUser(null)
      }
    }
  }

  return {
    token,
    refreshToken,
    user,
    loading,
    isAuthenticated,
    userRole,
    userName,
    login,
    logout,
    checkAuth,
    changePassword,
    updateProfile,
    updateUserInfo,
    initFromStorage,
    setAccessToken,
    setRefreshToken,
    setSession,
    clearAuthState,
  }
})
