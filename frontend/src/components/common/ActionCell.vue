<template>
  <a-space>
    <template v-for="act in visibleActions" :key="act.key">
      <a-popconfirm
        v-if="act.confirm"
        :title="act.confirm"
        @confirm="act.handler(record)"
      >
        <a-button type="link" size="small" :danger="act.danger">{{ act.label }}</a-button>
      </a-popconfirm>
      <a-button
        v-else
        type="link"
        size="small"
        :danger="act.danger"
        :loading="act.loading"
        @click="act.handler(record)"
      >
        {{ act.label }}
      </a-button>
    </template>
    <slot />
  </a-space>
</template>

<script setup lang="ts">
  import { computed } from 'vue'
  import { useAuthStore } from '@/stores/auth'

  export interface ActionItem<T = any> {
    /** 权限 key，不传则不校验 */
    key?: string
    /** 按钮文字 */
    label: string
    /** 点击处理函数 */
    handler: (record: T) => void
    /** 危险按钮（红色） */
    danger?: boolean
    /** 确认弹窗文字，有值则点击先弹确认框 */
    confirm?: string
    /** 加载状态 */
    loading?: boolean
  }

  const props = defineProps<{
    record: any
    actions: ActionItem[]
  }>()

  const authStore = useAuthStore()

  const visibleActions = computed(() =>
    props.actions.filter((a) => !a.key || authStore.hasPermission(a.key)),
  )
</script>
