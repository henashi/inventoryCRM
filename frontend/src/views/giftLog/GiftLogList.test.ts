import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'

const componentSource = readFileSync(new URL('./GiftLogList.vue', import.meta.url), 'utf-8')

describe('GiftLogList customer options loading', () => {
  it('loads customer filter options without mutating shared customer list pagination state', () => {
    expect(componentSource).toContain("customerApi.getCustomers({ page: 0, size: 100")
    expect(componentSource).not.toContain("customerStore.loadCustomers({ page: 0, size: 100 })")
  })
})
