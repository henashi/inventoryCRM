import dayjs from 'dayjs'

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

type GiftLogFilterSource = 'customer-detail' | 'dashboard' | 'customers' | 'standalone'

type GiftLogFilterState = {
  customerId?: number
  customerName: string
  hasCustomerContext: boolean
  contextLabel: string
  source: GiftLogFilterSource
}

type TrendPeriod = 'week' | 'month' | 'quarter'

type TrendPointSource = {
  registeredAt?: string
  createdAt?: string
}

type GiftLogTrendPointSource = {
  createdTime?: string
  issuedAt?: string
}

type LineChartData = {
  labels: string[]
  datasets: Array<{
    label: string
    data: number[]
    borderColor?: string
    backgroundColor?: string
    tension?: number
  }>
}

type DashboardTrendChartOptions = {
  period: TrendPeriod
  customers: TrendPointSource[]
  giftLogs: GiftLogTrendPointSource[]
  now?: string | Date
}

type OperationLogSummarySource = {
  status?: number | string
  module?: string
  operator?: string
  executionTime?: number
  operationTime?: string
}

type OperationLogSummary = {
  total: number
  successCount: number
  failureCount: number
  successRate: number
  avgExecutionTime: number
  latestModule: string
  latestOperator: string
}

type ConfigCategorySource = {
  groupCode?: string
  groupName?: string
  configGroup?: string
  configKey?: string
  status?: string | number
  updatedTime?: string
  updatedAt?: string
  createdTime?: string
}

