<template>
  <div class="user-management-page">
    <a-tabs v-model:activeKey="activeTab" @change="handleTabChange">
      <template #tabBarExtraContent>
        <a-button type="primary" @click="openCreateModal">
          <template #icon><plus-outlined /></template>
          新建用户
        </a-button>
      </template>
      <a-tab-pane key="users" tab="用户管理">
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
                <a-tag :color="roleColor(record.role)">{{
                  roleLabels[record.role] || record.role
                }}</a-tag>
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
                  <a-button type="link" size="small" @click="handleResetPassword(record)"
                    >重置密码</a-button
                  >
                  <a-button type="link" size="small" @click="openUserPermDrawer(record)"
                    >权限</a-button
                  >
                  <a-popconfirm title="确定删除此用户？" @confirm="handleDelete(record)">
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
                <a-select-option v-for="r in roles" :key="r.value" :value="r.value">
                  {{ r.label }}
                </a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="备注" name="remark">
              <a-textarea v-model:value="formData.remark" :rows="2" placeholder="可选" />
            </a-form-item>
          </a-form>
        </a-modal>

        <!-- 用户权限覆盖抽屉 -->
        <a-drawer
          v-model:open="userPermDrawerVisible"
          :title="'权限覆盖 - ' + (userPermDrawerUser?.username || '')"
          width="480"
        >
          <a-spin :spinning="userPermLoading">
            <template v-if="permDefs.length > 0">
              <div v-for="group in permissionGroups" :key="group.key" class="user-perm-group">
                <div class="perm-group-title-small">{{ group.label }}</div>
                <div v-for="p in group.permissions" :key="p.key" class="user-perm-item">
                  <div class="user-perm-item-label">{{ p.label }}</div>
                  <a-switch
                    size="small"
                    :checked="!!userPermState[p.key]"
                    @change="(val: boolean) => (userPermState[p.key] = val)"
                  />
                </div>
              </div>
            </template>
            <a-empty v-else description="暂无权限定义，请先在配置管理中创建 PERMISSION 分组" />
          </a-spin>
          <template #footer>
            <a-space>
              <a-button @click="userPermDrawerVisible = false">取消</a-button>
              <a-button type="primary" :loading="userPermSaving" @click="saveUserPerms">
                保存权限覆盖
              </a-button>
            </a-space>
          </template>
        </a-drawer>
      </a-tab-pane>

      <a-tab-pane key="permissions" tab="权限配置">
        <a-card :bordered="false">
          <template #title>
            <a-space>
              <span>角色权限矩阵</span>
              <a-tooltip title="配置每个角色对各功能和操作的访问权限">
                <question-circle-outlined />
              </a-tooltip>
            </a-space>
          </template>

          <a-spin :spinning="permLoading">
            <a-table
              :data-source="permTableData"
              :columns="permColumns"
              :pagination="false"
              size="small"
              bordered
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'feature'">
                  <span class="perm-feature-name">{{ record.feature }}</span>
                </template>
                <template v-else-if="column.key === 'action'">
                  <span class="perm-action-name">{{ record.action }}</span>
                </template>
                <template v-else-if="column.key.startsWith('role_')">
                  <div class="perm-cell">
                    <a-switch
                      size="small"
                      :checked="!!permState[column.role]?.[record.key]"
                      @change="(val: boolean) => togglePermission(column.role, record.key, val)"
                    />
                  </div>
                </template>
              </template>
            </a-table>

            <a-divider />

            <div class="perm-actions">
              <a-button type="primary" :loading="permSaving" @click="saveAllPermissions">
                保存所有角色权限
              </a-button>
            </div>
          </a-spin>
        </a-card>
      </a-tab-pane>
    </a-tabs>
  </div>
</template>

