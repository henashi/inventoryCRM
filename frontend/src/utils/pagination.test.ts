import { describe, expect, it } from 'vitest'
import { buildServerPageParams, toUiPage } from './pagination'

describe('pagination helpers', () => {
  it('converts backend page number to a 1-based UI page', () => {
    expect(toUiPage(0)).toBe(1)
    expect(toUiPage(4)).toBe(5)
  })

  it('maps table pagination current/pageSize into backend page/size params', () => {
    expect(buildServerPageParams({ current: 3, pageSize: 20 })).toEqual({
      page: 2,
      size: 20,
    })
  })

  it('falls back to the first page and default page size when table pagination is missing', () => {
    expect(buildServerPageParams(undefined)).toEqual({
      page: 0,
      size: 10,
    })
  })
})
