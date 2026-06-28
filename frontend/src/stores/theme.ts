import { ref, watch } from 'vue'
import { defineStore } from 'pinia'

const THEME_KEY = 'theme-mode'

export const useThemeStore = defineStore('theme', () => {
  // 从 localStorage 读取，若无则跟随系统偏好
  const isDark = ref<boolean>(
    (() => {
      const stored = localStorage.getItem(THEME_KEY)
      if (stored !== null) return stored === 'dark'
      return window.matchMedia('(prefers-color-scheme: dark)').matches
    })(),
  )

  // 同步到 localStorage + <html> 属性
  function applyTheme(dark: boolean) {
    localStorage.setItem(THEME_KEY, dark ? 'dark' : 'light')
    document.documentElement.setAttribute('data-theme', dark ? 'dark' : 'light')
  }

  // 初始化立即应用
  applyTheme(isDark.value)

  // 监听变化自动同步
  watch(isDark, (val) => applyTheme(val))

  function toggleTheme() {
    isDark.value = !isDark.value
  }

  function setTheme(dark: boolean) {
    isDark.value = dark
  }

  return {
    isDark,
    toggleTheme,
    setTheme,
  }
})
