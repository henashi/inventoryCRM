import { describe, expect, it } from 'vitest'
import {
  canAccessFeature,
  canDeleteGift,
  canDeleteGiftLog,
  canManageGiftCatalog,
  filterDashboardStats,
  filterQuickActions,
  resolveHomePath,
} from './accessControl'

describe('accessControl', () => {
  it('resolves dashboard as the shared authenticated home for manager', () => {
    expect(resolveHomePath('MANAGER')).toBe('/dashboard')
  })

  it('blocks user from restricted P0 features', () => {
    expect(canAccessFeature('USER', 'customers')).toBe(false)
    expect(canAccessFeature('USER', 'inventory')).toBe(false)
    expect(canAccessFeature('USER', 'data-dicts')).toBe(false)
    expect(canAccessFeature('USER', 'gift-logs')).toBe(true)
    expect(canAccessFeature('USER', 'account')).toBe(true)
  })

  it('keeps manager out of admin-only config features', () => {
    expect(canAccessFeature('MANAGER', 'customers')).toBe(true)
    expect(canAccessFeature('MANAGER', 'inventory')).toBe(true)
    expect(canAccessFeature('MANAGER', 'data-dicts')).toBe(false)
  })

  it('keeps inventory quick action for manager but still blocks placeholders and restricted actions', () => {
    const actions = [
      { action: 'addCustomer' },
      { action: 'addInventory' },
      { action: 'createOrder' },
      { action: 'distributeGift' },
      { action: 'exportReport' },
      { action: 'systemSettings' },
    ] as const

    expect(filterQuickActions(actions, 'MANAGER').map((item) => item.action)).toEqual([
      'addCustomer',
      'addInventory',
      'distributeGift',
      'exportReport',
    ])

    expect(filterQuickActions(actions, 'USER').map((item) => item.action)).toEqual([
      'distributeGift',
      'exportReport',
    ])
  })

  it('filters dashboard cards so user only sees routes that stay usable', () => {
    const stats = [
      { type: 'customers' },
      { type: 'products' },
      { type: 'inventory' },
      { type: 'gift-logs' },
      { type: 'gifts' },
      { type: 'dataDicts' },
    ] as const

    expect(filterDashboardStats(stats, 'USER').map((item) => item.type)).toEqual([
      'products',
      'gift-logs',
      'gifts',
    ])
  })

  it('allows manager to maintain gifts but only admin to delete gifts and gift logs', () => {
    expect(canManageGiftCatalog('ADMIN')).toBe(true)
    expect(canManageGiftCatalog('MANAGER')).toBe(true)
    expect(canManageGiftCatalog('USER')).toBe(false)

    expect(canDeleteGift('ADMIN')).toBe(true)
    expect(canDeleteGift('MANAGER')).toBe(false)
    expect(canDeleteGiftLog('ADMIN')).toBe(true)
    expect(canDeleteGiftLog('USER')).toBe(false)
  })
})
