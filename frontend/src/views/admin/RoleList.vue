<template>
  <div class="role-list-page">
    <a-card :bordered="false">
      <template #extra>
        <a-button type="primary" @click="openCreateModal">
          <template #icon><plus-outlined /></template>
          新建角色
        </a-button>
      </template>

      <a-table
        :data-source="roles"
        :columns="columns"
        :loading="loading"
        row-key="id"
        size="middle"
        :pagination="false"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <a-tag :color="roleTagColor(record.name)">{{ record.displayName || record.name }}</a-tag>
            <span class="role-name-tag">{{ record.name }}</span>
          </template>
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === '1' ? 'green' : 'red'">
              {{ record.status === '1' ? '启用' : '禁用' }}
            </a-tag>
          </template>
          <template v-if="column.key === 'sortOrder'">
            {{ record.sortOrder }}
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="openEditModal(record)">编辑</a-button>
              <a-button type="link" size="small" @click="goToRolePerm(record)">权限</a-button>
              <a-popconfirm
                :title="`确定删除角色「${record.displayName || record.name}」？`"
                @confirm="handleDelete(record)"
              >
                <a-button
                  type="link"
                  size="small"
                  danger
                  :disabled="isBuiltIn(record.name)"
                >删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 新建/编辑弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="isEditing ? '编辑角色' : '新建角色'"
      :confirm-loading="modalLoading"
      @ok="handleModalOk"
      @cancel="handleModalCancel"
    >
      <a-form ref="formRef" :model="formData" layout="vertical" :rules="formRules">
        <a-form-item label="角色标识" name="name">
          <a-input
            v-model:value="formData.name"
            placeholder="如：SUPER_ADMIN"
            :disabled="isEditing"
          />
        </a-form-item>
        <a-form-item label="显示名称" name="displayName">
          <a-input v-model:value="formData.displayName" placeholder="如：超级管理员" />
        </a-form-item>
        <a-form-item label="描述" name="description">
          <a-textarea v-model:value="formData.description" :rows="2" placeholder="角色描述（可选）" />
        </a-form-item>
        <a-form-item label="排序号" name="sortOrder">
          <a-input-number v-model:value="formData.sortOrder" :min="0" style="width: 120px" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
  import { ref, reactive, onMounted } from 'vue'
  import { useRouter } from 'vue-router'
  import { message } from 'ant-design-vue'
  import { PlusOutlined } from '@ant-design/icons-vue'
  import { adminApi, type RoleCreateParams } from '@/api/admin'
  import type { RoleDTO } from '@/types'

  const router = useRouter()
  const loading = ref(false)
  const roles = ref<RoleDTO[]>([])
  const modalVisible = ref(false)
  const modalLoading = ref(false)
  const isEditing = ref(false)
  const editingId = ref<number | null>(null)
  const formRef = ref()

  const columns = [
    { title: '角色标识', dataIndex: 'name', key: 'name' },
    { title: '排序', dataIndex: 'sortOrder', key: 'sortOrder', width: 80 },
    { title: '描述', dataIndex: 'description', key: 'description' },
    { title: '状态', key: 'status', width: 80 },
    { title: '操作', key: 'action', width: 240, fixed: 'right' as const },
  ]

  const builtInRoles = ['ADMIN', 'MANAGER', 'USER']

  const isBuiltIn = (name: string) => builtInRoles.includes(name)

  const roleTagColor = (name: string) => {
    const map: Record<string, string> = { ADMIN: 'red', MANAGER: 'blue', USER: 'green' }
    return map[name] || 'purple'
  }

  const defaultForm = (): RoleCreateParams & { displayName?: string } => ({
    name: '',
    displayName: '',
    description: '',
    sortOrder: 0,
  })

  const formData = reactive<RoleCreateParams & { displayName?: string }>(defaultForm())

  const formRules = {
    name: [
      { required: true, message: '请输入角色标识', trigger: 'blur' },
      { pattern: /^[A-Za-z][A-Za-z0-9_]*$/, message: '以字母开头，仅含字母数字下划线', trigger: 'blur' },
    ],
  }

  const loadRoles = async () => {
    loading.value = true
    try {
      roles.value = await adminApi.listRoles()
    } catch {
      message.error('加载角色列表失败')
    } finally {
      loading.value = false
    }
  }

  const openCreateModal = () => {
    isEditing.value = false
    editingId.value = null
    Object.assign(formData, defaultForm())
    modalVisible.value = true
  }

  const openEditModal = (record: RoleDTO) => {
    isEditing.value = true
    editingId.value = record.id
    formData.name = record.name
    formData.displayName = record.displayName || ''
    formData.description = record.description || ''
    formData.sortOrder = record.sortOrder
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
        await adminApi.updateRole(editingId.value, {
          displayName: formData.displayName || undefined,
          description: formData.description || undefined,
          sortOrder: formData.sortOrder,
        })
        message.success('角色已更新')
      } else {
        await adminApi.createRole({
          name: formData.name,
          displayName: formData.displayName || undefined,
          description: formData.description || undefined,
          sortOrder: formData.sortOrder,
        })
        message.success('角色已创建')
      }
      modalVisible.value = false
      loadRoles()
    } catch (err: any) {
      message.error(err?.response?.data?.message || '操作失败')
    } finally {
      modalLoading.value = false
    }
  }

  const handleModalCancel = () => {
    modalVisible.value = false
  }

  const handleDelete = async (record: RoleDTO) => {
    try {
      await adminApi.deleteRole(record.id)
      message.success('角色已删除')
      loadRoles()
    } catch (err: any) {
      message.error(err?.response?.data?.message || '删除失败')
    }
  }

  const goToRolePerm = (record: RoleDTO) => {
    router.push({ name: 'RolePermission', params: { roleId: record.id, roleName: record.name } })
  }

  onMounted(() => {
    loadRoles()
  })
</script>

<style scoped>
  .role-list-page {
    padding: 20px;
    min-height: 100%;
  }

  .role-name-tag {
    margin-left: 8px;
    font-size: 12px;
    color: #9ca3af;
  }
</style>
