import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'

const componentSource = readFileSync(new URL('./Login.vue', import.meta.url), 'utf-8')

describe('Login layout', () => {
  it('renders the login container with card layout', () => {
    expect(componentSource).toContain('class="login-container"')
    expect(componentSource).toContain('class="login-card"')
    expect(componentSource).toContain('class="login-header"')
  })

  it('renders logo, title and system name', () => {
    expect(componentSource).toContain('class="login-logo"')
    expect(componentSource).toContain('库存CRM系统')
    expect(componentSource).toContain('<inbox-outlined />')
  })

  it('renders registration entry below form', () => {
    expect(componentSource).toContain('class="register-entry"')
    expect(componentSource).toContain('立即注册')
    expect(componentSource).toContain('没有账号？')
  })

  it('renders login form with username and password fields', () => {
    expect(componentSource).toContain('id="login-form"')
    expect(componentSource).toContain('id="login-username"')
    expect(componentSource).toContain('id="login-password"')
    expect(componentSource).toContain('id="login-remember"')
    expect(componentSource).toContain('id="login-card"')
    expect(componentSource).toContain('id="login-title"')
    expect(componentSource).toContain('<user-outlined />')
    expect(componentSource).toContain('<lock-outlined />')
  })

  it('renders validation rules for username and password', () => {
    expect(componentSource).toContain('请输入用户名')
    expect(componentSource).toContain('请输入密码')
    expect(componentSource).toContain('用户名长度为3-20个字符')
    expect(componentSource).toContain('密码长度为6-20个字符')
  })

  it('renders login button and error message display', () => {
    expect(componentSource).toContain('v-if="errorMessage"')
    expect(componentSource).toContain('class="error-message"')
    expect(componentSource).toContain('html-type="submit"')
    expect(componentSource).toContain(':loading="loading"')
  })

  it('renders footer with copyright and support info', () => {
    expect(componentSource).toContain('class="login-footer"')
    expect(componentSource).toContain('© 2026 库存CRM系统')
    expect(componentSource).toContain('技术支持')
  })

  it('uses gradient background for login page', () => {
    expect(componentSource).toContain(
      'background: linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
    )
    expect(componentSource).toContain('box-shadow: 0 20px 40px')
  })

  it('applies dark theme overrides', () => {
    expect(componentSource).toContain("[data-theme='dark'] .login-card")
    expect(componentSource).toContain("[data-theme='dark'] .login-title")
    expect(componentSource).toContain("[data-theme='dark'] .login-footer")
    expect(componentSource).toContain("[data-theme='dark'] .error-message")
  })

  it('renders responsive mobile breakpoint', () => {
    expect(componentSource).toContain('@media (max-width: 480px)')
  })
})
