<template>
  <div class="inventory-detail-page">
    <div class="page-header">
      <div>
        <div class="page-subtitle">查看库存状态、预警原因与最近变更记录</div>
      </div>
      <a-space>
        <a-button @click="goBack">返回</a-button>
        <a-button @click="goToLogs">查看日志</a-button>
        <a-button type="primary" @click="openAction('in')">入库</a-button>
        <a-button @click="openAction('out')">出库</a-button>
        <a-button @click="openAction('adjust')">调整</a-button>
      </a-space>
    </div>

    <a-row :gutter="16">
      <a-col :xs="24" :xl="14">
        <a-card :loading="inventoryStore.isLoading" title="基础信息">
          <a-descriptions v-if="inventory" :column="2" bordered>
            <a-descriptions-item label="商品名称">{{ inventory.productName }}</a-descriptions-item>
            <a-descriptions-item label="商品编码">{{ inventory.productCode }}</a-descriptions-item>
            <a-descriptions-item label="当前库存"
              >{{ inventory.currentStock }} {{ inventory.unit }}</a-descriptions-item
            >
            <a-descriptions-item label="安全库存"
              >{{ inventory.safeStock }} {{ inventory.unit }}</a-descriptions-item
            >
            <a-descriptions-item label="最大库存"
              >{{ inventory.maxStock }} {{ inventory.unit }}</a-descriptions-item
            >
            <a-descriptions-item label="状态">
              <a-tag :color="inventory.status === 1 ? 'green' : 'default'">
                {{ inventory.status === 1 ? '正常' : '停用' }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="最近更新时间" :span="2">{{
              formatDateTime(inventory.lastUpdateTime)
            }}</a-descriptions-item>
          </a-descriptions>
        </a-card>
      </a-col>

      <a-col :xs="24" :xl="10">
        <a-card :loading="inventoryStore.isLoading" title="预警状态">
          <div v-if="inventory" class="alert-card">
            <a-alert
              :type="inventory.lowStock || inventory.outOfStock ? 'warning' : 'success'"
              :message="alertTitle"
              :description="inventory.alertReason || '当前库存处于安全范围内'"
              show-icon
            />
            <div class="alert-stats">
              <a-statistic title="当前库存" :value="inventory.currentStock" />
              <a-statistic title="安全库存" :value="inventory.safeStock" />
            </div>
          </div>
        </a-card>
      </a-col>
    </a-row>

    <a-card class="changes-card" title="最近变更记录">
      <a-table
        row-key="id"
        :loading="inventoryStore.isLoading"
        :columns="changeColumns"
        :data-source="inventory?.recentChanges || []"
        :pagination="false"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'changeType'">
            <a-tag :color="changeTypeColors[normChangeType(record.changeType)] || 'default'">{{
              getChangeTypeLabel(record.changeType)
            }}</a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'changeQuantity'">
            <span
              :class="
                record.changeType === 'out' || record.changeType === 'OUT' ? 'negative' : 'positive'
              "
            >
              {{
                record.changeType === 'out' || record.changeType === 'OUT'
                  ? `-${record.changeQuantity}`
                  : `+${record.changeQuantity}`
              }}
            </span>
          </template>
          <template v-else-if="column.dataIndex === 'createdAt'">
            {{ formatDateTime(record.createdAt) }}
          </template>
        </template>
      </a-table>
    </a-card>

    <InventoryActionModal
      :open="actionModalVisible"
      :mode="actionMode"
      :inventory="inventory"
      :inventory-options="inventory ? [inventory] : []"
      :loading="inventoryStore.actionLoading"
      @cancel="closeActionModal"
      @submit="handleActionSubmit"
    />
  </div>
</template>

<script setup lang="ts">
  import { computed, onMounted, ref, watch } from 'vue'
  import { useRoute, useRouter } from 'vue-router'
  import { message } from 'ant-design-vue'
  import dayjs from 'dayjs'
  import InventoryActionModal from '@/components/inventory/InventoryActionModal.vue'
  import { useInventoryStore } from '@/stores/inventory'
  import type { InventoryAdjustDTO, InventoryInDTO, InventoryOutDTO } from '@/types'

  const inventoryStore = useInventoryStore()
  const route = useRoute()
  const router = useRouter()

  const actionModalVisible = ref(false)
  const actionMode = ref<'in' | 'out' | 'adjust' | null>(null)

  const inventory = computed(() => inventoryStore.currentInventory)
  const inventoryId = computed(() => Number(route.params.id))
  const alertTitle = computed(() => {
    if (!inventory.value) return '库存状态'
    if (inventory.value.outOfStock) return '当前库存已缺货'
    if (inventory.value.lowStock) return '当前库存低于安全库存'
    return '当前库存正常'
  })

  const changeColumns = [
    { title: '时间', dataIndex: 'createdAt', key: 'createdAt', width: 180 },
    { title: '类型', dataIndex: 'changeType', key: 'changeType', width: 120 },
    { title: '变更数量', dataIndex: 'changeQuantity', key: 'changeQuantity', width: 120 },
    {
      title: '库存变化',
      dataIndex: 'afterQuantity',
      key: 'afterQuantity',
      customRender: ({ record }: { record: { beforeQuantity: number; afterQuantity: number } }) =>
        `${record.beforeQuantity} → ${record.afterQuantity}`,
    },
    { title: '原因', dataIndex: 'reason', key: 'reason' },
    { title: '操作人', dataIndex: 'operator', key: 'operator', width: 120 },
  ]

  const loadInventoryDetail = async () => {
    try {
      await inventoryStore.loadInventory(inventoryId.value)
    } catch {
      message.error('加载库存详情失败')
    }
  }

  const clearActionQuery = async () => {
    const nextQuery = { ...route.query }
    delete nextQuery.action
    await router.replace({ query: nextQuery })
  }

  const resolveActionIntent = () => {
    const action = route.query.action
    if (action === 'in' || action === 'out' || action === 'adjust') {
      actionMode.value = action
      actionModalVisible.value = true
    }
  }

  const goBack = () => {
    router.back()
  }

  const goToLogs = () => {
    router.push({ path: '/inventory/logs', query: { productId: inventoryId.value } })
  }

  const openAction = (mode: 'in' | 'out' | 'adjust') => {
    actionMode.value = mode
    actionModalVisible.value = true
  }

  const closeActionModal = async () => {
    actionModalVisible.value = false
    actionMode.value = null
    if (route.query.action) {
      await clearActionQuery()
    }
  }

  const handleActionSubmit = async (payload: {
    mode: 'in' | 'out' | 'adjust'
    productId: number
    data: InventoryInDTO | InventoryOutDTO | InventoryAdjustDTO
  }) => {
    try {
      if (payload.mode === 'in') {
        await inventoryStore.stockIn(payload.data as InventoryInDTO)
        message.success('入库成功')
      }

      if (payload.mode === 'out') {
        await inventoryStore.stockOut(payload.data as InventoryOutDTO)
        message.success('出库成功')
      }

      if (payload.mode === 'adjust') {
        await inventoryStore.adjustStock(payload.productId, payload.data as InventoryAdjustDTO)
        message.success('库存调整成功')
      }

      await closeActionModal()
      await loadInventoryDetail()
    } catch {
      message.error('库存操作失败')
    }
  }

  const changeTypeColors: Record<string, string> = {
    IN: '#58a6ff',
    OUT: '#a371f7',
    ADJUST: '#d29922',
    CREATE: '#58a6ff',
  }

  const normChangeType = (type: string) => type?.toUpperCase() || ''

  const getChangeTypeLabel = (type: string) => {
    const labels: Record<string, string> = {
      IN: '入库',
      OUT: '出库',
      ADJUST: '调整',
      CREATE: '新建',
    }

    return labels[normChangeType(type)] || type
  }

  const getChangeTypeColor = (type: string) => {
    const isDark = document.documentElement.getAttribute('data-theme') === 'dark'
    const colors: Record<string, string> = {
      IN: 'green',
      OUT: isDark ? '#7c3aed' : 'purple',
      ADJUST: 'orange',
      CREATE: 'blue',
    }

    return colors[normChangeType(type)] || 'default'
  }

  const formatDateTime = (value?: string) =>
    value ? dayjs(value).format('YYYY-MM-DD HH:mm:ss') : '--'

  onMounted(async () => {
    await loadInventoryDetail()
    resolveActionIntent()
  })

  watch(
    () => route.params.id,
    async () => {
      await loadInventoryDetail()
    },
  )

  watch(
    () => route.query.action,
    () => {
      resolveActionIntent()
    },
  )
</script>

<style scoped>
  .inventory-detail-page {
    padding: 20px;
  }

  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    gap: 16px;
  }

  .page-title {
    margin: 0;
  }

  .page-subtitle {
    color: #8c8c8c;
    margin-top: 4px;
  }

  .alert-card {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  .alert-stats {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 16px;
  }

  .changes-card {
    margin-top: 16px;
  }

  .positive {
    color: #58a6ff;
  }

  .negative {
    color: #a371f7;
  }

  /* ===== 暗色模式 ===== */
  [data-theme='dark'] .positive {
    color: #58a6ff !important;
  }
  [data-theme='dark'] .negative {
    color: #a371f7 !important;
  }
  [data-theme='dark'] .ant-tag-has-color {
    color: #e0e0e0 !important;
  }
</style>
