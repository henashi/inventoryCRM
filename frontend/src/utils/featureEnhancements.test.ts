import { describe, expect, it } from 'vitest'
import {
  buildCategoryFilterState,
  buildCustomerImportMeta,
  buildDashboardStats,
  buildGiftDistribution,
  buildProductImportMeta,
  buildProductListSummary,
  resolveProductListParams,
} from './featureEnhancements'

describe('featureEnhancements', () => {
  it('uses backend customer import metadata as-is', () => {
    const meta = buildCustomerImportMeta({
      successCount: 2,
      failureCount: 1,
      failureDetails: [
        { rowNumber: 3, identifier: '13800000000', reason: '手机号重复' },
      ],
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
      expect.objectContaining({ key: 'totalStockValue', value: '¥8600.00', helper: '库存总量 300' }),
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
})
