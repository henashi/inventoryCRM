import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'

const componentSource = readFileSync(new URL('./GiftList.vue', import.meta.url), 'utf-8')

describe('GiftList layout', () => {
  it('renders the gift page with header and table card', () => {
    expect(componentSource).toContain('class="gift-page"')
    expect(componentSource).toContain('class="page-header"')
    expect(componentSource).toContain('class="table-card"')
    expect(componentSource).toContain('class="page-actions"')
  })

  it('renders action buttons for add gift and view logs', () => {
    expect(componentSource).toContain('@click="showAddModal"')
    expect(componentSource).toContain('@click="handleViewDistributionLogs"')
    expect(componentSource).toContain('新增礼品')
    expect(componentSource).toContain('查看发放日志')
  })

  it('renders read-only alert for non-admin users', () => {
    expect(componentSource).toContain('v-if="!canManageCatalog"')
    expect(componentSource).toContain('class="page-alert"')
    expect(componentSource).toContain('当前账号为只读视角')
  })

  it('renders table columns: name, code, type, product, status, limitEnabled, limitPerPerson, action', () => {
    expect(componentSource).toContain("dataIndex: 'name'")
    expect(componentSource).toContain("dataIndex: 'code'")
    expect(componentSource).toContain("dataIndex: 'type'")
    expect(componentSource).toContain("dataIndex: 'product'")
    expect(componentSource).toContain("dataIndex: 'status'")
    expect(componentSource).toContain("dataIndex: 'limitEnabled'")
    expect(componentSource).toContain("dataIndex: 'limitPerPerson'")
    expect(componentSource).toContain("key: 'action'")
  })

  it('renders bodyCell templates for status, type, product, limitEnabled columns', () => {
    expect(componentSource).toContain("column.dataIndex === 'limitEnabled'")
    expect(componentSource).toContain("column.dataIndex === 'status'")
    expect(componentSource).toContain("column.dataIndex === 'product'")
    expect(componentSource).toContain("column.dataIndex === 'type'")
  })

  it('renders add/edit modal with gift form fields', () => {
    expect(componentSource).toContain('v-model:visible="modalVisible"')
    expect(componentSource).toContain(':title="modalTitle"')
    expect(componentSource).toContain('label="名称" name="name"')
    expect(componentSource).toContain('label="礼品类型" name="type"')
    expect(componentSource).toContain('label="关联商品" name="productId"')
    expect(componentSource).toContain('label="开启限制" name="limitEnabled"')
    expect(componentSource).toContain('label="限制数量" name="limitPerPerson"')
    expect(componentSource).toContain('label="描述" name="description"')
    expect(componentSource).toContain('label="备注" name="remark"')
  })

  it('renders detail drawer with gift descriptions', () => {
    expect(componentSource).toContain('v-model:open="detailVisible"')
    expect(componentSource).toContain('title="礼品详情"')
    expect(componentSource).toContain('detailGift.name')
    expect(componentSource).toContain('detailGift.code')
    expect(componentSource).toContain('detailGift.productName')
  })

  it('contains type labels: 邀约礼品/实体礼品/虚拟礼品/优惠券/积分', () => {
    expect(componentSource).toContain("return '邀约礼品'")
    expect(componentSource).toContain("return '实体礼品'")
    expect(componentSource).toContain("return '虚拟礼品'")
    expect(componentSource).toContain("return '优惠券'")
    expect(componentSource).toContain("return '积分'")
  })

  it('contains status labels: 进行中/已售罄/已过期/已暂停', () => {
    expect(componentSource).toContain("return '进行中'")
    expect(componentSource).toContain("return '已售罄'")
    expect(componentSource).toContain("return '已过期'")
    expect(componentSource).toContain("return '已暂停'")
  })
})
