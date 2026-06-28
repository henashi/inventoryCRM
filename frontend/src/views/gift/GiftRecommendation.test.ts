import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'

const componentSource = readFileSync(new URL('./GiftRecommendation.vue', import.meta.url), 'utf-8')

describe('GiftRecommendation layout', () => {
  it('renders the recommendation page with header', () => {
    expect(componentSource).toContain('class="recommendation-page"')
    expect(componentSource).toContain('class="page-header"')
    expect(componentSource).toContain('基于客户评分智能匹配最佳礼品')
  })

  it('renders birthday alert banner', () => {
    expect(componentSource).toContain('v-if="birthdayCustomers.length > 0"')
    expect(componentSource).toContain('一键发放生日礼品')
    expect(componentSource).toContain('@click="handleBatchBirthday"')
  })

  it('renders two-column layout: customer select table on left, recommendations on right', () => {
    expect(componentSource).toContain(':lg="10"')
    expect(componentSource).toContain(':lg="14"')
    expect(componentSource).toContain('title="选择客户"')
    expect(componentSource).toContain('>推荐结果<')
  })

  it('renders customer search input', () => {
    expect(componentSource).toContain('v-model:value="searchKeyword"')
    expect(componentSource).toContain('@search="handleSearch"')
    expect(componentSource).toContain('搜索客户姓名 / 手机号')
  })

  it('renders customer table columns: name, score, segment, birthday', () => {
    expect(componentSource).toContain("key: 'name'")
    expect(componentSource).toContain("key: 'score'")
    expect(componentSource).toContain("key: 'segment'")
    expect(componentSource).toContain("key: 'birthday'")
  })

  it('renders selected row highlight', () => {
    expect(componentSource).toContain('selected-row')
    expect(componentSource).toContain(':custom-row')
    expect(componentSource).toContain('handleSelectCustomer')
  })

  it('renders recommendation cards with rank, name, match score, reason and action', () => {
    expect(componentSource).toContain('class="recommendation-list"')
    expect(componentSource).toContain('class="recommendation-card"')
    expect(componentSource).toContain('class="rec-rank"')
    expect(componentSource).toContain('class="rec-name"')
    expect(componentSource).toContain('class="rec-match"')
    expect(componentSource).toContain('class="rec-reason"')
    expect(componentSource).toContain('class="rec-actions"')
    expect(componentSource).toContain('@click="handleIssueGift(rec)"')
  })

  it('renders empty states for no customer selected and no recommendations', () => {
    expect(componentSource).toContain('v-if="!selectedCustomerInfo"')
    expect(componentSource).toContain('recommendations.length === 0')
    expect(componentSource).toContain('请从左侧选择一个客户')
  })

  it('renders top-1 card highlight styling', () => {
    expect(componentSource).toContain("'top-1': index === 0")
    expect(componentSource).toContain('background: linear-gradient(135deg, #f6ffed')
  })

  it('contains segment labels: 高价值/成长/待激活', () => {
    expect(componentSource).toContain("return '高价值'")
    expect(componentSource).toContain("return '成长'")
    expect(componentSource).toContain("return '待激活'")
  })

  it('applies dark mode overrides', () => {
    expect(componentSource).toContain("[data-theme='dark'] .recommendation-card")
    expect(componentSource).toContain("[data-theme='dark'] .rec-name")
    expect(componentSource).toContain("[data-theme='dark'] .rec-reason")
  })
})
