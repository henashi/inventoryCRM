<template>
  <div class="login-container">
    <div class="login-card" id="login-card">
      <div class="login-header">
        <div class="login-logo">
          <inbox-outlined />
        </div>
        <h1 class="login-title" id="login-title">{{ isRegisterMode ? '注册新账号' : '库存CRM系统' }}</h1>
      </div>

      <!-- 登录表单 -->
      <a-form
        v-if="!isRegisterMode"
        ref="formRef"
        :model="formState"
        :rules="rules"
        :label-col="{ span: 0 }"
        :wrapper-col="{ span: 24 }"
        @finish="handleLogin"
        class="login-form"
        id="login-form"
      >
        <a-form-item name="username">
          <a-input
            id="login-username"
            v-model:value="formState.username"
            size="large"
            placeholder="用户名"
            :disabled="loading"
            @press-enter="handleLogin"
          >
            <template #prefix><user-outlined /></template>
          </a-input>
        </a-form-item>

        <a-form-item name="password">
          <a-input-password
            id="login-password"
            v-model:value="formState.password"
            size="large"
            placeholder="密码"
            :disabled="loading"
            @press-enter="handleLogin"
          >
            <template #prefix><lock-outlined /></template>
          </a-input-password>
        </a-form-item>

        <a-form-item>
          <a-checkbox id="login-remember" v-model:checked="formState.rememberMe">记住我</a-checkbox>
        </a-form-item>

        <a-form-item>
          <a-button
            type="primary"
            html-type="submit"
            size="large"
            :loading="loading"
            :disabled="loading"
            block
          >{{ loading ? '登录中...' : '登录' }}</a-button>
        </a-form-item>

        <div v-if="errorMessage" class="error-message">
          <exclamation-circle-outlined /> {{ errorMessage }}
        </div>

        <div class="form-toggle-text">
          <span>没有账号？</span>
          <a-button type="link" @click="switchToRegister">立即注册</a-button>
        </div>
      </a-form>

      <!-- 注册表单 -->
      <a-form
        v-else
        ref="registerFormRef"
        :model="registerForm"
        :rules="registerRules"
        :label-col="{ span: 0 }"
        :wrapper-col="{ span: 24 }"
        @finish="handleRegister"
        class="login-form"
      >
        <a-form-item name="username">
          <a-input
            v-model:value="registerForm.username"
            size="large"
            placeholder="用户名（3-20字符，字母数字下划线）"
            :disabled="registerLoading"
          >
            <template #prefix><user-outlined /></template>
          </a-input>
        </a-form-item>

        <a-form-item name="password">
          <a-input-password
            v-model:value="registerForm.password"
            size="large"
            placeholder="密码（6-20字符）"
            :disabled="registerLoading"
          >
            <template #prefix><lock-outlined /></template>
          </a-input-password>
        </a-form-item>

        <a-form-item name="confirmPassword">
          <a-input-password
            v-model:value="registerForm.confirmPassword"
            size="large"
            placeholder="确认密码"
            :disabled="registerLoading"
          >
            <template #prefix><lock-outlined /></template>
          </a-input-password>
        </a-form-item>

        <a-form-item name="realName">
          <a-input
            v-model:value="registerForm.realName"
            size="large"
            placeholder="真实姓名（可选）"
            :disabled="registerLoading"
          >
            <template #prefix><user-outlined /></template>
          </a-input>
        </a-form-item>

        <a-form-item name="email">
          <a-input
            v-model:value="registerForm.email"
            size="large"
            placeholder="邮箱"
            :disabled="registerLoading"
          >
            <template #prefix><mail-outlined /></template>
          </a-input>
        </a-form-item>

        <a-form-item>
          <a-button
            type="primary"
            html-type="submit"
            size="large"
            :loading="registerLoading"
            :disabled="registerLoading"
            block
          >{{ registerLoading ? '注册中...' : '注册' }}</a-button>
        </a-form-item>

        <div v-if="registerError" class="error-message">
          <exclamation-circle-outlined /> {{ registerError }}
        </div>

        <div class="form-toggle-text">
          <span>已有账号？</span>
          <a-button type="link" @click="switchToLogin">立即登录</a-button>
        </div>
      </a-form>

      <div class="login-footer">
        <p>© 2026 库存CRM系统</p>
        <p class="support-info">技术支持: 400-xxx-xxxx</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
  import { useRoute, useRouter } from 'vue-router'
  import {
    ExclamationCircleOutlined,
    InboxOutlined,
    LockOutlined,
    MailOutlined,
    UserOutlined,
  } from '@ant-design/icons-vue'
  import type { Rule } from 'ant-design-vue/es/form'
  import { useAuthStore } from '@/stores/auth'
  import { useThemeStore } from '@/stores/theme'
  import { resolveHomePath } from '@/router/accessControl'
  import type { LoginRequest } from '@/types/auth'
  import { authApi } from '@/api/auth'
  import { message } from 'ant-design-vue'

  const router = useRouter()
  const route = useRoute()

  // 登录
  const formRef = ref()
  const loading = ref(false)
  const errorMessage = ref('')
  const formState = reactive({
    username: '',
    password: '',
    rememberMe: false,
  })
  const rules: Record<string, Rule[]> = {
    username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
    password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  }

  let mediaQuery: MediaQueryList | null = null

  const applySystemTheme = () => {
    const isDark = window.matchMedia('(prefers-color-scheme: dark)').matches
    document.documentElement.setAttribute('data-theme', isDark ? 'dark' : 'light')
  }

  const handleLogin = async () => {
    try {
      loading.value = true
      errorMessage.value = ''
      await formRef.value?.validate()
      const result = await authStore.login(formState)
      if (result.success) {
        const redirect = route.query.redirect as string | undefined
        router.push(redirect || resolveHomePath(authStore.userRole))
      } else {
        errorMessage.value = result.error || '登录失败'
      }
    } catch (error: any) {
      errorMessage.value = error.errorFields ? '请检查表单输入' : error.message || '登录失败'
    } finally {
      loading.value = false
    }
  }

  // 注册
  const isRegisterMode = ref(false)
  const registerLoading = ref(false)
  const registerError = ref('')
  const registerFormRef = ref()
  const registerForm = reactive({
    username: '',
    password: '',
    confirmPassword: '',
    realName: '',
    email: '',
  })
  const registerRules: Record<string, Rule[]> = {
    username: [
      { required: true, message: '请输入用户名', trigger: 'blur' },
      { min: 3, max: 20, message: '长度 3-20', trigger: 'blur' },
      { pattern: /^[a-zA-Z0-9_]+$/, message: '字母数字下划线', trigger: 'blur' },
    ],
    password: [
      { required: true, message: '请输入密码', trigger: 'blur' },
      { min: 6, max: 20, message: '长度 6-20', trigger: 'blur' },
    ],
    confirmPassword: [
      { required: true, message: '请确认密码', trigger: 'blur' },
      {
        validator: (_rule: any, value: string) => {
          if (value !== registerForm.password) {
            return Promise.reject(new Error('两次密码不一致'))
          }
          return Promise.resolve()
        },
        trigger: 'blur',
      },
    ],
    realName: [{ max: 20, message: '不能超过20字符', trigger: 'blur' }],
    email: [
      { required: true, message: '请输入邮箱', trigger: 'blur' },
      { type: 'email', message: '邮箱格式不正确', trigger: 'blur' },
    ],
  }

  const switchToRegister = () => {
    isRegisterMode.value = true
    registerError.value = ''
  }

  const switchToLogin = () => {
    isRegisterMode.value = false
    errorMessage.value = ''
  }

  const handleRegister = async () => {
    try {
      await registerFormRef.value?.validate()
    } catch {
      return
    }
    registerLoading.value = true
    registerError.value = ''
    try {
      await authApi.register(registerForm as any)
      message.success('注册成功，请登录')
      isRegisterMode.value = false
    } catch (err: any) {
      registerError.value = err.response?.data?.message || '注册失败'
    } finally {
      registerLoading.value = false
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
  .login-container {
    min-height: 100vh;
    display: flex;
    align-items: center;
    justify-content: center;
    background: var(--bg-color, #f0f2f5);
  }

  .login-card {
    width: 400px;
    padding: 40px;
    background: var(--card-bg, #fff);
    border-radius: 8px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  }

  .login-header {
    text-align: center;
    margin-bottom: 32px;
  }

  .login-logo {
    font-size: 48px;
    color: var(--primary-color, #1890ff);
    margin-bottom: 12px;
  }

  .login-title {
    font-size: 24px;
    font-weight: 700;
    margin: 0;
    color: var(--text-color, #111827);
  }

  .login-form {
    margin-bottom: 24px;
  }

  .login-footer {
    text-align: center;
    color: var(--text-secondary, #6b7280);
    font-size: 12px;
    line-height: 1.8;
  }

  .login-footer .support-info {
    color: var(--text-tertiary, #9ca3af);
    font-size: 11px;
  }

  .error-message {
    color: #f5222d;
    font-size: 13px;
    text-align: center;
    margin-bottom: 12px;
  }

  .form-toggle-text {
    text-align: center;
    font-size: 13px;
    color: var(--text-secondary, #6b7280);
    margin-top: 16px;
  }
</style>
