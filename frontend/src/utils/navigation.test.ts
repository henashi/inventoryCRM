import { describe, expect, it, vi } from 'vitest'
import { logoutAndNavigateToLogin } from './navigation'

describe('navigation helpers', () => {
  it('waits for logout to finish before navigating to the login page', async () => {
    const calls: string[] = []
    const logout = vi.fn(async () => {
      calls.push('logout:start')
      await Promise.resolve()
      calls.push('logout:end')
    })
    const push = vi.fn(async (path: string) => {
      calls.push(`push:${path}`)
    })

    await logoutAndNavigateToLogin({ logout }, { push })

    expect(logout).toHaveBeenCalledTimes(1)
    expect(push).toHaveBeenCalledWith('/login')
    expect(calls).toEqual(['logout:start', 'logout:end', 'push:/login'])
  })
})
