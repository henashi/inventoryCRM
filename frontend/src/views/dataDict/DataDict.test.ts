import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'

const componentSource = readFileSync(new URL('./DataDict.vue', import.meta.url), 'utf-8')

describe('DataDict layout', () => {
  it('renders the data dictionary page container', () => {
    expect(componentSource).toContain('class="dataDict-page"')
    expect(componentSource).toContain('class="page-header"')
    expect(componentSource).toContain('class="page-actions"')
    expect(componentSource).toContain('class="table-card"')
  })

  it('renders action buttons: 新增配置 and 返回', () => {
    expect(componentSource).toContain('@click="showAddModal"')
    expect(componentSource).toContain('@click="handleBack"')
    expect(componentSource).toContain('新增配置')
    expect(componentSource).toContain('<plus-outlined />')
  })

  it('renders table columns: groupName, groupCode, paramName, paramCode, paramValue, status, description, action', () => {
    expect(componentSource).toContain("dataIndex: 'groupName'")
    expect(componentSource).toContain("dataIndex: 'groupCode'")
    expect(componentSource).toContain("dataIndex: 'paramName'")
    expect(componentSource).toContain("dataIndex: 'paramCode'")
    expect(componentSource).toContain("dataIndex: 'paramValue'")
    expect(componentSource).toContain("dataIndex: 'status'")
    expect(componentSource).toContain("dataIndex: 'description'")
    expect(componentSource).toContain("key: 'action'")
  })

  it('renders action column with edit, activate/deactivate and delete buttons', () => {
    expect(componentSource).toContain('@click="handleDataDictEdit(record)"')
    expect(componentSource).toContain('@click="handleActiveOrDisable(record, true)"')
    expect(componentSource).toContain('@click="handleActiveOrDisable(record, false)"')
    expect(componentSource).toContain('@click="handleDelete(record)"')
  })

  it('renders add/edit modal with form fields', () => {
    expect(componentSource).toContain('v-model:visible="modalVisible"')
    expect(componentSource).toContain(':title="modalTitle"')
    expect(componentSource).toContain('label="分组名称" name="groupName"')
    expect(componentSource).toContain('label="分组编码" name="groupCode"')
    expect(componentSource).toContain('label="配置名称" name="paramName"')
    expect(componentSource).toContain('label="配置编码" name="paramCode"')
    expect(componentSource).toContain('label="配置值" name="paramValue"')
    expect(componentSource).toContain('label="描述" name="description"')
  })

  it('contains status text mapping: DICT_STATUS_ACTIVE/DICT_STATUS_PAUSED', () => {
    expect(componentSource).toContain("return '生效'")
    expect(componentSource).toContain("return '失效'")
  })

  it('contains responsive mobile breakpoint', () => {
    expect(componentSource).toContain('@media (max-width: 768px)')
  })
})
