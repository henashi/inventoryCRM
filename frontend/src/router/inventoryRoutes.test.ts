import { describe, expect, it } from 'vitest'
import { inventoryModuleRoutes } from './inventoryModuleRoutes'

describe('inventory routes', () => {
  it('uses inventory overview as the main inventory entry', () => {
    const route = inventoryModuleRoutes.find((item) => item.path === '/inventory')

    expect(route?.name).toBe('InventoryOverview')
  })

  it('exposes detail and logs routes under the inventory module', () => {
    expect(inventoryModuleRoutes.find((item) => item.path === '/inventory/:id')?.name).toBe(
      'InventoryDetail',
    )
    expect(inventoryModuleRoutes.find((item) => item.path === '/inventory/logs')?.name).toBe(
      'InventoryLogs',
    )
  })
})
