// @ts-nocheck
import { describe, expect, it } from 'vitest'
import {
  buildCategoryFilterState,
  buildConfigCategoryStats,
  buildCustomerDetailBackTarget,
  buildCustomerImportMeta,
  buildDashboardStats,
  buildDashboardTrendChart,
  buildGiftDistribution,
  buildOperationLogSummary,
  buildProductImportMeta,
  buildProductListSummary,
  resolveGiftLogFilterState,
  resolveProductListParams,
} from './featureEnhancements'

describe('featureEnhancements', () => {
  it('uses backend customer import metadata as-is', () => {
    const meta = buildCustomerImportMeta({
      successCount: 2,
      failureCount: 1,
      failureDetails: [{ rowNumber: 3, identifier: '13800000000', reason: '手机号重复' }],
      templateFields: ['name', 'phone'],
      requiredFields: ['name'],
      duplicateStrategy: '按手机号判重',
      notes: ['仅支持 UTF-8 编码的 CSV 文件'],
    })

    expect(meta).not.toBeNull()
    expect(meta!.templateFields).toEqual(['name', 'phone'])
    expect(meta!.requiredFields).toEqual(['name'])
    expect(meta!.notes).toContain('仅支持 UTF-8 编码的 CSV 文件')
  })

  it('does not fabricate customer import metadata when backend metadata is unavailable', () => {
    expect(buildCustomerImportMeta()).toBeNull()
  })

  it('does not fabricate product import metadata when backend metadata is unavailable', () => {
    expect(buildProductImportMeta()).toBeNull()
  })

  it('returns empty-state config when product categories are unavailable', () => {
    expect(buildCategoryFilterState([])).toEqual({
      showSelect: false,
      emptyText: '暂无分类可筛选',
    })
  })

  it('maps customer and stock statistics to live dashboard cards', () => {
    const stats = buildDashboardStats(
      {
        totalCustomers: 125,
        normalCustomers: 117,
        disabledCustomers: 8,
        giftLevelDistribution: { 0: 80, 1: 20, 2: 15, 3: 10 },
      },
      {
        totalProducts: 32,
        activeProducts: 28,
        lowStockProducts: 6,
        outOfStockProducts: 2,
        totalStockQuantity: 640,
        totalStockValue: 15880.5,
      },
    )

    expect(stats).toEqual([
      expect.objectContaining({ title: '总客户数', value: '125', change: '117 正常 / 8 停用' }),
      expect.objectContaining({ title: '商品总数', value: '32', change: '28 在售 / 6 低库存' }),
      expect.objectContaining({ title: '库存总量', value: '640', change: '2 缺货 / ¥15880.50' }),
    ])
  })

  it('builds pie chart data from gift level distribution', () => {
    const chart = buildGiftDistribution({
      totalCustomers: 10,
      normalCustomers: 9,
      disabledCustomers: 1,
      giftLevelDistribution: { 0: 4, 1: 3, 2: 2, 3: 1 },
    })

    expect(chart.labels).toEqual(['普通客户', '等级1', '等级2', '等级3'])
    expect(chart.datasets[0]!.data).toEqual([4, 3, 2, 1])
  })

  it('builds product list summary cards from stock statistics', () => {
    const summary = buildProductListSummary({
      totalProducts: 18,
      activeProducts: 16,
      lowStockProducts: 5,
      outOfStockProducts: 2,
      totalStockQuantity: 300,
      totalStockValue: 8600,
    })

    expect(summary).toEqual([
      expect.objectContaining({ key: 'totalProducts', value: '18', helper: '16 个在售' }),
      expect.objectContaining({ key: 'lowStockProducts', value: '5', helper: '2 个缺货' }),
      expect.objectContaining({
        key: 'totalStockValue',
        value: '¥8600.00',
        helper: '库存总量 300',
      }),
    ])
  })

  it('keeps page search on list API while applying quick stock filters', () => {
    expect(resolveProductListParams({ keyword: '咖啡', category: '饮品' }, 'all')).toEqual({
      keyword: '咖啡',
      category: '饮品',
    })

    expect(resolveProductListParams({ keyword: '咖啡', category: '饮品' }, 'lowStock')).toEqual({
      keyword: '咖啡',
      category: '饮品',
      stockStatus: 'lowStock',
    })

    expect(resolveProductListParams({ keyword: '', category: undefined }, 'outOfStock')).toEqual({
      stockStatus: 'outOfStock',
    })
  })

  it('resolves customer detail back target from source hints', () => {
    expect(buildCustomerDetailBackTarget({ from: 'dashboard' })).toBe('/dashboard')
    expect(buildCustomerDetailBackTarget({ from: 'customer-detail', customerId: '12' })).toBe(
      '/customers/12',
    )
    expect(buildCustomerDetailBackTarget({})).toBe('/customers')
  })

  it('turns route query into explicit gift log customer context state', () => {
    expect(
      resolveGiftLogFilterState({ customerId: '7', customerName: '张三', from: 'customer-detail' }),
    ).toEqual({
      customerId: 7,
      customerName: '张三',
      hasCustomerContext: true,
      contextLabel: '客户“张三”',
      source: 'customer-detail',
    })

    expect(resolveGiftLogFilterState({ customerId: '0', customerName: '   ' })).toEqual({
      customerId: undefined,
      customerName: '',
      hasCustomerContext: false,
      contextLabel: '',
      source: 'standalone',
    })
  })

  it('builds live dashboard trend data from real customer and gift log records', () => {
    const chart = buildDashboardTrendChart({
      period: 'week',
      now: '2026-06-22T12:00:00',
      customers: [
        { registeredAt: '2026-06-22T09:00:00' },
        { registeredAt: '2026-06-21T10:00:00' },
        { registeredAt: '2026-06-21T11:00:00' },
      ],
      giftLogs: [{ createdTime: '2026-06-22T13:00:00' }, { createdTime: '2026-06-20T13:00:00' }],
    })

    expect(chart.labels).toHaveLength(7)
    expect(chart.datasets[0]).toEqual(expect.objectContaining({ label: '新增客户' }))
    expect(chart.datasets[1]).toEqual(expect.objectContaining({ label: '礼品发放' }))
    expect(chart.datasets[0]!.data.at(-2)).toBe(2)
    expect(chart.datasets[0]!.data.at(-1)).toBe(1)
    expect(chart.datasets[1]!.data.at(-3)).toBe(1)
    expect(chart.datasets[1]!.data.at(-1)).toBe(1)
  })

  it('summarizes recent operation logs for admin overview', () => {
    const summary = buildOperationLogSummary([
      {
        module: '客户管理',
        status: 1,
        operator: 'Alice',
        executionTime: 120,
        operationTime: '2026-06-22T09:00:00',
      },
      {
        module: '库存管理',
        status: 0,
        operator: 'Bob',
        executionTime: 240,
        operationTime: '2026-06-22T10:00:00',
      },
      {
        module: '库存管理',
        status: 1,
        operator: 'Carol',
        executionTime: 60,
        operationTime: '2026-06-22T08:00:00',
      },
    ])

    expect(summary).toEqual({
      total: 3,
      successCount: 2,
      failureCount: 1,
      successRate: 66.7,
      avgExecutionTime: 140,
      latestModule: '库存管理',
      latestOperator: 'Bob',
    })
  })

  it('builds config category stats from current configuration records', () => {
    const stats = buildConfigCategoryStats([
      {
        groupCode: 'gift',
        groupName: '礼品规则',
        status: 'ACTIVE',
        updatedTime: '2026-06-20T09:00:00',
      },
      {
        groupCode: 'gift',
        groupName: '礼品规则',
        status: 'PAUSED',
        updatedTime: '2026-06-21T09:00:00',
      },
      { configGroup: 'security', configKey: 'jwt.expire', updatedAt: '2026-06-22T09:00:00' },
      { configGroup: 'security', configKey: 'refresh.expire', updatedAt: '2026-06-18T09:00:00' },
      { configGroup: 'security', configKey: 'password.rule', updatedAt: '2026-06-19T09:00:00' },
    ])

    expect(stats).toEqual([
      expect.objectContaining({
        key: 'security',
        label: 'security',
        count: 3,
        activeCount: 3,
        latestUpdatedAt: '2026-06-22T09:00:00',
      }),
      expect.objectContaining({
        key: 'gift',
        label: '礼品规则',
        count: 2,
        activeCount: 1,
        latestUpdatedAt: '2026-06-21T09:00:00',
      }),
    ])
  })
})
