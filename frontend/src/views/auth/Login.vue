<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-header">
        <div class="login-logo">
          <inbox-outlined />
        </div>
        <h1 class="login-title">库存CRM系统</h1>
      </div>

      <a-alert
        class="login-alert"
        type="info"
        show-icon
        message="当前仅开放内部账号登录"
        description="系统不提供自助注册与找回密码入口，如需开通账号或重置密码，请联系管理员。"
      />

      <a-form
        ref="formRef"
        :model="formState"
        :rules="rules"
        :label-col="{ span: 0 }"
        :wrapper-col="{ span: 24 }"
        @finish="handleLogin"
        class="login-form"
      >
        <a-form-item name="username">
          <a-input
            v-model:value="formState.username"
            size="large"
            placeholder="用户名"
            :disabled="loading"
            @press-enter="handleLogin"
          >
            <template #prefix>
              <user-outlined />
            </template>
          </a-input>
        </a-form-item>

        <a-form-item name="password">
          <a-input-password
            v-model:value="formState.password"
            size="large"
            placeholder="密码"
            :disabled="loading"
            @press-enter="handleLogin"
          >
            <template #prefix>
              <lock-outlined />
            </template>
          </a-input-password>
        </a-form-item>

        <a-form-item>
          <a-checkbox v-model:checked="formState.rememberMe">
            记住我
          </a-checkbox>
        </a-form-item>

        <a-form-item>
          <a-button
            type="primary"
            html-type="submit"
            size="large"
            :loading="loading"
            :disabled="loading"
            block
          >
            {{ loading ? '登录中...' : '登录' }}
          </a-button>
        </a-form-item>

        <div v-if="errorMessage" class="error-message">
          <exclamation-circle-outlined />
          {{ errorMessage }}
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
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ExclamationCircleOutlined,
  InboxOutlined,
  LockOutlined,
  UserOutlined,
} from '@ant-design/icons-vue'
import type { Rule } from 'ant-design-vue/es/form'
import { useAuthStore } from '@/stores/auth'
import { resolveHomePath } from '@/router/accessControl'
import type { LoginRequest } from '@/types/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const formRef = ref()

const formState = reactive<LoginRequest>({
  username: '',
  password: '',
  rememberMe: false,
})

const loading = ref(false)
const errorMessage = ref('')

const rules: Record<string, Rule[]> = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度为3-20个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为6-20个字符', trigger: 'blur' },
  ],
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
    if (error.errorFields) {
      errorMessage.value = '请检查表单输入'
    } else {
      errorMessage.value = error.message || '登录失败'
    }
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  if (authStore.isAuthenticated) {
    router.push(resolveHomePath(authStore.userRole))
  }
})
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.login-card {
  width: 100%;
  max-width: 420px;
  background: white;
  border-radius: 12px;
  padding: 40px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
}

.login-header {
  text-align: center;
  margin-bottom: 24px;
}

.login-logo {
  width: 64px;
  height: 64px;
  margin: 0 auto 16px;
  background: linear-gradient(135deg, #1890ff, #722ed1);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  color: white;
}

.login-title {
  font-size: 28px;
  font-weight: 600;
  color: #262626;
  margin: 0;
}

.login-alert {
  margin-bottom: 20px;
}

.login-form {
  margin-bottom: 24px;
}

.error-message {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #ff4d4f;
  background: #fff2f0;
  border: 1px solid #ffccc7;
  border-radius: 6px;
  padding: 12px;
  margin-top: 16px;
  font-size: 14px;
}

.login-footer {
  text-align: center;
  color: #8c8c8c;
  font-size: 12px;
}

.login-footer p {
  margin: 4px 0;
}

.support-info {
  color: #bfbfbf;
}

@media (max-width: 480px) {
  .login-container {
    padding: 16px;
  }

  .login-card {
    padding: 24px;
  }

  .login-title {
    font-size: 24px;
  }
}
</style>
