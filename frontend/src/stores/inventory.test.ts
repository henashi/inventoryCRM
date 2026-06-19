import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useInventoryStore } from './inventory'

const inventoryApiMocks = vi.hoisted(() => ({
  getInventories: vi.fn(),
  getInventory: vi.fn(),
  getStockAlerts: vi.fn(),
  stockIn: vi.fn(),
  stockOut: vi.fn(),
  adjustStock: vi.fn(),
  exportInventoryReport: vi.fn(),
}))

vi.mock('@/api/inventory', () => ({
  inventoryApi: inventoryApiMocks,
}))

describe('inventory store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('uses five items as the default inventory page size', async () => {
    inventoryApiMocks.getInventories.mockResolvedValueOnce({
      content: [{ id: 1, productId: 1, productCode: 'P001', productName: '默认分页商品', currentStock: 5, safeStock: 10, maxStock: 20, unit: '件', status: 1 }],
      totalElements: 1,
      totalPages: 1,
      size: 5,
      number: 0,
      first: true,
      last: true,
      empty: false,
    })

    const store = useInventoryStore()
    await store.loadInventories()

    expect(inventoryApiMocks.getInventories).toHaveBeenCalledWith({ page: 0, size: 5 })
    expect(store.pagination.size).toBe(5)
    expect(store.lastQuery.size).toBe(5)
  })

  it('loads selectable inventories independently from the current paged list', async () => {
    inventoryApiMocks.getInventories.mockResolvedValueOnce({
      content: [{ id: 1, productId: 1, productCode: 'P001', productName: '当前页商品', currentStock: 5, safeStock: 10, maxStock: 20, unit: '件', status: 1 }],
      totalElements: 1,
      totalPages: 1,
      size: 10,
      number: 0,
      first: true,
      last: true,
      empty: false,
    })
    inventoryApiMocks.getInventories.mockResolvedValueOnce({
      content: [
        { id: 1, productId: 1, productCode: 'P001', productName: '当前页商品', currentStock: 5, safeStock: 10, maxStock: 20, unit: '件', status: 1 },
        { id: 2, productId: 2, productCode: 'P002', productName: '翻页外商品', currentStock: 8, safeStock: 10, maxStock: 20, unit: '件', status: 1 },
      ],
      totalElements: 2,
      totalPages: 1,
      size: 1000,
      number: 0,
      first: true,
      last: true,
      empty: false,
    })

    const store = useInventoryStore()
    await store.loadInventories({ page: 0, size: 10, keyword: '当前页' })
    await (store as any).loadSelectableInventories()

    expect(inventoryApiMocks.getInventories).toHaveBeenNthCalledWith(1, { page: 0, size: 10, keyword: '当前页' })
    expect(inventoryApiMocks.getInventories).toHaveBeenNthCalledWith(2, { page: 0, size: 1000 })
    expect((store as any).selectableInventories).toHaveLength(2)
  })
})
