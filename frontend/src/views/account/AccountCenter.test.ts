import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'

const componentSource = readFileSync(new URL('./AccountCenter.vue', import.meta.url), 'utf-8')

describe('AccountCenter layout', () => {
  it('formats account timestamps to seconds on the frontend', () => {
    expect(componentSource).toContain("import dayjs from 'dayjs'")
    expect(componentSource).toContain("dayjs(value).format('YYYY-MM-DD HH:mm:ss')")
    expect(componentSource).not.toContain(
      "const formatDateTime = (value?: string) => value || '--'",
    )
  })

  it('defaults to a read-only profile summary with top-right edit entry and equal-height desktop cards', () => {
    expect(componentSource).toContain('const isEditingProfile = ref(false)')
    expect(componentSource).toContain('v-if="!isEditingProfile"')
    expect(componentSource).toContain('v-else')
    expect(componentSource).toContain('编辑资料')
    expect(componentSource).toContain('取消编辑')
    expect(componentSource).toContain('class="profile-card profile-summary-card"')
    expect(componentSource).toContain('class="profile-card-extra"')
    expect(componentSource).not.toContain(
      '<a-space>\n                  <a-button type="primary" @click="startProfileEdit">编辑资料</a-button>',
    )
    expect(componentSource).toContain('class="account-card"')
    expect(componentSource).toContain('class="profile-content-row"')
    expect(componentSource).toContain('.profile-content-row')
    expect(componentSource).toContain('align-items: stretch;')
    expect(componentSource).toContain('.profile-card,')
    expect(componentSource).toContain('.account-card')
  })
})
