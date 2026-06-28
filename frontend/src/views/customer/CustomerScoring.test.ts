import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'

const componentSource = readFileSync(new URL('./CustomerScoring.vue', import.meta.url), 'utf-8')

describe('CustomerScoring layout', () => {
  it('renders the page container with header and actions', () => {
    expect(componentSource).toContain('class="page-container"')
    expect(componentSource).toContain('class="page-header"')
    expect(componentSource).toContain('class="page-header-actions"')
    expect(componentSource).toContain('基于多维度指标的客户价值评估')
  })

  it('renders three stat cards: 高价值, 成长, 待激活', () => {
    expect(componentSource).toContain('class="stat-card stat-high"')
    expect(componentSource).toContain('class="stat-card stat-growing"')
    expect(componentSource).toContain('class="stat-card stat-inactive"')
    expect(componentSource).toContain('segmentCounts.high')
    expect(componentSource).toContain('segmentCounts.growing')
    expect(componentSource).toContain('segmentCounts.inactive')
  })

  it('renders action buttons: 礼品推荐 and 执行评分', () => {
    expect(componentSource).toContain('@click="goToRecommendations"')
    expect(componentSource).toContain('@click="handleRunScoring"')
    expect(componentSource).toContain('礼品推荐')
    expect(componentSource).toContain('执行评分')
  })

  it('renders two-column layout: score ranking table and radar chart', () => {
    expect(componentSource).toContain(':xs="24" :lg="14"')
    expect(componentSource).toContain(':xs="24" :lg="10"')
    expect(componentSource).toContain('class="radar-card"')
    expect(componentSource).toContain('class="radar-chart"')
    expect(componentSource).toContain('class="dim-breakdown"')
  })

  it('renders score ranking table columns', () => {
    expect(componentSource).toContain("key: 'rank'")
    expect(componentSource).toContain("key: 'customer'")
    expect(componentSource).toContain("key: 'score'")
    expect(componentSource).toContain("key: 'segment'")
    expect(componentSource).toContain("key: 'giftLevel'")
  })

  it('renders segment filter radio group', () => {
    expect(componentSource).toContain('v-model:value="segmentFilter"')
    expect(componentSource).toContain('button-style="solid"')
    expect(componentSource).toContain('value="ALL"')
    expect(componentSource).toContain('value="HIGH_VALUE"')
    expect(componentSource).toContain('value="GROWING"')
    expect(componentSource).toContain('value="INACTIVE"')
  })

  it('renders template bodyCell for each column', () => {
    expect(componentSource).toContain("column.key === 'rank'")
    expect(componentSource).toContain("column.key === 'customer'")
    expect(componentSource).toContain("column.key === 'score'")
    expect(componentSource).toContain("column.key === 'segment'")
    expect(componentSource).toContain("column.key === 'giftLevel'")
  })

  it('renders radar chart and dimension breakdown on customer selection', () => {
    expect(componentSource).toContain('v-if="selectedCustomer"')
    expect(componentSource).toContain('class="radar-body"')
    expect(componentSource).toContain('class="dim-row"')
    expect(componentSource).toContain('customer.dimensionScores')
    expect(componentSource).toContain('getDimLabel')
    expect(componentSource).toContain('echarts.init')
    expect(componentSource).toContain("type: 'radar'")
  })

  it('contains segment labels: 高价值/成长/待激活', () => {
    expect(componentSource).toContain("return '高价值'")
    expect(componentSource).toContain("return '成长'")
    expect(componentSource).toContain("return '待激活'")
  })

  it('renders selected row highlight with row-selected class', () => {
    expect(componentSource).toContain("'row-selected'")
    expect(componentSource).toContain(':custom-row')
    expect(componentSource).toContain('handleRowClick')
  })

  it('renders equal-height row layout for side-by-side columns', () => {
    expect(componentSource).toContain('class="equal-row"')
    expect(componentSource).toContain('align-items: stretch;')
  })
})
