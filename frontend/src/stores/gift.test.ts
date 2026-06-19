import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useGiftStore } from './gift'

const giftApiMocks = vi.hoisted(() => ({
  loadGifts: vi.fn(),
  createGift: vi.fn(),
  updateGift: vi.fn(),
  deleteGift: vi.fn(),
  getGift: vi.fn(),
}))

vi.mock('@/api/gift', () => ({
  giftApi: giftApiMocks,
}))

describe('gift store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('keeps UI pagination as 1-based and reuses the current page after create/update/delete', async () => {
    giftApiMocks.loadGifts.mockResolvedValue({
      content: [{ id: 1, name: '首屏礼品', code: 'G001' }],
      totalElements: 1,
      totalPages: 1,
      size: 10,
      number: 0,
      first: true,
      last: true,
      empty: false,
    })

    const store = useGiftStore()

    await store.loadGifts({ page: 0, size: 10 })

    expect(store.pagination.page).toBe(1)
    expect(giftApiMocks.loadGifts).toHaveBeenNthCalledWith(1, { page: 0, size: 10 })

    giftApiMocks.createGift.mockResolvedValue(undefined)
    giftApiMocks.updateGift.mockResolvedValue(undefined)
    giftApiMocks.deleteGift.mockResolvedValue(undefined)

    await store.createGift({
      name: '新礼品',
      code: 'G002',
      status: 'ACTIVE',
      type: 'PHYSICAL',
      limitEnabled: false,
      isDeleted: 0,
    })
    await store.updateGift(1, {
      id: 1,
      name: '更新礼品',
      code: 'G001',
      status: 'ACTIVE',
      type: 'PHYSICAL',
      limitEnabled: false,
      isDeleted: 0,
    })
    await store.deleteGift(1)

    expect(giftApiMocks.loadGifts).toHaveBeenNthCalledWith(2, { page: 0, size: 10 })
    expect(giftApiMocks.loadGifts).toHaveBeenNthCalledWith(3, { page: 0, size: 10 })
    expect(giftApiMocks.loadGifts).toHaveBeenNthCalledWith(4, { page: 0, size: 10 })
  })
})
