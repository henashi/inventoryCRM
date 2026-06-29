<template>
  <div class="user-management-page">
    <div class="page-header">
      <div class="page-actions">
        <a-button type="primary" @click="openCreateModal">
          <template #icon><plus-outlined /></template>
          新建用户
        </a-button>
      </div>
    </div>

    <a-card class="search-card">
      <a-form class="search-form" layout="vertical" @finish="handleSearch">
        <a-row :gutter="[16, 16]" style="width: 100%">
          <a-col :xs="24" :sm="12" :md="8">
            <a-form-item label="关键词">
              <a-input
                v-model:value="searchKeyword"
                placeholder="用户名/姓名"
                allow-clear
                @press-enter="handleSearch"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12" :md="6" class="search-actions-col">
            <a-space>
              <a-button type="primary" html-type="submit">搜索</a-button>
              <a-button @click="resetSearch">重置</a-button>
            </a-space>
          </a-col>
        </a-row>
      </a-form>
    </a-card>

    <a-card :bordered="false">
      <a-table
        :data-source="users"
        :columns="columns"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        size="middle"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'role'">
            <a-tag :color="roleColor(record.role)">{{ record.role }}</a-tag>
          </template>
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 1 ? 'green' : 'red'">
              {{ record.status === 1 ? '启用' : '禁用' }}
            </a-tag>
          </template>
          <template v-if="column.key === 'lastLoginAt'">
            {{ record.lastLoginAt ? formatDateTime(record.lastLoginAt) : '--' }}
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="openEditModal(record)">编辑</a-button>
              <a-button
                type="link"
                size="small"
                :danger="record.status === 1"
                @click="handleToggleStatus(record)"
              >
                {{ record.status === 1 ? '禁用' : '启用' }}
              </a-button>
              <a-button type="link" size="small" @click="handleResetPassword(record)">重置密码</a-button>
              <a-popconfirm
                title="确定删除此用户？"
                @confirm="handleDelete(record)"
              >
                <a-button type="link" size="small" danger>删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 新建/编辑弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="isEditing ? '编辑用户' : '新建用户'"
      :confirm-loading="modalLoading"
      @ok="handleModalOk"
      @cancel="handleModalCancel"
    >
      <a-form ref="formRef" :model="formData" layout="vertical" :rules="formRules">
        <a-form-item label="用户名" name="username">
          <a-input v-model:value="formData.username" placeholder="3-50字符，字母数字下划线" />
        </a-form-item>
        <a-form-item label="密码" name="password">
          <a-input-password
            v-model:value="formData.password"
            :placeholder="isEditing ? '留空则不修改' : '6-20字符'"
          />
        </a-form-item>
        <a-form-item label="角色" name="role">
          <a-select v-model:value="formData.role" placeholder="选择角色">
            <a-select-option value="ADMIN">管理员</a-select-option>
            <a-select-option value="MANAGER">经理</a-select-option>
            <a-select-option value="USER">普通用户</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="备注" name="remark">
          <a-textarea v-model:value="formData.remark" :rows="2" placeholder="可选" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { adminApi } from '@/api/admin'
import type { UserDTO } from '@/types'

const loading = ref(false)
const users = ref<UserDTO[]>([])
const searchKeyword = ref('')
const modalVisible = ref(false)
const modalLoading = ref(false)
const isEditing = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref()

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
})

const defaultForm = () => ({
  username: '',
  password: '',
  role: 'USER' as string,
  remark: '',
})

const formData = reactive(defaultForm())

const formRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '长度 3-50', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]+$/, message: '只能包含字母数字下划线', trigger: 'blur' },
  ],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
}

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
  { title: '用户名', dataIndex: 'username', key: 'username' },
  { title: '姓名', dataIndex: 'realName', key: 'realName' },
  { title: '邮箱', dataIndex: 'email', key: 'email' },
  { title: '角色', key: 'role', dataIndex: 'role' },
  { title: '状态', key: 'status', dataIndex: 'status' },
  { title: '最后登录', key: 'lastLoginAt', dataIndex: 'lastLoginAt' },
  { title: '操作', key: 'action', width: 280, fixed: 'right' as const },
]

const roleColor = (role: string) => {
  const map: Record<string, string> = { ADMIN: 'red', MANAGER: 'blue', USER: 'green' }
  return map[role] || 'default'
}

const formatDateTime = (val: string) => {
  return val ? val.replace('T', ' ').slice(0, 19) : '--'
}

const loadUsers = async () => {
  loading.value = true
  try {
    const res = await adminApi.listUsers(
      searchKeyword.value || undefined,
      pagination.current - 1,
      pagination.pageSize,
    )
    users.value = res.content || []
    pagination.total = res.totalElements || 0
  } catch {
    message.error('加载用户列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.current = 1
  loadUsers()
}

const resetSearch = () => {
  searchKeyword.value = ''
  pagination.current = 1
  loadUsers()
}

const handleTableChange = (pag: { current: number; pageSize: number }) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadUsers()
}

const openCreateModal = () => {
  isEditing.value = false
  editingId.value = null
  Object.assign(formData, defaultForm())
  modalVisible.value = true
}

const openEditModal = (record: UserDTO) => {
  isEditing.value = true
  editingId.value = record.id
  formData.username = record.username
  formData.password = ''
  formData.role = record.role
  formData.remark = record.remark || ''
  modalVisible.value = true
}

const handleModalOk = async () => {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  modalLoading.value = true
  try {
    if (isEditing.value && editingId.value) {
      await adminApi.updateUser(editingId.value, formData as any)
      message.success('用户已更新')
    } else {
      await adminApi.createUser(formData as any)
      message.success('用户已创建')
    }
    modalVisible.value = false
    loadUsers()
  } catch (err: any) {
    message.error(err.response?.data?.message || '操作失败')
  } finally {
    modalLoading.value = false
  }
}

const handleModalCancel = () => {
  modalVisible.value = false
}

const handleToggleStatus = (record: UserDTO) => {
  const action = record.status === 1 ? '禁用' : '启用'
  Modal.confirm({
    title: `确认${action}用户`,
    content: `${action}用户「${record.username}」？`,
    onOk: async () => {
      try {
        await adminApi.toggleStatus(record.id)
        message.success(`用户已${action}`)
        loadUsers()
      } catch {
        message.error('操作失败')
      }
    },
  })
}

const handleResetPassword = (record: UserDTO) => {
  Modal.confirm({
    title: '确认重置密码',
    content: `将用户「${record.username}」的密码重置为 123456？`,
    onOk: async () => {
      try {
        await adminApi.resetPassword(record.id)
        message.success('密码已重置为 123456')
      } catch {
        message.error('重置失败')
      }
    },
  })
}

const handleDelete = async (record: UserDTO) => {
  try {
    await adminApi.deleteUser(record.id)
    message.success('用户已删除')
    loadUsers()
  } catch {
    message.error('删除失败')
  }
}

onMounted(loadUsers)
</script>

<style scoped>
.user-management-page {
  padding: 20px;
  min-height: 100%;
}

.page-header {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 16px;
}

.search-card {
  margin-bottom: 16px;
}

.search-form {
  width: 100%;
}

.search-actions-col {
  display: flex;
  align-items: flex-end;
  padding-bottom: 24px;
}
</style>
