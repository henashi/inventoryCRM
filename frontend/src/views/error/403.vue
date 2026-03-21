<template>
  <div class="error-page">
    <div class="error-card">
      <h1>403</h1>
      <p>权限不足，无法访问该页面。</p>
      <a-button type="primary" @click="goHome">返回首页</a-button>
      <a-button type="primary" @click="logout">退出登录</a-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
const router = useRouter()
const authStore = useAuthStore()
const goHome = () => router.push('/dashboard')
const logout = () => {
  authStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.error-page{
  min-height: 60vh;
  display:flex;
  align-items:center;
  justify-content:center;
}
.error-card{
  text-align:center;
  background:#fff;
  padding:40px;
  border-radius:8px;
  box-shadow:0 8px 24px rgba(0,0,0,0.08);
}
.error-card h1{
  font-size:48px;
  margin:0 0 8px;
}
.error-card p{
  color:#666;
  margin-bottom:16px;
}
</style>
