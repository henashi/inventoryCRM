import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'

const componentSource = readFileSync(new URL('./CustomerDetail.vue', import.meta.url), 'utf-8')

describe('CustomerDetail layout', () => {
  it('renders the customer detail page container', () => {
    expect(componentSource).toContain('class="customer-detail-page"')
    expect(componentSource).toContain('class="page-header"')
  })

  it('renders action buttons: 返回, 查看礼品记录, 发放礼品', () => {
    expect(componentSource).toContain('@click="goBack"')
    expect(componentSource).toContain('@click="goToGiftLogs"')
    expect(componentSource).toContain('@click="issueGift"')
    expect(componentSource).toContain('查看礼品记录')
    expect(componentSource).toContain('发放礼品')
  })

  it('renders loading, error and not-found states', () => {
    expect(componentSource).toContain(':spinning="pageLoading"')
    expect(componentSource).toContain('status="error"')
    expect(componentSource).toContain('title="客户详情加载失败"')
    expect(componentSource).toContain('status="404"')
    expect(componentSource).toContain('title="未找到客户"')
  })

  it('renders summary hero section with avatar, title, tags and stats', () => {
    expect(componentSource).toContain('class="summary-hero"')
    expect(componentSource).toContain('class="hero-main"')
    expect(componentSource).toContain('class="hero-title-row"')
    expect(componentSource).toContain('class="hero-meta"')
    expect(componentSource).toContain('class="hero-stats"')
    expect(componentSource).toContain('class="hero-stat-item"')
    expect(componentSource).toContain('getCustomerColor')
  })

  it('renders profile info card with descriptions in 2 columns', () => {
    expect(componentSource).toContain('title="档案信息"')
    expect(componentSource).toContain(':column="2" bordered')
    expect(componentSource).toContain('label="姓名"')
    expect(componentSource).toContain('label="手机号"')
    expect(componentSource).toContain('label="邮箱"')
    expect(componentSource).toContain('label="礼品等级"')
    expect(componentSource).toContain('label="推荐人"')
    expect(componentSource).toContain('label="地址"')
  })

  it('renders relationship summary card with customer status and gift level', () => {
    expect(componentSource).toContain('title="关系与摘要"')
    expect(componentSource).toContain('class="summary-list"')
    expect(componentSource).toContain('class="summary-item"')
    expect(componentSource).toContain('客户状态')
    expect(componentSource).toContain('礼品等级')
    expect(componentSource).toContain('推荐来源')
  })

  it('renders recent gift logs card with table loading and empty states', () => {
    expect(componentSource).toContain('title="最近礼品记录"')
    expect(componentSource).toContain(':spinning="giftLogLoading"')
    expect(componentSource).toContain('class="gift-log-list"')
    expect(componentSource).toContain('class="gift-log-item"')
    expect(componentSource).toContain('giftLog.giftName')
    expect(componentSource).toContain('giftLog.quantity')
    expect(componentSource).toContain('giftLog.operator')
  })

  it('renders gift log status helpers: PENDING/CANCELLED/ISSUED', () => {
    expect(componentSource).toContain("'PENDING'")
    expect(componentSource).toContain("'CANCELLED'")
    expect(componentSource).toContain("'ISSUED'")
  })

  it('applies dark mode overrides for hero, gift-log and summary sections', () => {
    expect(componentSource).toContain("[data-theme='dark'] .hero-stat-item")
    expect(componentSource).toContain("[data-theme='dark'] .gift-log-item")
    expect(componentSource).toContain("[data-theme='dark'] .summary-label")
  })

  it('renders responsive layout breakpoints for tablet and mobile', () => {
    expect(componentSource).toContain('@media (max-width: 992px)')
    expect(componentSource).toContain('@media (max-width: 768px)')
  })
})
