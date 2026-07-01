// @vitest-environment jsdom
import { describe, it, expect, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { mountShallow } from '../../test-utils/mountShallow'

// Mock vue-router before importing component
const mockPush = vi.fn()
const mockRoute = {
  path: '/gift-logs',
  query: {},
  params: {},
  fullPath: '/gift-logs',
  hash: '',
  matched: [],
  redirectedFrom: undefined,
  name: 'gift-logs',
  meta: {},
}

vi.mock('vue-router', () => ({
  useRoute: () => mockRoute,
  useRouter: () => ({ push: mockPush, replace: vi.fn(), back: vi.fn() }),
  createRouter: vi.fn(),
  createWebHistory: vi.fn(),
}))

// Mock stores and API before importing component
vi.mock('@/stores/auth', () => ({
  useAuthStore: () => ({
    user: { username: 'admin', role: 'ADMIN' },
    hasPermission: () => true,
    permissions: {},
    permissionList: [],
  }),
}))

vi.mock('@/stores/giftLog', () => ({
  useGiftLogStore: () => ({
    giftLogs: [],
    loading: false,
    pagination: { current: 1, pageSize: 10, total: 0 },
    fetchGiftLogs: vi.fn(),
    deleteGiftLog: vi.fn(),
  }),
}))

vi.mock('@/stores/gift', () => ({
  useGiftStore: () => ({
    gifts: [],
    loadGifts: vi.fn(),
  }),
}))

vi.mock('@/api/customer', () => ({
  customerApi: {
    getCustomers: vi.fn().mockResolvedValue({ content: [] }),
  },
}))

vi.mock('@/router/accessControl', () => ({
  canDeleteGiftLog: vi.fn().mockReturnValue(true),
}))

vi.mock('@/utils/pagination', () => ({
  buildServerPageParams: vi.fn(),
  toServerPage: vi.fn(),
}))

vi.mock('@/utils/featureEnhancements', () => ({
  resolveGiftLogFilterState: vi.fn().mockReturnValue({}),
}))

import GiftLogList from './GiftLogList.vue'

describe('GiftLogList mount-based', () => {
  it('renders the gift log page container with header', async () => {
    setActivePinia(createPinia())

    const wrapper = mountShallow(GiftLogList)

    expect(wrapper.find('.gift-log-page').exists()).toBe(true)
    expect(wrapper.find('.page-header').exists()).toBe(true)
    expect(wrapper.text()).toContain('支持查看发放详情')
  })
})
