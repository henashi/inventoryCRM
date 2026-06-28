import { test, expect } from '@playwright/test'

test.describe('Inventory CRM', () => {

  test('登录页加载正常', async ({ page }) => {
    await page.goto('/login')
    await expect(page.locator('h1')).toContainText('登录')
    await expect(page.locator('input[type="text"]')).toBeVisible()
  })

  test('登录失败时显示错误提示', async ({ page }) => {
    await page.goto('/login')
    await page.fill('input[type="text"]', 'admin')
    await page.fill('input[type="password"]', 'wrong_password')
    await page.click('button[type="submit"]')
    // 错误提示应出现
    await expect(page.locator('.ant-message')).toBeVisible({ timeout: 5000 })
  })

  test('登录成功后可访问仪表盘', async ({ page }) => {
    await page.goto('/login')
    await page.fill('input[type="text"]', 'admin')
    await page.fill('input[type="password"]', 'admin123')
    await page.click('button[type="submit"]')

    // 应重定向到仪表盘
    await page.waitForURL('/dashboard', { timeout: 5000 })
    await expect(page.locator('.dashboard')).toBeVisible({ timeout: 5000 })
  })

  test('未登录时访问需要认证的页面应重定向到登录页', async ({ page }) => {
    await page.goto('/products')
    await page.waitForURL('/login', { timeout: 5000 })
  })
})
