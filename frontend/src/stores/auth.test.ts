import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from './auth'

const authApiMocks = vi.hoisted(() => ({
  login: vi.fn(),
  logout: vi.fn(),
  getCurrentUser: vi.fn(),
  refreshToken: vi.fn(),
  changePassword: vi.fn(),
  updateProfile: vi.fn(),
  register: vi.fn(),
}))

vi.mock('@/api/auth', () => ({
  authApi: authApiMocks,
}))

vi.mock('ant-design-vue', () => ({
  message: {
    success: vi.fn(),
    error: vi.fn(),
  },
}))

const createStorage = () => {
  const storage = new Map<string, string>()

  return {
    getItem: (key: string) => storage.get(key) ?? null,
    setItem: (key: string, value: string) => {
      storage.set(key, value)
    },
    removeItem: (key: string) => {
      storage.delete(key)
    },
    clear: () => {
      storage.clear()
    },
  }
}

describe('auth store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    vi.stubGlobal('localStorage', createStorage())
    vi.stubGlobal('sessionStorage', createStorage())
  })

  it('persists access token and refresh token after login', async () => {
    authApiMocks.login.mockResolvedValue({
      token: 'access-token',
      refreshToken: 'refresh-token',
      expiresIn: 3600,
      tokenType: 'Bearer',
      user: {
        id: 1,
        username: 'alice',
        realName: 'Alice',
        email: 'alice@example.com',
        role: 'ADMIN',
        status: 1,
      },
    })

    const store = useAuthStore()
    const result = await store.login({ username: 'alice', password: 'secret' })

    expect(result.success).toBe(true)
    expect(store.token).toBe('access-token')
    expect(store.refreshToken).toBe('refresh-token')
    expect(localStorage.getItem('token')).toBe('access-token')
    expect(localStorage.getItem('refreshToken')).toBe('refresh-token')
  })

  it('clears both tokens and cached user after password change', async () => {
    const store = useAuthStore()
    store.token = 'access-token' as never
    store.refreshToken = 'refresh-token' as never
    store.user = {
      id: 1,
      username: 'alice',
      realName: 'Alice',
      email: 'alice@example.com',
      role: 'ADMIN',
      status: 1,
    } as never
    localStorage.setItem('token', 'access-token')
    localStorage.setItem('refreshToken', 'refresh-token')
    localStorage.setItem('user', JSON.stringify(store.user))

    authApiMocks.changePassword.mockResolvedValue(undefined)

    const result = await store.changePassword({
      oldPassword: 'old-password',
      newPassword: 'new-password',
    })

    expect(result.success).toBe(true)
    expect(store.token).toBeNull()
    expect(store.refreshToken).toBeNull()
    expect(store.user).toBeNull()
    expect(localStorage.getItem('token')).toBeNull()
    expect(localStorage.getItem('refreshToken')).toBeNull()
    expect(localStorage.getItem('user')).toBeNull()
  })

  it('clears refresh token together with access token on logout', async () => {
    const store = useAuthStore()
    store.token = 'access-token' as never
    store.refreshToken = 'refresh-token' as never
    localStorage.setItem('token', 'access-token')
    localStorage.setItem('refreshToken', 'refresh-token')

    authApiMocks.logout.mockResolvedValue(undefined)

    await store.logout({ notify: false })

    expect(authApiMocks.logout).toHaveBeenCalledTimes(1)
    expect(store.token).toBeNull()
    expect(store.refreshToken).toBeNull()
    expect(localStorage.getItem('token')).toBeNull()
    expect(localStorage.getItem('refreshToken')).toBeNull()
  })
})