<script setup lang="ts">
  import { ref, reactive, computed, onMounted } from 'vue'
  import { message, Modal } from 'ant-design-vue'
  import { PlusOutlined, QuestionCircleOutlined } from '@ant-design/icons-vue'
  import { adminApi } from '@/api/admin'
  import type { UserDTO } from '@/types'

  // ===== 权限配置常量 =====
  const roles = ref<{ value: string; label: string }[]>([
    { value: 'ADMIN', label: '管理员' },
    { value: 'MANAGER', label: '经理' },
    { value: 'USER', label: '普通用户' },
  ])

  const roleLabels = reactive<Record<string, string>>({
    ADMIN: '管理员',
    MANAGER: '经理',
    USER: '普通用户',
  })

  // 从 data-dict 动态加载
  const permDefs = ref<Array<{ key: string; name: string; module: string; moduleName: string; type: string; defaultRoles: string }>>([])
  const permDefsLoading = ref(false)

  // 动态构建权限组
  const permissionGroups = computed(() => {
    const groups: Record<
      string,
      { key: string; label: string; permissions: { key: string; label: string }[] }
    > = {}

    for (const def of permDefs.value) {
      const moduleKey = def.module || def.key?.split(':')[0] || 'other'
      const moduleLabel = def.moduleName || moduleKey
      if (!groups[moduleKey]) {
        groups[moduleKey] = {
          key: moduleKey,
          label: moduleLabel,
          permissions: [],
        }
      }
      groups[moduleKey].permissions.push({ key: def.key, label: def.name })
    }

    return Object.values(groups)
  })

  // ===== 用户权限覆盖状态 =====
  const userPermDrawerVisible = ref(false)
  const userPermDrawerUser = ref<UserDTO | null>(null)
  const userPermLoading = ref(false)
  const userPermSaving = ref(false)
  const userPermState = ref<Record<string, boolean>>({})

  const openUserPermDrawer = async (record: UserDTO) => {
    userPermDrawerUser.value = record
    userPermDrawerVisible.value = true
    userPermLoading.value = true
    try {
      if (permDefs.value.length === 0) {
        await loadPermDefs()
      }
      const perms = await adminApi.getUserPermissions(record.id)
      userPermState.value = { ...perms }
    } catch {
      userPermState.value = {}
      message.error('加载用户权限失败')
    } finally {
      userPermLoading.value = false
    }
  }

  const saveUserPerms = async () => {
    if (!userPermDrawerUser.value) return
    userPermSaving.value = true
    try {
      await adminApi.updateUserPermissions(userPermDrawerUser.value.id, userPermState.value)
      message.success('用户权限覆盖已保存')
      userPermDrawerVisible.value = false
    } catch {
      message.error('保存失败')
    } finally {
      userPermSaving.value = false
    }
  }

  const loadPermDefs = async () => {
    permDefsLoading.value = true
    try {
      permDefs.value = await adminApi.getPermissionDefinitions()
    } catch {
      permDefs.value = []
    } finally {
      permDefsLoading.value = false
    }
  }

  // ===== 用户管理状态 =====
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

  // ===== 权限配置状态 =====
  const activeTab = ref('users')
  const permLoading = ref(false)
  const permSaving = ref(false)
  const permState = ref<Record<string, Record<string, boolean>>>({
    ADMIN: {},
    MANAGER: {},
    USER: {},
  })

  const permColumns = computed(() => [
    { title: '功能', dataIndex: 'feature', key: 'feature', width: 120 },
    { title: '操作', dataIndex: 'action', key: 'action', width: 140 },
    ...roles.value.map((r) => ({
      title: r.label,
      key: `role_${r.value}`,
      role: r.value,
      align: 'center' as const,
      width: 100,
    })),
  ])

  const permTableData = computed(() => {
    const data: { key: string; feature: string; action: string }[] = []
    for (const group of permissionGroups.value) {
      for (const p of group.permissions) {
        data.push({
          key: p.key,
          feature: group.label,
          action: p.label,
        })
      }
    }
    return data
  })

  const loadAllPermissions = async () => {
    permLoading.value = true
    try {
      await loadPermDefs()
      const results = await Promise.all(roles.value.map((r) => adminApi.getPermissions(r.value)))
      roles.value.forEach((r, i) => {
        if (results[i]) permState.value[r.value] = results[i]!
      })
    } catch {
      message.error('加载权限配置失败')
    } finally {
      permLoading.value = false
    }
  }

  const togglePermission = (role: string, key: string, value: boolean) => {
    if (!permState.value[role]) {
      permState.value[role] = {}
    }
    permState.value[role][key] = value
  }

  const buildCleanPerms = (rolePerms: Record<string, boolean>): Record<string, boolean> => {
    const clean: Record<string, boolean> = {}
    for (const def of permDefs.value) {
      clean[def.key] = rolePerms[def.key] ?? false
    }
    return clean
  }

  const saveAllPermissions = async () => {
    permSaving.value = true
    try {
      await Promise.all(
        roles.value.map((r) =>
          adminApi.updatePermissions(r.value, buildCleanPerms(permState.value[r.value] ?? {})),
        ),
      )
    } catch {
      message.error('保存权限失败')
    } finally {
      permSaving.value = false
    }
  }

  const loadRoles = async () => {
    try {
      const roleList = await adminApi.listRoles()
      roles.value = roleList.map((r) => ({ value: r.name, label: r.displayName || r.name }))
      const labels: Record<string, string> = {}
      roleList.forEach((r) => { labels[r.name] = r.displayName || r.name })
      Object.assign(roleLabels, labels)
    } catch {
      // 使用默认角色列表
    }
  }

  const handleTabChange = (key: string) => {
    if (key === 'permissions' && Object.keys(permState.value.ADMIN ?? {}).length === 0) {
      loadAllPermissions()
    }
  }

  // ===== 用户管理方法 =====
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

  onMounted(() => {
    loadRoles()
    loadUsers()
  })
