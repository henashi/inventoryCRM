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

  it('renders toggleable login/register forms', () => {
    expect(componentSource).toContain('isRegisterMode')
    expect(componentSource).toContain('立即注册')
    expect(componentSource).toContain('已有账号？')
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
  })

  it('renders login button and error message display', () => {
    expect(componentSource).toContain('v-if="errorMessage"')
    expect(componentSource).toContain('class="error-message"')
    expect(componentSource).toContain('html-type="submit"')
    expect(componentSource).toContain(':loading="loading"')
  })

  it('renders register form fields', () => {
    expect(componentSource).toContain('确认密码')
    expect(componentSource).toContain('真实姓名（可选）')
    expect(componentSource).toContain('<mail-outlined />')
  })

  it('renders footer with copyright and support info', () => {
    expect(componentSource).toContain('class="login-footer"')
    expect(componentSource).toContain('© 2026 库存CRM系统')
    expect(componentSource).toContain('技术支持')
  })

  it('uses modern flat design', () => {
    expect(componentSource).toContain('border-radius: 8px')
    expect(componentSource).toContain('box-shadow: 0 2px 12px')
  })

  it('uses CSS variables for theme support', () => {
    expect(componentSource).toContain('var(--bg-color')
    expect(componentSource).toContain('var(--card-bg')
    expect(componentSource).toContain('var(--text-color')
    expect(componentSource).toContain('var(--text-secondary')
  })

  it('uses card width 400px for desktop', () => {
    expect(componentSource).toContain('width: 400px')
    expect(componentSource).toContain('min-height: 100vh')
  })
})
