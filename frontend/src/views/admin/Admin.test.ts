import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'

const componentSource = readFileSync(new URL('./Admin.vue', import.meta.url), 'utf-8')

describe('Admin overview layout', () => {
  it('renders the admin page container with header', () => {
    expect(componentSource).toContain('class="admin-page"')
    expect(componentSource).toContain('class="page-header"')
    expect(componentSource).toContain('class="page-subtitle"')
    expect(componentSource).toContain('聚合系统概览、模块入口与运行提醒')
  })

  it('renders four summary cards: 客户总量, 商品概况, 库存操作, 系统配置', () => {
    expect(componentSource).toContain("key: 'customers'")
    expect(componentSource).toContain("key: 'products'")
    expect(componentSource).toContain("key: 'inventory'")
    expect(componentSource).toContain("key: 'configs'")
    expect(componentSource).toContain('customerStats.value.totalCustomers')
    expect(componentSource).toContain('productStats.value.totalProducts')
    expect(componentSource).toContain('inventoryStats.value.totalOperations')
    expect(componentSource).toContain('configTotal.value')
  })

  it('renders role & permission section', () => {
    expect(componentSource).toContain('title="角色与权限说明"')
    expect(componentSource).toContain('class="role-header"')
    expect(componentSource).toContain('class="permission-list"')
    expect(componentSource).toContain('class="permission-item"')
    expect(componentSource).toContain('ADMIN')
    expect(componentSource).toContain('MANAGER')
    expect(componentSource).toContain('USER')
  })

  it('renders inventory log alerts section', () => {
    expect(componentSource).toContain('title="库存与日志提醒"')
    expect(componentSource).toContain('class="runtime-list"')
    expect(componentSource).toContain('inventoryStats.totalOperations')
    expect(componentSource).toContain('lowStockProducts')
  })

  it('renders config summary section', () => {
    expect(componentSource).toContain('title="配置摘要"')
    expect(componentSource).toContain('configTotal')
    expect(componentSource).toContain('recentConfigs')
    expect(componentSource).toContain('authStore.userName')
  })

  it('renders operation log preview section', () => {
    expect(componentSource).toContain('title="操作日志预览"')
    expect(componentSource).toContain('operationLogSummary.total')
    expect(componentSource).toContain('operationLogSummary.successCount')
    expect(componentSource).toContain('recentOperationLogs')
  })

  it('renders account management section', () => {
    expect(componentSource).toContain('title="账号管理预留"')
    expect(componentSource).toContain('accountUser?.username')
    expect(componentSource).toContain('currentRoleText')
    expect(componentSource).toContain('accountStatusText')
  })

  it('renders config category statistics section with grid', () => {
    expect(componentSource).toContain('title="配置分类统计"')
    expect(componentSource).toContain('class="category-grid"')
    expect(componentSource).toContain('class="category-item"')
    expect(componentSource).toContain('configCategoryStats')
    expect(componentSource).toContain('category-count')
  })

  it('contains shortcut navigation items', () => {
    expect(componentSource).toContain("title: '客户管理'")
    expect(componentSource).toContain("title: '库存总览'")
    expect(componentSource).toContain("title: '库存日志'")
    expect(componentSource).toContain("title: '礼品发放'")
    expect(componentSource).toContain("title: '配置管理'")
    expect(componentSource).toContain("title: '商品管理'")
  })

  it('renders responsive breakpoints for grid layout', () => {
    expect(componentSource).toContain('@media (max-width: 1200px)')
    expect(componentSource).toContain('@media (max-width: 992px)')
    expect(componentSource).toContain('@media (max-width: 768px)')
  })
})
