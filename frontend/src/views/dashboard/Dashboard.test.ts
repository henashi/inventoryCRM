import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'

const componentSource = readFileSync(new URL('./Dashboard.vue', import.meta.url), 'utf-8')

describe('Dashboard layout', () => {
  it('renders the dashboard container with header and description', () => {
    expect(componentSource).toContain('class="dashboard"')
    expect(componentSource).toContain('class="dashboard-header"')
    expect(componentSource).toContain('class="dashboard-desc"')
    expect(componentSource).toContain('库存管理系统概览')
  })

  it('renders summary cards with label, value and helper text', () => {
    expect(componentSource).toContain('class="summary-card clickable"')
    expect(componentSource).toContain('class="summary-card-label"')
    expect(componentSource).toContain('class="summary-card-value"')
    expect(componentSource).toContain('class="summary-card-helper"')
    expect(componentSource).toContain('v-for="card in summaryCards"')
    expect(componentSource).toContain('card.label')
    expect(componentSource).toContain('card.value')
    expect(componentSource).toContain('card.helper')
    expect(componentSource).toContain('@click="router.push(card.path)"')
    expect(componentSource).toContain('min-height: 140px;')
  })

  it('renders two-column chart layout with trend and pie charts', () => {
    expect(componentSource).toContain(':xs="24" :lg="16"')
    expect(componentSource).toContain(':xs="24" :lg="8"')
    expect(componentSource).toContain('title="出入库趋势"')
    expect(componentSource).toContain('title="库存占比"')
    expect(componentSource).toContain('class="chart-wrap"')
    expect(componentSource).toContain('ref="trendChartRef"')
    expect(componentSource).toContain('ref="pieChartRef"')
  })

  it('renders trend chart with 7-day / 30-day toggle', () => {
    expect(componentSource).toContain('v-model:value="chartDays"')
    expect(componentSource).toContain('value="7"')
    expect(componentSource).toContain('value="30"')
    expect(componentSource).toContain('watch(chartDays')
  })

  it('renders alerts list section with low-stock items', () => {
    expect(componentSource).toContain('title="待处理"')
    expect(componentSource).toContain('v-if="alerts.length === 0"')
    expect(componentSource).toContain(':data-source="alerts"')
    expect(componentSource).toContain('class="alert-item"')
    expect(componentSource).toContain("item.level === 'DANGER'")
  })

  it('renders recent orders section', () => {
    expect(componentSource).toContain('title="最近订单"')
    expect(componentSource).toContain('v-if="recentOrders.length === 0"')
    expect(componentSource).toContain(':data-source="recentOrders"')
    expect(componentSource).toContain('item.customerName')
    expect(componentSource).toContain('item.finalAmount')
  })

  it('renders ECharts setup for trend and pie charts', () => {
    expect(componentSource).toContain("import * as echarts from 'echarts'")
    expect(componentSource).toContain("type: 'line'")
    expect(componentSource).toContain("type: 'pie'")
    expect(componentSource).toContain("radius: ['40%', '65%']")
    expect(componentSource).toContain('echarts.init')
  })

  it('applies dark mode color handling', () => {
    expect(componentSource).toContain('isDarkMode')
    expect(componentSource).toContain('getChartColors')
    expect(componentSource).toContain('getChartTextColor')
    expect(componentSource).toContain('rgba(74,222,128,0.08)')
  })

  it('renders responsive margins and clickable card transitions', () => {
    expect(componentSource).toContain('transform 0.2s ease')
    expect(componentSource).toContain('box-shadow 0.2s ease')
    expect(componentSource).toContain('translateY(-2px)')
    expect(componentSource).toContain('.summary-card.clickable:hover')
  })
})
