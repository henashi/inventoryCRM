import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'

const componentSource = readFileSync(new URL('./CustomerList.vue', import.meta.url), 'utf-8')

describe('CustomerList compact layout', () => {
  it('renders stat card CSS classes for compact primary line display', () => {
    expect(componentSource).toContain('.stats-card')
    expect(componentSource).toContain('.stats-primary-line')
    expect(componentSource).toContain('.stats-title')
    expect(componentSource).toContain('.stats-value-inline')
    expect(componentSource).toContain('.stats-secondary-line')
    expect(componentSource).toContain('align-items: baseline;')
    expect(componentSource).toContain('font-size: 18px;')
  })

  it('uses denser card body spacing so search, stats and table fit better on one screen', () => {
    expect(componentSource).toContain('.search-card :deep(.ant-card-body)')
    expect(componentSource).toContain('padding: 16px 20px;')
    expect(componentSource).toContain('.table-card :deep(.ant-card-body)')
    expect(componentSource).toContain('padding: 12px 16px 16px;')
    expect(componentSource).toContain('.stats-card')
    expect(componentSource).toContain('min-height: 84px;')
  })

  it('adds dashboard-like accents without increasing card height', () => {
    expect(componentSource).toContain('.stats-card::before')
    expect(componentSource).toContain('height: 3px;')
    expect(componentSource).toContain('.stats-card::after')
    expect(componentSource).toContain('radial-gradient(circle')
    expect(componentSource).toContain('box-shadow: 0 10px 24px -22px')
  })
})
