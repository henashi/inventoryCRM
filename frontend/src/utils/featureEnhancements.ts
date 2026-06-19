type ImportFailureDetail = {
  rowNumber: number
  identifier?: string
  reason: string
}

type ImportResult = {
  successCount: number
  failureCount: number
  failureDetails: ImportFailureDetail[]
  templateFields?: string[]
  requiredFields?: string[]
  duplicateStrategy?: string
  notes?: string[]
}

type ImportMeta = {
  templateFields: string[]
  requiredFields: string[]
  duplicateStrategy: string
  notes: string[]
}

type CustomerStatistics = {
  totalCustomers: number
  normalCustomers: number
  disabledCustomers: number
  giftLevelDistribution: Record<number, number>
}

type ProductStockStatistics = {
  totalProducts: number
  activeProducts: number
  lowStockProducts: number
  outOfStockProducts: number
  totalStockQuantity: number
  totalStockValue: number
}

type DashboardStat = {
  title: string
  value: string
  change: string
}

type ProductSummaryCard = {
  key: 'totalProducts' | 'lowStockProducts' | 'totalStockValue'
  title: string
  value: string
  helper: string
}

type ProductQuickFilter = 'all' | 'lowStock' | 'outOfStock'

type ProductListFilters = {
  keyword?: string
  category?: string
}

type CategoryFilterState = {
  showSelect: boolean
  emptyText: string
}

const productSummaryLabels = {
  totalProducts: '商品总数',
  lowStockProducts: '低库存商品',
  totalStockValue: '库存货值',
}

const toCurrency = (value: number) => `¥${value.toFixed(2)}`

const extractImportMeta = (result?: Partial<ImportResult>): ImportMeta | null => {
  if (
    !result?.templateFields?.length
    || !result.requiredFields?.length
    || !result.duplicateStrategy
    || !result.notes?.length
  ) {
    return null
  }

  return {
    templateFields: result.templateFields,
    requiredFields: result.requiredFields,
    duplicateStrategy: result.duplicateStrategy,
    notes: result.notes,
  }
}

export const buildCustomerImportMeta = (result?: Partial<ImportResult>) => extractImportMeta(result)

export const buildProductImportMeta = (result?: Partial<ImportResult>) => extractImportMeta(result)

export const buildCategoryFilterState = (categories: string[]): CategoryFilterState => {
  if (categories.length > 0) {
    return {
      showSelect: true,
      emptyText: '',
    }
  }

  return {
    showSelect: false,
    emptyText: '暂无分类可筛选',
  }
}

export const buildDashboardStats = (
  customerStats: CustomerStatistics,
  productStats: ProductStockStatistics,
): DashboardStat[] => ([
  {
    title: '总客户数',
    value: String(customerStats.totalCustomers),
    change: `${customerStats.normalCustomers} 正常 / ${customerStats.disabledCustomers} 停用`,
  },
  {
    title: '商品总数',
    value: String(productStats.totalProducts),
    change: `${productStats.activeProducts} 在售 / ${productStats.lowStockProducts} 低库存`,
  },
  {
    title: '库存总量',
    value: String(productStats.totalStockQuantity),
    change: `${productStats.outOfStockProducts} 缺货 / ${toCurrency(productStats.totalStockValue)}`,
  },
])

export const buildGiftDistribution = (customerStats: CustomerStatistics) => ({
  labels: ['普通客户', '等级1', '等级2', '等级3'],
  datasets: [
    {
      data: [0, 1, 2, 3].map((level) => customerStats.giftLevelDistribution[level] || 0),
      backgroundColor: ['#d9d9d9', '#69b1ff', '#95de64', '#ffc069'],
    },
  ],
})

export const buildProductListSummary = (
  productStats: ProductStockStatistics,
): ProductSummaryCard[] => ([
  {
    key: 'totalProducts',
    title: productSummaryLabels.totalProducts,
    value: String(productStats.totalProducts),
    helper: `${productStats.activeProducts} 个在售`,
  },
  {
    key: 'lowStockProducts',
    title: productSummaryLabels.lowStockProducts,
    value: String(productStats.lowStockProducts),
    helper: `${productStats.outOfStockProducts} 个缺货`,
  },
  {
    key: 'totalStockValue',
    title: productSummaryLabels.totalStockValue,
    value: toCurrency(productStats.totalStockValue),
    helper: `库存总量 ${productStats.totalStockQuantity}`,
  },
])

export const resolveProductListParams = (
  filters: ProductListFilters,
  quickFilter: ProductQuickFilter,
) => {
  const params = Object.fromEntries(
    Object.entries({
      keyword: filters.keyword?.trim() || undefined,
      category: filters.category || undefined,
    }).filter(([_, value]) => value !== undefined),
  ) as Record<string, string>

  if (quickFilter !== 'all') {
    params.stockStatus = quickFilter
  }

  return params
}

export type {
  CategoryFilterState,
  CustomerStatistics,
  ImportFailureDetail,
  ImportMeta,
  ImportResult,
  ProductQuickFilter,
  ProductStockStatistics,
  ProductSummaryCard,
}
