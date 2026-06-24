import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'

const componentSource = readFileSync(new URL('./Dashboard.vue', import.meta.url), 'utf-8')

describe('Dashboard layout', () => {
  it('keeps recent customers and low-stock cards at the same desktop height', () => {
    expect(componentSource).toContain('class="row-card dashboard-equal-row"')
    expect(componentSource).toContain('class="dashboard-equal-col"')
    expect(componentSource).toContain('class="h-full dashboard-equal-card"')
    expect(componentSource).toContain('.dashboard-equal-row')
    expect(componentSource).toContain('align-items: stretch;')
    expect(componentSource).toContain('.dashboard-equal-col')
    expect(componentSource).toContain('display: flex;')
    expect(componentSource).toContain('.dashboard-equal-card')
    expect(componentSource).toContain('height: 100%;')
    expect(componentSource).toContain('.dashboard-equal-card :deep(.ant-card-body)')
    expect(componentSource).toContain('.dashboard-equal-card :deep(.ant-list)')
    expect(componentSource).toContain('flex: 1;')
  })

  it('renders stat values inline after the label with larger typography', () => {
    expect(componentSource).toContain('class="stat-primary-line"')
    expect(componentSource).toContain('class="stat-title"')
    expect(componentSource).toContain('class="stat-value-inline"')
    expect(componentSource).toContain('{{ stat.title }}')
    expect(componentSource).toContain('{{ stat.value }}')
    expect(componentSource).toContain('.stat-primary-line')
    expect(componentSource).toContain('gap: 10px;')
    expect(componentSource).toContain('.stat-value-inline')
    expect(componentSource).toContain('font-size: 30px;')
    expect(componentSource).not.toContain('class="text-2xl font-bold mt-2"')
  })

  it('uses lighter labels and aligned number starts for a more dashboard-like rhythm', () => {
    expect(componentSource).toContain('.stat-title')
    expect(componentSource).toContain('color: #9ca3af;')
    expect(componentSource).toContain('min-width: 64px;')
    expect(componentSource).toContain('justify-content: flex-start;')
    expect(componentSource).toContain('white-space: nowrap;')
  })

  it('adds more vertical breathing room between the primary line and the summary line', () => {
    expect(componentSource).toContain('class="stat-secondary-line text-xs"')
    expect(componentSource).toContain('.stat-secondary-line')
    expect(componentSource).toContain('margin-top: 14px;')
    expect(componentSource).not.toContain('class="text-xs mt-2"')
  })

  it('keeps the quick-action icon at the original left position while centering the text block in the remaining area', () => {
    expect(componentSource).toContain('class="quick-actions-grid"')
    expect(componentSource).toContain('class="quick-action-grid-item"')
    expect(componentSource).toContain('class="quick-action-content"')
    expect(componentSource).toContain('class="quick-action-copy"')
    expect(componentSource).toContain('class="quick-action-title"')
    expect(componentSource).toContain('class="quick-action-description"')
    expect(componentSource).toContain('.quick-actions-grid')
    expect(componentSource).toContain('grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));')
    expect(componentSource).toContain('.quick-action-card :deep(.ant-card-body)')
    expect(componentSource).toContain('justify-content: center;')
    expect(componentSource).toContain('.quick-action-content')
    expect(componentSource).toContain('display: grid;')
    expect(componentSource).toContain('grid-template-columns: 36px minmax(0, 1fr);')
    expect(componentSource).toContain('column-gap: 28px;')
    expect(componentSource).toContain('width: 100%;')
    expect(componentSource).toContain('.quick-action-copy')
    expect(componentSource).toContain('justify-self: center;')
    expect(componentSource).toContain('width: max-content;')
    expect(componentSource).toContain('max-width: 100%;')
    expect(componentSource).not.toContain('class="quick-action-spacer"')
    expect(componentSource).not.toContain('width: max-content;\n  max-width: 100%;\n  height: 100%;')
  })
})
