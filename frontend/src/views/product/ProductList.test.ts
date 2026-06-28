import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'

const componentSource = readFileSync(new URL('./ProductList.vue', import.meta.url), 'utf-8')

describe('ProductList dashboard-style compact layout', () => {
  it('renders compact search, table cards and toolbar structure', () => {
    expect(componentSource).toContain('class="search-form"')
    expect(componentSource).toContain('class="search-card"')
    expect(componentSource).toContain('class="table-card"')
    expect(componentSource).toContain('class="toolbar-row"')
    expect(componentSource).toContain('size="middle"')
  })

  it('uses tighter spacing across search and table sections', () => {
    expect(componentSource).toContain('.search-card :deep(.ant-card-body)')
    expect(componentSource).toContain('padding: 16px 20px;')
    expect(componentSource).toContain('.table-card :deep(.ant-card-body)')
    expect(componentSource).toContain('padding: 12px 16px 16px;')
    expect(componentSource).toContain('.summary-row,')
    expect(componentSource).toContain('margin-bottom: 16px;')
  })

  it('adds dashboard-like accents to summary cards in CSS', () => {
    expect(componentSource).toContain('.summary-card::before')
    expect(componentSource).toContain('height: 3px;')
    expect(componentSource).toContain('.summary-card::after')
    expect(componentSource).toContain('radial-gradient(circle')
    expect(componentSource).toContain('box-shadow: 0 10px 24px -22px')
  })

  it('keeps summary columns at full row width instead of shrinking to content', () => {
    expect(componentSource).toContain('.summary-row :deep(.ant-col)')
    expect(componentSource).toContain('display: block;')
    expect(componentSource).toContain('.summary-row .summary-card')
    expect(componentSource).toContain('width: 100%;')
  })
})
