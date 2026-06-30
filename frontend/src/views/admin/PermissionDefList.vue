<template>
  <div class="perm-def-page">
    <a-card :bordered="false">
      <template #extra>
        <a-button type="primary" @click="openCreateModal">
          <template #icon><plus-outlined /></template>
          新增权限
        </a-button>
      </template>

      <a-table
        :data-source="list"
        :columns="columns"
        :loading="loading"
        row-key="id"
        size="middle"
        :pagination="{ pageSize: 20, showTotal: (t: number) => `共 ${t} 条` }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'type'">
            <a-tag :color="typeColor(record.type)">{{ typeLabel(record.type) }}</a-tag>
          </template>
          <template v-if="column.key === 'defaultRoles'">
            <span style="font-size: 12px; color: #6b7280">{{ record.defaultRoles }}</span>
          </template>
          <template v-if="column.key === 'moduleName'">
            <span v-if="record.moduleName">{{ record.moduleName }}</span>
            <span v-else style="color: #9ca3af">{{ record.module }}</span>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="openEditModal(record)">编辑</a-button>
              <a-popconfirm title="确定删除此权限？" @confirm="handleDelete(record)">
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
      :title="isEditing ? '编辑权限' : '新增权限'"
      :confirm-loading="modalLoading"
      @ok="handleModalOk"
      @cancel="handleModalCancel"
      width="560"
    >
      <a-form ref="formRef" :model="formData" layout="vertical" :rules="formRules">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="权限标识" name="key">
              <a-input v-model:value="formData.key" placeholder="如 products:export" :disabled="isEditing" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="权限名称" name="name">
              <a-input v-model:value="formData.name" placeholder="如 导出商品" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-item label="模块标识" name="module">
              <a-input v-model:value="formData.module" placeholder="如 products" :disabled="isEditing" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="模块名称" name="moduleName">
              <a-input v-model:value="formData.moduleName" placeholder="如 商品管理" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="类型" name="type">
              <a-select v-model:value="formData.type">
                <a-select-option value="ACTION">操作</a-select-option>
                <a-select-option value="MENU">菜单</a-select-option>
                <a-select-option value="API">接口</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="默认角色" name="defaultRoles">
          <a-input v-model:value="formData.defaultRoles" placeholder='如 ["ADMIN","MANAGER"]' />
        </a-form-item>
        <a-form-item label="描述" name="description">
          <a-textarea v-model:value="formData.description" :rows="2" placeholder="可选" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
  import { ref, reactive, onMounted } from 'vue'
  import { message } from 'ant-design-vue'
  import { PlusOutlined } from '@ant-design/icons-vue'
  import { adminApi } from '@/api/admin'
  import type { PermissionDefDTO } from '@/types'

  const loading = ref(false)
  const list = ref<PermissionDefDTO[]>([])
  const modalVisible = ref(false)
  const modalLoading = ref(false)
  const isEditing = ref(false)
  const editingId = ref<number | null>(null)
  const formRef = ref()

  const columns = [
    { title: '标识', dataIndex: 'key', key: 'key', width: 180 },
    { title: '名称', dataIndex: 'name', key: 'name', width: 140 },
    { title: '模块', key: 'moduleName', width: 100 },
    { title: '类型', key: 'type', width: 80 },
    { title: '默认角色', key: 'defaultRoles', ellipsis: true },
    { title: '操作', key: 'action', width: 140, fixed: 'right' as const },
  ]

  const typeColor = (t: string) => {
    const map: Record<string, string> = { MENU: 'blue', API: 'orange', ACTION: 'green' }
    return map[t] || 'default'
  }

  const typeLabel = (t: string) => {
    const map: Record<string, string> = { MENU: '菜单', API: '接口', ACTION: '操作' }
    return map[t] || t
  }

  const defaultForm = () => ({
    key: '',
    name: '',
    module: '',
    moduleName: '',
    type: 'ACTION' as string,
    defaultRoles: '',
    description: '',
  })

  const formData = reactive(defaultForm())

  const formRules = {
    key: [{ required: true, message: '请输入权限标识', trigger: 'blur' }],
    name: [{ required: true, message: '请输入权限名称', trigger: 'blur' }],
  }

  const loadList = async () => {
    loading.value = true
    try {
      list.value = await adminApi.listPermissionDefs()
    } catch {
      message.error('加载权限列表失败')
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

  const openEditModal = (record: PermissionDefDTO) => {
    isEditing.value = true
    editingId.value = record.id
    formData.key = record.key
    formData.name = record.name
    formData.module = record.module || ''
    formData.moduleName = record.moduleName || ''
    formData.type = record.type
    formData.defaultRoles = record.defaultRoles || ''
    formData.description = record.description || ''
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
        await adminApi.updatePermissionDef(editingId.value, {
          name: formData.name,
          moduleName: formData.moduleName || undefined,
          type: formData.type,
          defaultRoles: formData.defaultRoles || undefined,
          description: formData.description || undefined,
        })
        message.success('权限已更新')
      } else {
        await adminApi.createPermissionDef({
          key: formData.key,
          name: formData.name,
          module: formData.module || undefined,
          moduleName: formData.moduleName || undefined,
          type: formData.type,
          defaultRoles: formData.defaultRoles || undefined,
          description: formData.description || undefined,
        })
        message.success('权限已创建')
      }
      modalVisible.value = false
      loadList()
    } catch (err: any) {
      message.error(err?.response?.data?.message || '操作失败')
    } finally {
      modalLoading.value = false
    }
  }

  const handleModalCancel = () => {
    modalVisible.value = false
  }

  const handleDelete = async (record: PermissionDefDTO) => {
    try {
      await adminApi.deletePermissionDef(record.id)
      message.success('权限已删除')
      loadList()
    } catch (err: any) {
      message.error(err?.response?.data?.message || '删除失败')
    }
  }

  onMounted(() => {
    loadList()
  })
</script>

<style scoped>
  .perm-def-page {
    padding: 20px;
    min-height: 100%;
  }
</style>
