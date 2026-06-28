import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'

const componentSource = readFileSync(new URL('./InventoryDetail.vue', import.meta.url), 'utf-8')

describe('InventoryDetail layout', () => {
  it('renders the page container with page-header and description sections', () => {
    expect(componentSource).toContain('class="inventory-detail-page"')
    expect(componentSource).toContain('class="page-header"')
    expect(componentSource).toContain('class="page-subtitle"')
    expect(componentSource).toContain('查看库存状态、预警原因与最近变更记录')
  })

  it('contain action buttons: 返回, 查看日志, 入库, 出库, 调整', () => {
    expect(componentSource).toContain('@click="goBack"')
    expect(componentSource).toContain('@click="goToLogs"')
    expect(componentSource).toContain('@click="openAction(\'in\')"')
    expect(componentSource).toContain('@click="openAction(\'out\')"')
    expect(componentSource).toContain('@click="openAction(\'adjust\')"')
  })

  it('renders two side-by-side info cards using a-row gutter 16', () => {
    expect(componentSource).toContain(':xs="24" :xl="14"')
    expect(componentSource).toContain(':xs="24" :xl="10"')
    expect(componentSource).toContain('<a-row :gutter="16"')
  })

  it('renders basic info card with bordered descriptions', () => {
    expect(componentSource).toContain('title="基础信息"')
    expect(componentSource).toContain(':column="2" bordered')
    expect(componentSource).toContain('商品名称')
    expect(componentSource).toContain('商品编码')
    expect(componentSource).toContain('当前库存')
    expect(componentSource).toContain('安全库存')
    expect(componentSource).toContain('最大库存')
  })

  it('renders alert status card with alert-stats grid', () => {
    expect(componentSource).toContain('title="预警状态"')
    expect(componentSource).toContain('class="alert-card"')
    expect(componentSource).toContain('class="alert-stats"')
    expect(componentSource).toContain('grid-template-columns: repeat(2, minmax(0, 1fr))')
    expect(componentSource).toContain('当前库存处于安全范围内')
  })

  it('renders recent changes table card with type/quantity/operator columns', () => {
    expect(componentSource).toContain('title="最近变更记录"')
    expect(componentSource).toContain('class="changes-card"')
    expect(componentSource).toContain("dataIndex === 'changeType'")
    expect(componentSource).toContain("dataIndex === 'changeQuantity'")
    expect(componentSource).toContain("dataIndex === 'createdAt'")
    expect(componentSource).toContain('changeTypeColors')
    expect(componentSource).toContain('normChangeType')
  })

  it('renders positive/negative quantity classes', () => {
    expect(componentSource).toContain('.positive')
    expect(componentSource).toContain('color: #58a6ff')
    expect(componentSource).toContain('.negative')
    expect(componentSource).toContain('color: #a371f7')
  })

  it('renders inventory-action-modal component reference', () => {
    expect(componentSource).toContain('<InventoryActionModal')
    expect(componentSource).toContain('import InventoryActionModal from')
    expect(componentSource).toContain(':open="actionModalVisible"')
    expect(componentSource).toContain(':mode="actionMode"')
    expect(componentSource).toContain('@cancel="closeActionModal"')
    expect(componentSource).toContain('@submit="handleActionSubmit"')
  })

  it('applies dark mode overrides for positive/negative colors', () => {
    expect(componentSource).toContain("[data-theme='dark'] .positive")
    expect(componentSource).toContain("[data-theme='dark'] .negative")
    expect(componentSource).toContain("[data-theme='dark'] .ant-tag-has-color")
  })

  it('contains all textual operation labels: 入库/出库/调整/新建', () => {
    expect(componentSource).toContain("IN: '入库'")
    expect(componentSource).toContain("OUT: '出库'")
    expect(componentSource).toContain("ADJUST: '调整'")
    expect(componentSource).toContain("CREATE: '新建'")
  })
})
