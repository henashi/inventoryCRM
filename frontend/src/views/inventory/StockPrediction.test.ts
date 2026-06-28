import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'

const componentSource = readFileSync(new URL('./StockPrediction.vue', import.meta.url), 'utf-8')

describe('StockPrediction layout', () => {
  it('renders the prediction page with header and subtitle', () => {
    expect(componentSource).toContain('class="prediction-page"')
    expect(componentSource).toContain('class="page-header"')
    expect(componentSource).toContain('class="page-header-main"')
    expect(componentSource).toContain('class="page-header-actions"')
    expect(componentSource).toContain('OLS 线性回归模型')
  })

  it('renders four stat cards in a row: danger, warning, restock, normal', () => {
    expect(componentSource).toContain('class="stat-card stat-danger"')
    expect(componentSource).toContain('class="stat-card stat-warning"')
    expect(componentSource).toContain('class="stat-card stat-restock"')
    expect(componentSource).toContain('class="stat-card stat-normal"')
    expect(componentSource).toContain('summary.dangerCount')
    expect(componentSource).toContain('summary.warningCount')
    expect(componentSource).toContain('summary.totalSuggestedRestockQty')
    expect(componentSource).toContain('summary.normalCount')
  })

  it('renders table toolbar with search and filter', () => {
    expect(componentSource).toContain('class="table-toolbar"')
    expect(componentSource).toContain('v-model:value="searchKeyword"')
    expect(componentSource).toContain('v-model:value="alertFilter"')
    expect(componentSource).toContain('@click="handleSearch"')
  })

  it('renders table column definitions for prediction data', () => {
    expect(componentSource).toContain("key: 'product'")
    expect(componentSource).toContain("key: 'stock'")
    expect(componentSource).toContain("key: 'avgOut'")
    expect(componentSource).toContain("key: 'daysToEmpty'")
    expect(componentSource).toContain("key: 'modelInfo'")
    expect(componentSource).toContain("key: 'alertLevel'")
    expect(componentSource).toContain("key: 'suggestRestock'")
    expect(componentSource).toContain("key: 'trend'")
    expect(componentSource).toContain("key: 'action'")
  })

  it('renders template bodyCell for each column', () => {
    expect(componentSource).toContain("column.key === 'product'")
    expect(componentSource).toContain("column.key === 'stock'")
    expect(componentSource).toContain("column.key === 'avgOut'")
    expect(componentSource).toContain("column.key === 'daysToEmpty'")
    expect(componentSource).toContain("column.key === 'modelInfo'")
    expect(componentSource).toContain("column.key === 'alertLevel'")
    expect(componentSource).toContain("column.key === 'suggestRestock'")
    expect(componentSource).toContain("column.key === 'trend'")
    expect(componentSource).toContain("column.key === 'action'")
  })

  it('renders expanded row trend chart template', () => {
    expect(componentSource).toContain('#expandedRowRender')
    expect(componentSource).toContain('class="trend-row"')
    expect(componentSource).toContain('class="trend-header"')
    expect(componentSource).toContain('class="trend-chart"')
    expect(componentSource).toContain("'trend-chart-' + record.productId")
  })

  it('renders the run prediction button with sync icon', () => {
    expect(componentSource).toContain('@click="handleRunPrediction"')
    expect(componentSource).toContain(':loading="runningPrediction"')
    expect(componentSource).toContain('执行全量预测')
    expect(componentSource).toContain('<SyncOutlined />')
  })

  it('uses alert level helpers: DANGER/WARNING/NORMAL', () => {
    expect(componentSource).toContain("return '高危'")
    expect(componentSource).toContain("return '预警'")
    expect(componentSource).toContain("return '正常'")
  })

  it('contains ECharts setup for trend visualization', () => {
    expect(componentSource).toContain("import * as echarts from 'echarts'")
    expect(componentSource).toContain('echarts.init')
    expect(componentSource).toContain("type: 'bar'")
    expect(componentSource).toContain('barMaxWidth: 16')
  })
})
