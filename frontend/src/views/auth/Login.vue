<!-- frontend/src/views/auth/Login.vue -->
<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-header">
        <div class="login-logo">
          <inbox-outlined />
        </div>
        <h1 class="login-title">库存CRM系统</h1>
        <!-- <p class="login-subtitle">请输入您的账户信息</p> -->
      </div>

      <!-- 登录表单 -->
      <a-form
        ref="formRef"
        :model="formState"
        :rules="rules"
        :label-col="{ span: 0 }"
        :wrapper-col="{ span: 24 }"
        @finish="handleLogin"
        class="login-form"
      >
        <!-- 用户名 -->
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

        <!-- 密码 -->
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

        <!-- 记住我 -->
        <a-form-item>
          <a-checkbox v-model:checked="formState.rememberMe">
            记住我
          </a-checkbox>
        </a-form-item>

        <!-- 登录按钮 -->
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

        <!-- 错误信息 -->
        <div v-if="errorMessage" class="error-message">
          <exclamation-circle-outlined />
          {{ errorMessage }}
        </div>
      </a-form>

      <!-- 底部信息 -->
      <div class="login-footer">
        <p>© 2026 库存CRM系统</p>
        <p class="support-info">技术支持: 400-xxx-xxxx</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { 
  UserOutlined, 
  LockOutlined, 
  InboxOutlined,
  ExclamationCircleOutlined 
} from '@ant-design/icons-vue'
import { useAuthStore } from '@/stores/auth'
import type { LoginRequest } from '@/types/auth'
import type { Rule } from 'ant-design-vue/es/form'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const formRef = ref()

// 表单数据
const formState = reactive<LoginRequest>({
  username: '',
  password: '',
  rememberMe: false
})

// 状态
const loading = ref(false)
const errorMessage = ref('')

// 表单验证规则
const rules: Record<string, Rule[]> = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度为3-20个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为6-20个字符', trigger: 'blur' }
  ]
}

// 处理登录
const handleLogin = async () => {
  try {
    loading.value = true
    errorMessage.value = ''
    
    // 验证表单
    await formRef.value?.validate()
    
    // 调用登录
    const result = await authStore.login(formState)
    
    if (result.success) {
      // 登录成功后的跳转
      const redirect = route.query.redirect as string
      router.push(redirect || '/dashboard')
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

// 页面加载时检查登录状态
onMounted(() => {
  if (authStore.isAuthenticated) {
    router.push('/dashboard')
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
  margin-bottom: 32px;
}

.login-logo {
  width: 80px;
  height: 80px;
  margin: 0 auto 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 36px;
}

.login-title {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
}

.login-subtitle {
  color: #666;
  font-size: 14px;
}

.login-form {
  margin-bottom: 24px;
}

.error-message {
  background: #fff2f0;
  border: 1px solid #ffccc7;
  color: #ff4d4f;
  padding: 12px 16px;
  border-radius: 6px;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.login-footer {
  text-align: center;
  color: #666;
  font-size: 14px;
  border-top: 1px solid #f0f0f0;
  padding-top: 20px;
}

.support-info {
  font-size: 12px;
  margin-top: 4px;
  color: #999;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .login-card {
    padding: 24px;
    max-width: 100%;
  }
  
  .login-logo {
    width: 60px;
    height: 60px;
    font-size: 24px;
  }
  
  .login-title {
    font-size: 20px;
  }
}
</style>