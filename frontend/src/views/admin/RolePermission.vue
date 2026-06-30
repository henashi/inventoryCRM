<template>
  <div class="role-perm-page">
    <a-page-header title="角色权限配置" :sub-title="`${roleName} - 配置该角色对各功能的访问权限`" @back="() => router.push({ name: 'RoleList' })">
    </a-page-header>

    <a-card :bordered="false">
      <a-spin :spinning="loading">
        <template v-if="permDefs.length > 0">
          <div v-for="group in permissionGroups" :key="group.key" class="perm-group">
            <div class="perm-group-title">{{ group.label }}</div>
            <div v-for="p in group.permissions" :key="p.key" class="perm-item">
              <div class="perm-item-label">{{ p.label }}</div>
              <a-switch
                size="small"
                :checked="!!permState[p.key]"
                @change="(val: boolean) => togglePermission(p.key, val)"
              />
            </div>
          </div>

          <a-divider />

          <div class="perm-actions">
            <a-space>
              <a-button @click="router.push({ name: 'RoleList' })">返回</a-button>
              <a-button type="primary" :loading="saving" @click="savePermissions">保存权限</a-button>
            </a-space>
          </div>
        </template>
        <a-empty v-else description="暂无权限定义，请先在配置管理中创建 PERMISSION 分组" />
      </a-spin>
    </a-card>
  </div>
</template>

<script setup lang="ts">
  import { ref, computed, onMounted } from 'vue'
  import { useRoute, useRouter } from 'vue-router'
  import { message } from 'ant-design-vue'
  import { adminApi } from '@/api/admin'

  const route = useRoute()
  const router = useRouter()

  const roleName = (route.params.roleName as string) || ''
  const loading = ref(false)
  const saving = ref(false)
  const permState = ref<Record<string, boolean>>({})
  const permDefs = ref<Array<{ key: string; name: string; module: string; moduleName: string; type: string; defaultRoles: string }>>([])

  const permissionGroups = computed(() => {
    const groups: Record<string, { key: string; label: string; permissions: { key: string; label: string }[] }> = {}
    for (const def of permDefs.value) {
      const moduleKey = def.module || def.key?.split(':')[0] || 'other'
      const moduleLabel = def.moduleName || moduleKey
      if (!groups[moduleKey]) {
        groups[moduleKey] = { key: moduleKey, label: moduleLabel, permissions: [] }
      }
      groups[moduleKey].permissions.push({ key: def.key, label: def.name })
    }
    return Object.values(groups)
  })

  const loadData = async () => {
    loading.value = true
    try {
      const [defs, perms] = await Promise.all([
        adminApi.getPermissionDefinitions(),
        adminApi.getPermissions(roleName),
      ])
      permDefs.value = defs
      permState.value = { ...perms }
    } catch {
      message.error('加载权限数据失败')
    } finally {
      loading.value = false
    }
  }

  const togglePermission = (key: string, value: boolean) => {
    permState.value[key] = value
  }

  const savePermissions = async () => {
    saving.value = true
    try {
      const clean: Record<string, boolean> = {}
      for (const def of permDefs.value) {
        clean[def.key] = permState.value[def.key] ?? false
      }
      await adminApi.updatePermissions(roleName, clean)
      message.success('角色权限已保存')
    } catch {
      message.error('保存失败')
    } finally {
      saving.value = false
    }
  }

  onMounted(() => {
    loadData()
  })
</script>

<style scoped>
  .role-perm-page {
    padding: 20px;
    min-height: 100%;
  }

  .perm-group {
    margin-bottom: 24px;
  }

  .perm-group-title {
    font-size: 15px;
    font-weight: 600;
    color: #1f2937;
    margin-bottom: 10px;
    padding-bottom: 6px;
    border-bottom: 1px solid #f0f0f0;
  }

  .perm-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 8px 12px;
    border-radius: 4px;
  }

  .perm-item:hover {
    background: #f5f7fa;
  }

  .perm-item-label {
    font-size: 14px;
    color: #374151;
  }

  [data-theme='dark'] .perm-group-title {
    color: #e0e0e0;
    border-bottom-color: #3c3c3c;
  }

  [data-theme='dark'] .perm-item:hover {
    background: #2d2d2d;
  }

  [data-theme='dark'] .perm-item-label {
    color: #cccccc;
  }

  .perm-actions {
    display: flex;
    justify-content: center;
  }
</style>
