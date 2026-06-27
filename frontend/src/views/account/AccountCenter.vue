<template>
  <div class="account-page">
    <div class="page-header">
      <div>

        <p class="page-subtitle">维护个人资料、安全设置与退出策略说明</p>
      </div>
      <a-space>
        <a-button @click="goToDashboard">
          <home-outlined />
          返回首页
        </a-button>
        <a-button danger @click="handleLogout">
          <logout-outlined />
          退出登录
        </a-button>
      </a-space>
    </div>

    <a-alert
      class="page-alert"
      type="info"
      show-icon
      message="当前不开放自助注册与找回密码"
      description="账号创建与密码重置通过管理员或线下支持流程处理；用户名不可修改，如需变更请联系管理员。"
    />

    <a-tabs v-model:activeKey="activeTab">
      <a-tab-pane key="profile" tab="个人资料">
        <a-row class="profile-content-row" :gutter="[16, 16]">
          <a-col :xs="24" :xl="14">
            <a-card class="profile-card profile-summary-card" title="基本信息">
              <template #extra>
                <a-button
                  v-if="!isEditingProfile"
                  class="profile-card-extra"
                  type="primary"
                  @click="startProfileEdit"
                >
                  编辑资料
                </a-button>
              </template>
              <template v-if="!isEditingProfile">
                <a-descriptions :column="1" bordered size="small">
                  <a-descriptions-item label="用户名">{{ currentUser?.username || '--' }}</a-descriptions-item>
                  <a-descriptions-item label="真实姓名">{{ currentUser?.realName || '--' }}</a-descriptions-item>
                  <a-descriptions-item label="邮箱">{{ currentUser?.email || '--' }}</a-descriptions-item>
                </a-descriptions>
                <a-alert
                  class="form-alert"
                  type="info"
                  show-icon
                  message="如需修改资料，请进入编辑模式"
                  description="用户名不可修改，当前仅支持更新真实姓名与邮箱。"
                />
              </template>
              <a-form
                v-else
                ref="profileFormRef"
                :model="profileForm"
                :rules="profileRules"
                layout="vertical"
              >
                <a-form-item label="用户名">
                  <a-input v-model:value="profileForm.username" disabled />
                </a-form-item>
                <a-alert
                  class="form-alert"
                  type="warning"
                  show-icon
                  message="用户名不可修改"
                  description="当前接口仅支持更新真实姓名与邮箱，如需调整用户名请联系管理员。"
                />
                <a-form-item label="真实姓名" name="realName">
                  <a-input v-model:value="profileForm.realName" maxlength="20" placeholder="请输入真实姓名" />
                </a-form-item>
                <a-form-item label="邮箱" name="email">
                  <a-input v-model:value="profileForm.email" maxlength="50" placeholder="请输入邮箱" />
                </a-form-item>
                <a-space>
                  <a-button type="primary" :loading="profileSubmitting" @click="handleProfileSubmit">
                    保存资料
                  </a-button>
                  <a-button @click="cancelProfileEdit">取消编辑</a-button>
                  <a-button @click="syncProfileForm">重置</a-button>
                </a-space>
              </a-form>
            </a-card>
          </a-col>

          <a-col :xs="24" :xl="10">
            <a-card class="account-card" title="账号信息">
              <a-descriptions :column="1" bordered size="small">
                <a-descriptions-item label="角色">{{ roleText }}</a-descriptions-item>
                <a-descriptions-item label="状态">{{ statusText }}</a-descriptions-item>
                <a-descriptions-item label="创建时间">{{ formatDateTime(currentUser?.createdAt) }}</a-descriptions-item>
                <a-descriptions-item label="最近登录">{{ formatDateTime(currentUser?.lastLoginAt) }}</a-descriptions-item>
                <a-descriptions-item label="邮箱校验">
                  {{ currentUser?.email ? '已配置' : '未配置' }}
                </a-descriptions-item>
              </a-descriptions>
            </a-card>
          </a-col>
        </a-row>

        <a-row :gutter="[16, 16]" style="margin-top: 58px;">
          <a-col :xs="24">
            <a-card title="角色与权限说明" class="section-card">
              <div class="role-header">
                <span>当前登录</span>
                <a-tag :color="currentRoleColor">{{ roleText }}</a-tag>
              </div>
              <div class="permission-list">
                <div v-for="item in permissionItems" :key="item.title" class="permission-item">
                  <div class="permission-title-row">
                    <strong>{{ item.title }}</strong>
                    <a-tag :color="item.color">{{ item.roleName }}</a-tag>
                  </div>
                  <div class="permission-desc">{{ item.description }}</div>
                </div>
              </div>
            </a-card>
          </a-col>
        </a-row>
      </a-tab-pane>

      <a-tab-pane key="password" tab="修改密码">
        <a-card title="安全设置">
          <a-alert
            class="form-alert"
            type="warning"
            show-icon
            message="修改密码后将立即退出当前登录"
            description="系统会同步清空本地 access token、refresh token 与用户缓存，请使用新密码重新登录。"
          />
          <a-form
            ref="passwordFormRef"
            :model="passwordForm"
            :rules="passwordRules"
            layout="vertical"
          >
            <a-form-item label="旧密码" name="oldPassword">
              <a-input-password v-model:value="passwordForm.oldPassword" placeholder="请输入当前密码" />
            </a-form-item>
            <a-form-item label="新密码" name="newPassword">
              <a-input-password v-model:value="passwordForm.newPassword" placeholder="请输入新密码" />
            </a-form-item>
            <a-form-item label="确认新密码" name="confirmPassword">
              <a-input-password v-model:value="passwordForm.confirmPassword" placeholder="请再次输入新密码" />
            </a-form-item>
            <a-space>
              <a-button type="primary" :loading="passwordSubmitting" @click="handlePasswordSubmit">
                更新密码
              </a-button>
              <a-button @click="resetPasswordForm">清空</a-button>
            </a-space>
          </a-form>
        </a-card>
      </a-tab-pane>
    </a-tabs>
  </div>