type ConfigCategoryStat = {
  key: string
  label: string
  count: number
  activeCount: number
  latestUpdatedAt?: string
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

const toPositiveNumber = (value: unknown) => {
  const numericValue = Number(value)
  return Number.isFinite(numericValue) && numericValue > 0 ? numericValue : undefined
}

const toTrimmedString = (value: unknown) => {
  if (typeof value !== 'string') {
    return ''
  }

  return value.trim()
}

const toSource = (value: unknown): GiftLogFilterSource => {
  if (value === 'customer-detail' || value === 'dashboard' || value === 'customers') {
    return value
  }

  return 'standalone'
}

const createEmptyBucketValues = (size: number) => Array.from({ length: size }, () => 0)

const countByBucket = (
  items: Array<string | undefined>,
  buckets: dayjs.Dayjs[],
  unit: 'day' | 'month',
) => {
  const counts = createEmptyBucketValues(buckets.length)

  items.forEach((item) => {
    if (!item) {
      return
    }

    const value = dayjs(item)
    if (!value.isValid()) {
      return
    }

    const index = buckets.findIndex((bucket) => bucket.isSame(value, unit))
    if (index >= 0) {
      counts[index] += 1
    }
  })

  return counts
}

const resolveTimestamp = (value?: string) => {
  if (!value) {
    return 0
  }

  const dateValue = dayjs(value)
  return dateValue.isValid() ? dateValue.valueOf() : 0
}

const resolveConfigKey = (item: ConfigCategorySource) => {
  return item.groupCode?.trim() || item.configGroup?.trim() || 'ungrouped'
}

const resolveConfigLabel = (item: ConfigCategorySource) => {
  return item.groupName?.trim() || item.configGroup?.trim() || item.groupCode?.trim() || '未分组'
}

const resolveConfigUpdatedAt = (item: ConfigCategorySource) => {
  return item.updatedTime || item.updatedAt || item.createdTime
}

const isConfigActive = (status?: string | number) => {
  if (status === undefined || status === null || status === '') {
    return true
  }

  if (typeof status === 'number') {
    return status === 1
  }

  const normalizedStatus = status.toUpperCase()
  return normalizedStatus !== 'PAUSED' && normalizedStatus !== 'DISABLED' && normalizedStatus !== 'INACTIVE' && normalizedStatus !== '0'
}

const buildWeekTrendChart = (
  customers: TrendPointSource[],
  giftLogs: GiftLogTrendPointSource[],
  now: dayjs.Dayjs,
): LineChartData => {
  const buckets = Array.from({ length: 7 }, (_, index) => now.subtract(6 - index, 'day').startOf('day'))

  return {
    labels: buckets.map((bucket) => bucket.format('MM-DD')),
    datasets: [
      {
        label: '新增客户',
        data: countByBucket(customers.map((item) => item.registeredAt || item.createdAt), buckets, 'day'),
        borderColor: '#1890ff',
        backgroundColor: 'rgba(24, 144, 255, 0.1)',
        tension: 0.35,
      },
      {
        label: '礼品发放',
        data: countByBucket(giftLogs.map((item) => item.issuedAt || item.createdTime), buckets, 'day'),
        borderColor: '#52c41a',
        backgroundColor: 'rgba(82, 196, 26, 0.1)',
        tension: 0.35,
      },
    ],
  }
}

const buildMonthTrendChart = (
  customers: TrendPointSource[],
  giftLogs: GiftLogTrendPointSource[],
  now: dayjs.Dayjs,
): LineChartData => {
  const totalDays = now.date()
  const buckets = Array.from({ length: totalDays }, (_, index) => now.startOf('month').add(index, 'day'))

  return {
    labels: buckets.map((bucket) => bucket.format('MM-DD')),
    datasets: [
      {
        label: '新增客户',
        data: countByBucket(customers.map((item) => item.registeredAt || item.createdAt), buckets, 'day'),
        borderColor: '#1890ff',
        backgroundColor: 'rgba(24, 144, 255, 0.1)',
        tension: 0.35,
      },
      {
        label: '礼品发放',
        data: countByBucket(giftLogs.map((item) => item.issuedAt || item.createdTime), buckets, 'day'),
        borderColor: '#52c41a',
        backgroundColor: 'rgba(82, 196, 26, 0.1)',
        tension: 0.35,
      },
    ],
  }
}

const buildQuarterTrendChart = (
  customers: TrendPointSource[],
  giftLogs: GiftLogTrendPointSource[],
  now: dayjs.Dayjs,
): LineChartData => {
  const buckets = Array.from({ length: 3 }, (_, index) => now.startOf('month').subtract(2 - index, 'month'))

  return {
    labels: buckets.map((bucket) => bucket.format('M月')),
    datasets: [
      {
        label: '新增客户',
        data: countByBucket(customers.map((item) => item.registeredAt || item.createdAt), buckets, 'month'),
        borderColor: '#1890ff',
        backgroundColor: 'rgba(24, 144, 255, 0.1)',
        tension: 0.35,
      },
      {
        label: '礼品发放',
        data: countByBucket(giftLogs.map((item) => item.issuedAt || item.createdTime), buckets, 'month'),
        borderColor: '#52c41a',
        backgroundColor: 'rgba(82, 196, 26, 0.1)',
        tension: 0.35,
      },
    ],
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

export const buildCustomerDetailBackTarget = (query: Record<string, unknown>) => {
  if (query.from === 'dashboard') {
    return '/dashboard'
  }

  if (query.from === 'customer-detail') {
    const relatedCustomerId = toPositiveNumber(query.customerId)
    if (relatedCustomerId) {
      return `/customers/${relatedCustomerId}`
    }
  }

  return '/customers'
}

export const resolveGiftLogFilterState = (query: Record<string, unknown>): GiftLogFilterState => {
  const customerId = toPositiveNumber(query.customerId)
  const customerName = toTrimmedString(query.customerName)
  const source = toSource(query.from)

  return {
    customerId,
    customerName,
    hasCustomerContext: customerId !== undefined,
    contextLabel: customerId !== undefined && customerName ? `客户“${customerName}”` : '',
    source,
  }
}

export const buildDashboardTrendChart = ({
  period,
  customers,
  giftLogs,
  now,
}: DashboardTrendChartOptions): LineChartData => {
  const currentTime = dayjs(now || new Date())

  if (period === 'quarter') {
    return buildQuarterTrendChart(customers, giftLogs, currentTime)
  }

  if (period === 'month') {
    return buildMonthTrendChart(customers, giftLogs, currentTime)
  }

  return buildWeekTrendChart(customers, giftLogs, currentTime)
}

export const buildOperationLogSummary = (
  logs: OperationLogSummarySource[],
): OperationLogSummary => {
  const total = logs.length
  if (!total) {
    return {
      total: 0,
      successCount: 0,
      failureCount: 0,
      successRate: 0,
      avgExecutionTime: 0,
      latestModule: '',
      latestOperator: '',
    }
  }

  const successCount = logs.filter((item) => Number(item.status) === 1).length
  const totalExecutionTime = logs.reduce((sum, item) => sum + Number(item.executionTime || 0), 0)
  const latestLog = [...logs].sort((left, right) => resolveTimestamp(right.operationTime) - resolveTimestamp(left.operationTime))[0]

  return {
    total,
    successCount,
    failureCount: total - successCount,
    successRate: Number(((successCount / total) * 100).toFixed(1)),
    avgExecutionTime: Math.round(totalExecutionTime / total),
    latestModule: latestLog?.module || '',
    latestOperator: latestLog?.operator || '',
  }
}

export const buildConfigCategoryStats = (
  items: ConfigCategorySource[],
  limit = 4,
): ConfigCategoryStat[] => {
  const groups = new Map<string, ConfigCategoryStat>()

  items.forEach((item) => {
    const key = resolveConfigKey(item)
    const label = resolveConfigLabel(item)
    const latestUpdatedAt = resolveConfigUpdatedAt(item)
    const current = groups.get(key) || {
      key,
      label,
      count: 0,
      activeCount: 0,
      latestUpdatedAt,
    }

    current.count += 1
    if (isConfigActive(item.status)) {
      current.activeCount += 1
    }

    if (resolveTimestamp(latestUpdatedAt) > resolveTimestamp(current.latestUpdatedAt)) {
      current.latestUpdatedAt = latestUpdatedAt
    }

    groups.set(key, current)
  })

  return Array.from(groups.values())
    .sort((left, right) => (
      right.count - left.count
      || resolveTimestamp(right.latestUpdatedAt) - resolveTimestamp(left.latestUpdatedAt)
      || left.label.localeCompare(right.label, 'zh-CN')
    ))
    .slice(0, limit)
}

export type {
  CategoryFilterState,
  ConfigCategoryStat,
  CustomerStatistics,
  GiftLogFilterState,
  ImportFailureDetail,
  ImportMeta,
  ImportResult,
  LineChartData,
  OperationLogSummary,
  ProductQuickFilter,
  ProductStockStatistics,
  ProductSummaryCard,
  TrendPeriod,
}
