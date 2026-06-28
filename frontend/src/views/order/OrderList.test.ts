import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'

const componentSource = readFileSync(new URL('./OrderList.vue', import.meta.url), 'utf-8')

describe('OrderList layout', () => {
  it('renders the page container with header and title', () => {
    expect(componentSource).toContain('class="page-container"')
    expect(componentSource).toContain('class="page-header"')
    expect(componentSource).toContain('class="page-header-actions"')
  })

  it('renders add order button and table', () => {
    expect(componentSource).toContain('@click="showForm = true"')
    expect(componentSource).toContain('新增订单')
    expect(componentSource).toContain('row-key="id"')
  })

  it('renders table columns: customer, items, amount, time, remark, action', () => {
    expect(componentSource).toContain("key: 'customer'")
    expect(componentSource).toContain("key: 'items'")
    expect(componentSource).toContain("key: 'amount'")
    expect(componentSource).toContain("key: 'time'")
    expect(componentSource).toContain("dataIndex: 'remark'")
    expect(componentSource).toContain("key: 'action'")
  })

  it('renders bodyCell templates for customer/amount/time/items columns', () => {
    expect(componentSource).toContain("column.key === 'customer'")
    expect(componentSource).toContain("column.key === 'amount'")
    expect(componentSource).toContain("column.key === 'time'")
    expect(componentSource).toContain("column.key === 'items'")
    expect(componentSource).toContain("column.key === 'action'")
  })

  it('renders expanded row for order items', () => {
    expect(componentSource).toContain('#expandedRowRender')
    expect(componentSource).toContain('class="expanded-items"')
    expect(componentSource).toContain('class="item-row"')
    expect(componentSource).toContain('item.productName')
    expect(componentSource).toContain('item.totalAmount')
  })

  it('renders add order modal with form fields', () => {
    expect(componentSource).toContain('v-model:open="showForm"')
    expect(componentSource).toContain('title="新增订单"')
    expect(componentSource).toContain('客户')
    expect(componentSource).toContain('商品明细')
    expect(componentSource).toContain('v-model:value="form.customerId"')
    expect(componentSource).toContain('v-model:value="item.productId"')
    expect(componentSource).toContain('v-model:value="item.quantity"')
    expect(componentSource).toContain('v-model:value="item.unitPrice"')
    expect(componentSource).toContain('v-model:value="item.totalAmount"')
  })

  it('renders order summary with subtotal/discount/finalTotal', () => {
    expect(componentSource).toContain('class="order-summary"')
    expect(componentSource).toContain('class="summary-row"')
    expect(componentSource).toContain('subtotal')
    expect(componentSource).toContain('v-model:value="form.discount"')
    expect(componentSource).toContain('finalTotal')
  })

  it('renders delete popconfirm in action column', () => {
    expect(componentSource).toContain('<a-popconfirm')
    expect(componentSource).toContain('确定删除此订单？')
    expect(componentSource).toContain('@confirm="handleDelete(record.id)"')
  })

  it('renders amount display with red color style', () => {
    expect(componentSource).toContain('.amount-cell')
    expect(componentSource).toContain('color: #f5222d')
  })
})