</template>

<script setup lang="ts">
import dayjs from 'dayjs'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { FormInstance } from 'ant-design-vue'
import type { Rule } from 'ant-design-vue/es/form'
import { message } from 'ant-design-vue'
import { HomeOutlined, LogoutOutlined } from '@ant-design/icons-vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const profileFormRef = ref<FormInstance>()
const passwordFormRef = ref<FormInstance>()
const profileSubmitting = ref(false)
const passwordSubmitting = ref(false)
const activeTab = ref('profile')
const isEditingProfile = ref(false)

const profileForm = reactive({
  username: '',
  realName: '',
  email: '',
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const profileRules: Record<string, Rule[]> = {
  realName: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' },
    { min: 2, max: 20, message: '真实姓名长度为2-20个字符', trigger: 'blur' },
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' },
  ],
}

const passwordRules: Record<string, Rule[]> = {
  oldPassword: [
    { required: true, message: '请输入旧密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为6-20个字符', trigger: 'blur' },
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为6-20个字符', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: async () => {
        if (passwordForm.confirmPassword !== passwordForm.newPassword) {
          throw new Error('两次输入的新密码不一致')
        }
      },
      trigger: 'blur',
    },
  ],
}

const currentUser = computed(() => authStore.user)
const roleText = computed(() => {
  switch (authStore.userRole) {
    case 'ADMIN':
      return '系统管理员'
    case 'MANAGER':
      return '经理'
    default:
      return '普通用户'
  }
})
const statusText = computed(() => currentUser.value?.status === 1 ? '启用' : '停用')

const currentRoleColor = computed(() => ({
  ADMIN: 'red',
  MANAGER: 'blue',
  USER: 'green',
}[authStore.userRole] || 'blue'))

