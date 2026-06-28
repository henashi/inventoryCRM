import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'

const componentSource = readFileSync(new URL('./InventoryLogList.vue', import.meta.url), 'utf-8')

describe('InventoryLogList layout', () => {
  it('renders the page container with search and table cards', () => {
    expect(componentSource).toContain('class="inventory-log-page"')
    expect(componentSource).toContain('class="search-card"')
    expect(componentSource).toContain('class="table-card"')
    expect(componentSource).toContain('class="page-header"')
  })

  it('renders search form with product, type, operator and date range', () => {
    expect(componentSource).toContain('searchForm.productId')
    expect(componentSource).toContain('searchForm.type')
    expect(componentSource).toContain('searchForm.operator')
    expect(componentSource).toContain('searchForm.dateRange')
    expect(componentSource).toContain('<a-range-picker')
    expect(componentSource).toContain('@finish="handleSearch"')
  })

  it('renders table columns for log display', () => {
    expect(componentSource).toContain("dataIndex: 'productInfo'")
    expect(componentSource).toContain("dataIndex: 'type'")
    expect(componentSource).toContain("dataIndex: 'stockChange'")
    expect(componentSource).toContain("dataIndex: 'operator'")
    expect(componentSource).toContain("dataIndex: 'reason'")
    expect(componentSource).toContain("dataIndex: 'status'")
    expect(componentSource).toContain("dataIndex: 'createdAt'")
    expect(componentSource).toContain("dataIndex: 'actions'")
  })

  it('renders template bodyCell for each custom column', () => {
    expect(componentSource).toContain("column.dataIndex === 'createdAt'")
    expect(componentSource).toContain("column.dataIndex === 'productInfo'")
    expect(componentSource).toContain("column.dataIndex === 'type'")
    expect(componentSource).toContain("column.dataIndex === 'stockChange'")
    expect(componentSource).toContain("column.dataIndex === 'operator'")
    expect(componentSource).toContain("column.dataIndex === 'reason'")
    expect(componentSource).toContain("column.dataIndex === 'status'")
    expect(componentSource).toContain("column.dataIndex === 'actions'")
  })

  it('renders export button in page header', () => {
    expect(componentSource).toContain('@click="handleExport"')
    expect(componentSource).toContain(':loading="exportLoading"')
    expect(componentSource).toContain('getOperationTypeColor')
    expect(componentSource).toContain('getSimplifyTypeText')
  })

  it('renders detail drawer with log detail sections', () => {
    expect(componentSource).toContain('<a-drawer')
    expect(componentSource).toContain('v-model:open="detailVisible"')
    expect(componentSource).toContain('title="操作日志详情"')
    expect(componentSource).toContain('class="log-detail"')
  })

  it('contains operation type labels: 入库/出库/调整/调拨/盘点/新建', () => {
    expect(componentSource).toContain("IN: '入库'")
    expect(componentSource).toContain("OUT: '出库'")
    expect(componentSource).toContain("ADJUST: '调整'")
    expect(componentSource).toContain("TRANSFER: '调拨'")
    expect(componentSource).toContain("CHECK: '盘点'")
    expect(componentSource).toContain("CREATE: '新建'")
  })

  it('uses compact styling with small table size and responsive layout', () => {
    expect(componentSource).toContain('size="small"')
    expect(componentSource).toContain(':scroll="{ x: 1000 }"')
    expect(componentSource).toContain('.search-card :deep(.ant-card-body)')
    expect(componentSource).toContain('padding: 16px 20px;')
  })

  it('renders stock change visual classes: positive, negative, zero', () => {
    expect(componentSource).toContain('change-positive')
    expect(componentSource).toContain('change-negative')
    expect(componentSource).toContain('change-zero')
  })
})
