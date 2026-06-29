<template>
  <div class="login-container">
    <div class="login-card" id="login-card">
      <div class="login-header">
        <div class="login-logo">
          <inbox-outlined />
        </div>
        <h1 class="login-title" id="login-title">库存CRM系统</h1>
      </div>

      <a-form ref="formRef" :model="formState" :rules="rules" :label-col="{ span: 0 }" :wrapper-col="{ span: 24 }" @finish="handleSubmit" class="login-form" id="login-form">
        <a-form-item name="username">
          <a-input id="login-username" v-model:value="formState.username" size="large" placeholder="用户名" :disabled="loading">
            <template #prefix><user-outlined /></template>
          </a-input>
        </a-form-item>

        <a-form-item name="password">
          <a-input-password id="login-password" v-model:value="formState.password" size="large" placeholder="密码" :disabled="loading">
            <template #prefix><lock-outlined /></template>
          </a-input-password>
        </a-form-item>

        <a-form-item v-if="showRegister" name="confirmPassword">
          <a-input-password v-model:value="formState.confirmPassword" size="large" placeholder="确认密码" :disabled="loading">
            <template #prefix><lock-outlined /></template>
          </a-input-password>
        </a-form-item>

        <a-form-item v-if="showRegister" name="email" class="email-item">
          <div class="email-row">
            <inbox-outlined class="email-prefix" />
            <a-input v-model:value="emailName" size="large" placeholder="邮箱账号" :disabled="loading" class="email-name-input" />
            <a-select v-model:value="emailDomain" size="large" style="width:100px;flex-shrink:0" :disabled="loading" class="email-domain-select">
              <a-select-option value="">自定义</a-select-option>
              <a-select-option value="@qq.com">@qq.com</a-select-option>
              <a-select-option value="@163.com">@163.com</a-select-option>
              <a-select-option value="@gmail.com">@gmail.com</a-select-option>
              <a-select-option value="@outlook.com">@outlook.com</a-select-option>
              <a-select-option value="@foxmail.com">@foxmail.com</a-select-option>
              <a-select-option value="@126.com">@126.com</a-select-option>
              <a-select-option value="@sina.com">@sina.com</a-select-option>
              <a-select-option value="@yeah.net">@yeah.net</a-select-option>
            </a-select>
          </div>
        </a-form-item>
        <a-form-item v-if="showRegister" name="phone">
          <a-input v-model:value="formState.phone" size="large" placeholder="手机号（可选）" :disabled="loading" maxlength="11">
            <template #prefix><phone-outlined /></template>
          </a-input>
        </a-form-item>

        <a-form-item v-if="!showRegister">
          <a-checkbox id="login-remember" v-model:checked="formState.rememberMe">记住我</a-checkbox>
        </a-form-item>

        <a-form-item>
          <a-button type="primary" html-type="submit" size="large" :loading="loading" :disabled="loading" block>{{ loading ? '提交中...' : (showRegister ? '注册' : '登录') }}</a-button>
        </a-form-item>

        <div v-if="errorMessage" class="error-message"><exclamation-circle-outlined /> {{ errorMessage }}</div>
      </a-form>

      <div class="register-entry"><span v-if="!showRegister">没有账号？</span><span v-else>已有账号？</span> <span class="register-link" @click="toggleRegister">{{ showRegister ? '立即登录' : '立即注册' }}</span></div>

      <div class="login-footer"><p>© 2026 库存CRM系统</p><p class="support-info">技术支持: 400-xxx-xxxx</p></div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
  import { useRoute, useRouter } from 'vue-router'
  import { ExclamationCircleOutlined, InboxOutlined, LockOutlined, PhoneOutlined, UserOutlined } from '@ant-design/icons-vue'
  import type { Rule } from 'ant-design-vue/es/form'
  import { useAuthStore } from '@/stores/auth'
  import { useThemeStore } from '@/stores/theme'
  import { resolveHomePath } from '@/router/accessControl'
  import type { LoginRequest } from '@/types/auth'
  import { authApi } from '@/api/auth'
  import { message } from 'ant-design-vue'

  const router = useRouter()
  const route = useRoute()
  const authStore = useAuthStore()
  const formRef = ref()

  const showRegister = ref(false)
  const loading = ref(false)
  const errorMessage = ref('')
  const emailName = ref('')
  const emailDomain = ref('')

  const formState = reactive({
    username: '',
    password: '',
    confirmPassword: '',
    email: '',
    phone: '',
    rememberMe: false,
  })

  const rules: Record<string, Rule[]> = {
    username: [
      { required: true, message: '请输入用户名', trigger: 'blur' },
      { min: 3, max: 20, message: '用户名长度为3-20个字符', trigger: 'blur' },
    ],
    password: [
      { required: true, message: '请输入密码', trigger: 'blur' },
      { min: 6, max: 20, message: '密码长度为6-20个字符', trigger: 'blur' },
    ],
    confirmPassword: [
      { required: true, message: '请确认密码', trigger: 'blur' },
      {
        validator: (_rule: any, value: string) => {
          if (value !== formState.password) return Promise.reject(new Error('两次密码不一致'))
          return Promise.resolve()
        },
        trigger: 'blur',
      },
    ],
    email: [
      { type: 'email', message: '邮箱格式不正确', trigger: 'blur' },
    ],
    phone: [
      { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' },
    ],
  }



  let mediaQuery: MediaQueryList | null = null

  function applySystemTheme() {
    const isDark = window.matchMedia('(prefers-color-scheme: dark)').matches
    document.documentElement.setAttribute('data-theme', isDark ? 'dark' : 'light')
    useThemeStore().setTheme(isDark)
  }

  const toggleRegister = () => {
    showRegister.value = !showRegister.value
    errorMessage.value = ''
    emailName.value = ''
    emailDomain.value = ''
  }

  const handleSubmit = async () => {
    if (showRegister.value) {
      await handleRegister()
    } else {
      await handleLogin()
    }
  }

  const handleLogin = async () => {
    try {
      loading.value = true
      errorMessage.value = ''
      await formRef.value?.validate()
      const result = await authStore.login({ username: formState.username, password: formState.password, rememberMe: formState.rememberMe })
      if (result.success) {
        const redirect = route.query.redirect as string | undefined
        router.push(redirect || resolveHomePath(authStore.userRole))
      } else {
        errorMessage.value = result.error || '登录失败'
      }
    } catch (error: any) {
      errorMessage.value = error.errorFields ? '请检查表单输入' : (error.message || '登录失败')
    } finally {
      loading.value = false
    }
  }

  const handleRegister = async () => {
    try {
      loading.value = true
      errorMessage.value = ''
      formState.email = emailName.value + emailDomain.value
      await formRef.value?.validate()
      await authApi.register({
        username: formState.username,
        password: formState.password,
        confirmPassword: formState.confirmPassword,
        email: formState.email,
        phone: formState.phone || undefined,
      })
      message.success('注册成功，请登录')
      showRegister.value = false
    } catch (error: any) {
      errorMessage.value = error.response?.data?.message || '注册失败'
    } finally {
      loading.value = false
    }
  }

  onMounted(() => {
    if (authStore.isAuthenticated) router.push(resolveHomePath(authStore.userRole))
    localStorage.removeItem('theme-mode')
    applySystemTheme()
    mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
    mediaQuery.addEventListener('change', applySystemTheme)
  })

  onBeforeUnmount(() => {
    mediaQuery?.removeEventListener('change', applySystemTheme)
  })
</script>

<style scoped>
  .login-container { min-height: 100vh; display: flex; align-items: center; justify-content: center; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 20px; }
  .login-card { width: 100%; max-width: 420px; background: #ffffff; border-radius: 12px; padding: 40px; box-shadow: 0 20px 40px rgba(0,0,0,0.1); }
  .login-header { text-align: center; padding-top: 32px; margin-bottom: 32px; }
  .login-logo { width: 64px; height: 64px; margin: 0 auto 16px; background: linear-gradient(135deg, #1890ff, #722ed1); border-radius: 16px; display: flex; align-items: center; justify-content: center; font-size: 32px; color: white; }
  .login-title { font-size: 28px; font-weight: 600; color: #262626; margin: 0; }
  .login-form { margin-bottom: 24px; }
  .error-message { display: flex; align-items: center; gap: 8px; color: #ff4d4f; background: #fff2f0; border: 1px solid #ffccc7; border-radius: 6px; padding: 12px; margin-top: 16px; font-size: 14px; }
  .login-footer { text-align: center; color: #8c8c8c; font-size: 12px; }
  .login-footer p { margin: 4px 0; }
  .support-info { color: #bfbfbf; }
  .register-entry { text-align: center; font-size: 13px; color: #8c8c8c; margin-bottom: 24px; }
  .register-entry .register-link { color: #1890ff; cursor: pointer; }
  .register-entry .register-link:hover { color: #40a9ff; }
  .email-item .ant-form-item-control-input { min-height: unset; }
  .email-row { display: flex; align-items: center; border: 1px solid #d9d9d9; border-radius: 6px; transition: border-color 0.2s; }
  .email-row:focus-within { border-color: #4096ff; box-shadow: 0 0 0 2px rgba(24,144,255,0.1); }
  .email-prefix { font-size: 16px; color: #8c8c8c; margin: 0 8px; flex-shrink: 0; }
  .email-name-input { border: none !important; box-shadow: none !important; flex: 1; }
  .email-name-input.ant-input-lg { border-radius: 0 !important; }
  .email-domain-select .ant-select-selector { border: none !important; box-shadow: none !important; border-left: 1px solid #d9d9d9 !important; border-radius: 0 !important; }
  @media (max-width: 480px) { .login-container { padding: 16px; } .login-card { padding: 24px; } .login-title { font-size: 24px; } }
  [data-theme='dark'] .login-card { background: var(--bg-card); box-shadow: 0 20px 40px rgba(0,0,0,0.4); }
  [data-theme='dark'] .login-title { color: var(--text-primary); }
  [data-theme='dark'] .login-footer { color: var(--text-secondary); }
  [data-theme='dark'] .support-info { color: var(--text-tertiary); }
  [data-theme='dark'] .error-message { color: #ff7875; background: rgba(255,77,79,0.1); border-color: rgba(255,77,79,0.3); }
</style>
