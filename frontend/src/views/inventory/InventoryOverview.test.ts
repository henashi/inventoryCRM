import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const componentSource = readFileSync(resolve(__dirname, 'InventoryOverview.vue'), 'utf-8')

describe('InventoryOverview layout', () => {
  it('keeps all five header actions in one responsive group with 商品入库 directly before 返回仪表盘', () => {
    expect(componentSource).toContain('class="page-header-main"')
    expect(componentSource).toContain('class="page-header-actions"')
    expect(componentSource).not.toContain('page-header-actions-group')
    expect(componentSource).not.toContain('page-header-actions-priority')
    expect(componentSource).toContain('class="header-action-back"')
    expect(componentSource).toContain('class="header-action-primary"')
    expect(componentSource).toContain('class="search-form"')
    expect(componentSource).toContain('class="search-field search-field-stock-range"')
    expect(componentSource).toContain('class="search-actions"')

    const lowAlertIndex = componentSource.indexOf('低库存预警')
    const exportIndex = componentSource.indexOf('导出快照')
    const logsIndex = componentSource.indexOf('库存日志')
    const stockInIndex = componentSource.indexOf('商品入库')
    const backIndex = componentSource.indexOf('返回仪表盘')

    expect(lowAlertIndex).toBeLessThan(exportIndex)
    expect(exportIndex).toBeLessThan(logsIndex)
    expect(logsIndex).toBeLessThan(stockInIndex)
    expect(stockInIndex).toBeLessThan(backIndex)
    expect(componentSource).toContain('@media (max-width: 1200px)')
    expect(componentSource).toContain('@media (max-width: 768px)')
  })
})
