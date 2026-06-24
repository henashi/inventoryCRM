import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'

const componentSource = readFileSync(new URL('./ProductList.vue', import.meta.url), 'utf-8')

describe('ProductList dashboard-style compact layout', () => {
  it('renders compact summary cards with inline title and value', () => {
    expect(componentSource).toContain('class="search-form"')
    expect(componentSource).toContain('class="summary-primary-line"')
    expect(componentSource).toContain('class="summary-title"')
    expect(componentSource).toContain('class="summary-value-inline"')
    expect(componentSource).toContain('class="summary-secondary-line"')
    expect(componentSource).toContain('size="middle"')
  })

  it('uses tighter spacing across search, summary and table sections', () => {
    expect(componentSource).toContain('.search-card :deep(.ant-card-body)')
    expect(componentSource).toContain('padding: 16px 20px;')
    expect(componentSource).toContain('.table-card :deep(.ant-card-body)')
    expect(componentSource).toContain('padding: 12px 16px 16px;')
    expect(componentSource).toContain('.summary-card')
    expect(componentSource).toContain('min-height: 84px;')
  })

  it('adds dashboard-like accents to summary cards', () => {
    expect(componentSource).toContain('.summary-card::before')
    expect(componentSource).toContain('height: 3px;')
    expect(componentSource).toContain('.summary-card::after')
    expect(componentSource).toContain('radial-gradient(circle')
    expect(componentSource).toContain('box-shadow: 0 10px 24px -22px')
  })

  it('keeps summary columns and cards at full row width instead of shrinking to content', () => {
    expect(componentSource).toContain('.summary-row :deep(.ant-col)')
    expect(componentSource).toContain('display: block;')
    expect(componentSource).toContain('.summary-row .summary-card')
    expect(componentSource).toContain('width: 100%;')
  })
})