</script>

<style scoped>
  .user-management-page {
    padding: 20px;
    min-height: 100%;
  }

  .search-card {
    margin-bottom: 16px;
  }

  .user-management-page :deep(.ant-tabs-tab) {
    font-size: 15px;
  }

  .user-management-page :deep(.ant-tabs-extra-content) {
    margin-bottom: 8px;
  }

  .search-form {
    width: 100%;
  }

  .search-actions-col {
    display: flex;
    align-items: flex-end;
    padding-bottom: 24px;
  }

  /* ===== 权限配置样式 ===== */
  .perm-cell {
    display: flex;
    justify-content: center;
    align-items: center;
  }

  .perm-feature-name {
    font-weight: 600;
    font-size: 13px;
    color: #1f2937;
  }

  .perm-action-name {
    font-size: 13px;
    color: #374151;
  }

  .perm-actions {
    display: flex;
    justify-content: center;
  }

  [data-theme='dark'] .perm-feature-name {
    color: #e0e0e0;
  }

  [data-theme='dark'] .perm-action-name {
    color: #cccccc;
  }

  /* ===== 用户权限覆盖样式 ===== */
  .user-perm-group {
    margin-bottom: 20px;
  }

  .perm-group-title-small {
    font-size: 14px;
    font-weight: 600;
    color: #1f2937;
    margin-bottom: 8px;
    padding-bottom: 4px;
    border-bottom: 1px solid #f0f0f0;
  }

  .user-perm-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 6px 8px;
    border-radius: 4px;
  }

  .user-perm-item:hover {
    background: #f5f7fa;
  }

  .user-perm-item-label {
    font-size: 13px;
    color: #374151;
  }

  [data-theme='dark'] .perm-group-title-small {
    color: #e0e0e0;
    border-bottom-color: #3c3c3c;
  }

  [data-theme='dark'] .user-perm-item:hover {
    background: #2d2d2d;
  }

  [data-theme='dark'] .user-perm-item-label {
    color: #cccccc;
  }
</style>