const permissionItems = [
  {
    title: '系统管理员',
    role: 'ADMIN',
    roleName: '管理员',
    color: 'red',
    description: '可访问系统概览、配置管理、库存与业务模块，适合演示全局管理能力。',
  },
  {
    title: '业务经理',
    role: 'MANAGER',
    roleName: '经理',
    color: 'blue',
    description: '聚焦客户、商品、礼品与库存流程，不暴露系统级配置入口。',
  },
  {
    title: '普通用户',
    role: 'USER',
    roleName: '普通用户',
    color: 'green',
    description: '可查看商品和礼品相关页面，避免进入无权限的系统管理能力。',
  },
]

const applyRouteTab = () => {
  activeTab.value = route.query.tab === 'password' ? 'password' : 'profile'
}

const syncProfileForm = () => {
  profileForm.username = currentUser.value?.username || ''
  profileForm.realName = currentUser.value?.realName || ''
  profileForm.email = currentUser.value?.email || ''
}

const resetPasswordForm = () => {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  passwordFormRef.value?.clearValidate()
}

const startProfileEdit = () => {
  syncProfileForm()
  isEditingProfile.value = true
}

const cancelProfileEdit = () => {
  syncProfileForm()
  profileFormRef.value?.clearValidate()
  isEditingProfile.value = false
}

const formatDateTime = (value?: string) => {
  if (!value) {
    return '--'
  }

  return dayjs(value).isValid() ? dayjs(value).format('YYYY-MM-DD HH:mm:ss') : value
}

const ensureCurrentUser = async () => {
  if (authStore.user) {
    syncProfileForm()
    return
  }

  const authenticated = await authStore.checkAuth()
  if (!authenticated) {
    router.replace('/login')
    return
  }

  syncProfileForm()
}

const handleProfileSubmit = async () => {
  try {
    profileSubmitting.value = true
    await profileFormRef.value?.validate()
    const result = await authStore.updateProfile({
      username: profileForm.username,
      realName: profileForm.realName,
      email: profileForm.email,
    })

    if (!result.success) {
      message.error(result.error)
      return
    }

    syncProfileForm()
    isEditingProfile.value = false
  } finally {
    profileSubmitting.value = false
  }
}

const handlePasswordSubmit = async () => {
  try {
    passwordSubmitting.value = true
    await passwordFormRef.value?.validate()
    const result = await authStore.changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
    })

    if (!result.success) {
      message.error(result.error)
      return
    }

    router.replace('/login')
  } finally {
    passwordSubmitting.value = false
  }
}

const handleLogout = async () => {
  await authStore.logout()
  router.replace('/login')
}

const goToDashboard = () => {
  router.push('/dashboard')
}

watch(() => route.query.tab, applyRouteTab)
watch(currentUser, () => {
  syncProfileForm()
  if (!currentUser.value) {
    isEditingProfile.value = false
  }
})

onMounted(async () => {
  applyRouteTab()
  await ensureCurrentUser()
})
</script>

<style scoped>
.account-page {
  min-height: 100vh;
  padding: 20px;
  background: var(--bg-page);
  transition: background 0.3s ease;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.page-title {
  margin: 0;
  font-size: 28px;
  color: var(--text-primary);
}

.page-subtitle {
  margin: 8px 0 0;
  color: #8c8c8c;
}

.page-alert {
  margin-bottom: 16px;
}

.form-alert {
  margin-bottom: 16px;
}

.profile-content-row {
  align-items: stretch;
}

.profile-card,
.account-card {
  height: 100%;
}

.profile-summary-card :deep(.ant-card-body),
.account-card :deep(.ant-card-body) {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
}

.profile-card-extra {
  margin-inline-start: 8px;
}

.section-card {
  margin-top: 16px;
}

.role-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}

.permission-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.permission-item {
  padding: 14px 16px;
  border: 1px solid var(--border-color);
  border-radius: 12px;
}

.permission-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.permission-desc {
  margin-top: 8px;
  color: var(--text-secondary);
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
