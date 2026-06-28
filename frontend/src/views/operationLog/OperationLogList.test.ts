import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'

const componentSource = readFileSync(new URL('./OperationLogList.vue', import.meta.url), 'utf-8')

describe('OperationLogList layout', () => {
  it('renders the page container with header and title', () => {
    expect(componentSource).toContain('class="page-container"')
    expect(componentSource).toContain('class="page-header"')
    expect(componentSource).toContain('class="page-title"')
    expect(componentSource).toContain('系统日志')
  })

  it('renders four stat cards: 日志总数, 成功, 失败, 涉及模块', () => {
    expect(componentSource).toContain('class="stat-card stat-card-total"')
    expect(componentSource).toContain('class="stat-card stat-card-success"')
    expect(componentSource).toContain('class="stat-card stat-card-fail"')
    expect(componentSource).toContain('class="stat-card stat-card-module"')
    expect(componentSource).toContain('pagination.total')
    expect(componentSource).toContain('stats.successCount')
    expect(componentSource).toContain('stats.failCount')
    expect(componentSource).toContain('stats.moduleCount')
  })

  it('renders search card with module, operator, status and date range filters', () => {
    expect(componentSource).toContain('class="search-card"')
    expect(componentSource).toContain('searchForm.module')
    expect(componentSource).toContain('searchForm.operator')
    expect(componentSource).toContain('searchForm.status')
    expect(componentSource).toContain('searchForm.dateRange')
    expect(componentSource).toContain('@finish="handleSearch"')
  })

  it('renders search and reset buttons', () => {
    expect(componentSource).toContain('class="search-actions"')
    expect(componentSource).toContain('html-type="submit"')
    expect(componentSource).toContain('@click="handleReset"')
  })

  it('renders table columns: operationTime, module, operationType, description, operator, executionTime, status, actions', () => {
    expect(componentSource).toContain("dataIndex: 'operationTime'")
    expect(componentSource).toContain("dataIndex: 'module'")
    expect(componentSource).toContain("dataIndex: 'operationType'")
    expect(componentSource).toContain("dataIndex: 'description'")
    expect(componentSource).toContain("dataIndex: 'operator'")
    expect(componentSource).toContain("dataIndex: 'executionTime'")
    expect(componentSource).toContain("dataIndex: 'status'")
    expect(componentSource).toContain("dataIndex: 'actions'")
  })

  it('renders bodyCell templates for all custom columns', () => {
    expect(componentSource).toContain("column.dataIndex === 'operationTime'")
    expect(componentSource).toContain("column.dataIndex === 'module'")
    expect(componentSource).toContain("column.dataIndex === 'operationType'")
    expect(componentSource).toContain("column.dataIndex === 'description'")
    expect(componentSource).toContain("column.dataIndex === 'operator'")
    expect(componentSource).toContain("column.dataIndex === 'executionTime'")
    expect(componentSource).toContain("column.dataIndex === 'status'")
    expect(componentSource).toContain("column.dataIndex === 'actions'")
  })

  it('renders detail drawer with operation log details', () => {
    expect(componentSource).toContain('v-model:open="detailVisible"')
    expect(componentSource).toContain('title="操作日志详情"')
    expect(componentSource).toContain('class="detail-content"')
    expect(componentSource).toContain('currentLog.operationTime')
    expect(componentSource).toContain('currentLog.requestUrl')
    expect(componentSource).toContain('currentLog.description')
    expect(componentSource).toContain('currentLog.errorMessage')
  })

  it('contains module color mapping', () => {
    expect(componentSource).toContain("客户管理: 'blue'")
    expect(componentSource).toContain("商品管理: 'cyan'")
    expect(componentSource).toContain("库存管理: 'green'")
    expect(componentSource).toContain("系统管理: 'geekblue'")
  })

  it('contains operation type labels', () => {
    expect(componentSource).toContain("CREATE: '新增'")
    expect(componentSource).toContain("DELETE: '删除'")
    expect(componentSource).toContain("CONTENT_UPDATE: '修改内容'")
    expect(componentSource).toContain("STATUS_UPDATE: '修改状态'")
  })
})
