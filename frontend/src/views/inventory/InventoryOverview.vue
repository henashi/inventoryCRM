<template>
  <div class="inventory-page">
    <div class="page-header">
      <div class="page-header-main">
        <h1 class="page-title">库存总览</h1>
        <div class="page-subtitle">统一查看库存快照、预警与出入库操作</div>
      </div>
      <div class="page-header-actions">
        <a-button @click="openAlertsDrawer">低库存预警</a-button>
        <a-button :loading="exportLoading" @click="handleExport">导出快照</a-button>
        <a-button @click="goToLogs">库存日志</a-button>
        <a-button class="header-action-primary" type="primary" @click="openAction('in')">商品入库</a-button>
        <a-button class="header-action-back" @click="goToDashboard">返回仪表盘</a-button>
      </div>
    </div>

    <a-card class="search-card">
      <a-form class="search-form" layout="vertical" @finish="handleSearch">
        <div class="search-grid">
          <div class="search-field search-field-keyword">
            <a-form-item label="关键词">
              <a-input v-model:value="searchForm.keyword" allow-clear placeholder="商品名称 / 编码" />
            </a-form-item>
          </div>
          <div class="search-field search-field-status">
            <a-form-item label="状态">
              <a-select v-model:value="searchForm.status" allow-clear placeholder="全部状态">
                <a-select-option :value="1">正常</a-select-option>
                <a-select-option :value="0">停用</a-select-option>
              </a-select>
            </a-form-item>
          </div>
          <div class="search-field search-field-stock-range">
            <a-form-item label="库存范围">
              <a-space-compact class="stock-range-inputs">
                <a-input-number v-model:value="searchForm.minStock" :min="0" class="stock-range-input" placeholder="最小" />
                <a-input-number v-model:value="searchForm.maxStock" :min="0" class="stock-range-input" placeholder="最大" />
              </a-space-compact>
            </a-form-item>
          </div>
          <div class="search-field search-field-checkbox">
            <a-form-item label="筛选项">
              <a-checkbox v-model:checked="searchForm.lowStockOnly">仅看低库存</a-checkbox>
            </a-form-item>
          </div>
          <div class="search-actions">
            <a-button type="primary" html-type="submit">搜索</a-button>
            <a-button @click="handleReset">重置</a-button>
          </div>
        </div>
      </a-form>
    </a-card>

    <a-card>
      <a-table
        row-key="id"
        :loading="inventoryStore.isLoading"
        :columns="columns"
        :data-source="inventoryStore.inventories"
        :pagination="pagination"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'productName'">
            <div class="product-cell">
              <div class="product-name">{{ record.productName }}</div>
              <div class="product-code">{{ record.productCode }}</div>
            </div>
          </template>

          <template v-else-if="column.dataIndex === 'currentStock'">
            <a-tag :color="getStockColor(record)">{{ record.currentStock }} {{ record.unit }}</a-tag>
          </template>

          <template v-else-if="column.dataIndex === 'status'">
            <a-tag :color="record.status === 1 ? 'green' : 'default'">
              {{ record.status === 1 ? '正常' : '停用' }}
            </a-tag>
          </template>

          <template v-else-if="column.dataIndex === 'alert'">
            <a-tag v-if="record.lowStock || record.outOfStock" color="red">{{ record.alertReason || '低库存' }}</a-tag>
            <span v-else>正常</span>
          </template>

          <template v-else-if="column.dataIndex === 'lastUpdateTime'">
            {{ formatDateTime(record.lastUpdateTime) }}
          </template>

          <template v-else-if="column.dataIndex === 'actions'">
            <a-space>
              <a-button type="link" size="small" @click="goToDetail(record)">详情</a-button>
              <a-button type="link" size="small" @click="openAction('in', record)">入库</a-button>
              <a-button type="link" size="small" @click="openAction('out', record)">出库</a-button>
              <a-button type="link" size="small" @click="openAction('adjust', record)">调整</a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-drawer v-model:open="alertsDrawerVisible" title="低库存预警" width="420">
      <a-list :data-source="inventoryStore.alerts" :loading="alertsLoading">
        <template #renderItem="{ item }">
          <a-list-item>
            <a-list-item-meta :description="item.alertReason || '低库存'">
              <template #title>
                <div class="alert-title">
                  <span>{{ item.productName }}</span>
                  <a-tag color="red">{{ item.currentStock }} {{ item.unit }}</a-tag>
                </div>
              </template>
            </a-list-item-meta>
            <template #actions>
              <a-button type="link" size="small" @click="goToDetail(item)">详情</a-button>
              <a-button type="link" size="small" @click="openAction('in', item)">补货</a-button>
            </template>
          </a-list-item>
        </template>
      </a-list>
    </a-drawer>

    <InventoryActionModal
      :open="actionModalVisible"
      :mode="actionMode"
      :inventory="selectedInventory"
      :inventory-options="inventoryStore.selectableInventories"
      :loading="inventoryStore.actionLoading || inventoryStore.selectableLoading"
      @cancel="closeActionModal"
      @submit="handleActionSubmit"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import InventoryActionModal from '@/components/inventory/InventoryActionModal.vue'
import { useInventoryStore } from '@/stores/inventory'
import type { Inventory, InventoryAdjustDTO, InventoryInDTO, InventoryOutDTO } from '@/types'

const inventoryStore = useInventoryStore()
const router = useRouter()
const route = useRoute()

const exportLoading = ref(false)
const alertsLoading = ref(false)
const alertsDrawerVisible = ref(false)
const actionModalVisible = ref(false)
const actionMode = ref<'in' | 'out' | 'adjust' | null>(null)
const selectedInventory = ref<Inventory | null>(null)
const searchForm = reactive({
  keyword: '',
  status: undefined as 0 | 1 | undefined,
  minStock: undefined as number | undefined,
  maxStock: undefined as number | undefined,
  lowStockOnly: false,
})

const columns = [
  { title: '商品', dataIndex: 'productName', key: 'productName', width: 220 },
  { title: '当前库存', dataIndex: 'currentStock', key: 'currentStock', width: 120 },
  { title: '安全库存', dataIndex: 'safeStock', key: 'safeStock', width: 120 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '预警', dataIndex: 'alert', key: 'alert', width: 220 },
  { title: '最近变更', dataIndex: 'lastUpdateTime', key: 'lastUpdateTime', width: 180 },
  { title: '操作', dataIndex: 'actions', key: 'actions', width: 220, fixed: 'right' },
]

const pagination = computed(() => ({
  current: inventoryStore.pagination.page,
  pageSize: inventoryStore.pagination.size,
  total: inventoryStore.pagination.total,
  pageSizeOptions: ['5', '10', '20'],
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条`,
}))

const getQueryParams = (page = 0, size = inventoryStore.pagination.size) => ({
  page,
  size,
  keyword: searchForm.keyword || undefined,
  status: searchForm.status,
  minStock: searchForm.minStock,
  maxStock: searchForm.maxStock,
  lowStockOnly: searchForm.lowStockOnly || undefined,
})

const loadInventories = async (page = 0, size = inventoryStore.pagination.size) => {
  try {
    await inventoryStore.loadInventories(getQueryParams(page, size))
  } catch {
    message.error('加载库存列表失败')
  }
}

const loadAlerts = async () => {
  try {
    alertsLoading.value = true
    await inventoryStore.loadAlerts()
  } catch {
    message.error('加载预警列表失败')
  } finally {
    alertsLoading.value = false
  }
}

const ensureSelectableInventories = async () => {
  if (inventoryStore.selectableInventories.length > 0) {
    return
  }

  try {
    await inventoryStore.loadSelectableInventories()
  } catch {
    message.error('加载商品选项失败')
    throw new Error('selectable inventories load failed')
  }
}

const clearActionQuery = async () => {
  const nextQuery = { ...route.query }
  delete nextQuery.action
  delete nextQuery.productId
  await router.replace({ query: nextQuery })
}

const resolveRouteIntent = async () => {
  const action = route.query.action
  if (action !== 'in' && action !== 'out' && action !== 'adjust') {
    return
  }

  const productId = Number(route.query.productId)
  if (productId) {
    try {
      const inventory = await inventoryStore.loadInventory(productId)
      selectedInventory.value = inventory
    } catch {
      message.error('加载库存详情失败，无法直接打开操作')
      await clearActionQuery()
      return
    }
  } else {
    selectedInventory.value = null
    try {
      await ensureSelectableInventories()
    } catch {
      await clearActionQuery()
      return
    }
  }

  actionMode.value = action
  actionModalVisible.value = true
}

const handleSearch = async () => {
  await loadInventories(0, inventoryStore.pagination.size)
}

const handleReset = async () => {
  searchForm.keyword = ''
  searchForm.status = undefined
  searchForm.minStock = undefined
  searchForm.maxStock = undefined
  searchForm.lowStockOnly = false
  await loadInventories(0, inventoryStore.pagination.size)
}

const handleTableChange = async (pag: { current?: number; pageSize?: number }) => {
  await loadInventories((pag.current || 1) - 1, pag.pageSize || inventoryStore.pagination.size)
}

const handleExport = async () => {
  try {
    exportLoading.value = true
    await inventoryStore.exportInventories(getQueryParams(inventoryStore.pagination.page - 1, inventoryStore.pagination.size))
    message.success('库存快照导出成功')
  } catch {
    message.error('库存快照导出失败')
  } finally {
    exportLoading.value = false
  }
}

const openAlertsDrawer = async () => {
  alertsDrawerVisible.value = true
  await loadAlerts()
}

const goToDashboard = () => {
  router.push('/dashboard')
}

const goToLogs = () => {
  router.push('/inventory/logs')
}

const goToDetail = (inventory: Inventory) => {
  router.push(`/inventory/${inventory.id || inventory.productId}`)
}

const openAction = async (mode: 'in' | 'out' | 'adjust', inventory?: Inventory | null) => {
  if (!inventory) {
    try {
      await ensureSelectableInventories()
    } catch {
      return
    }
  }

  actionMode.value = mode
  selectedInventory.value = inventory || null
  actionModalVisible.value = true
}

const closeActionModal = async () => {
  actionModalVisible.value = false
  actionMode.value = null
  selectedInventory.value = null
  if (route.query.action) {
    await clearActionQuery()
  }
}

const handleActionSubmit = async (payload: { mode: 'in' | 'out' | 'adjust'; productId: number; data: InventoryInDTO | InventoryOutDTO | InventoryAdjustDTO }) => {
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
    await Promise.all([
      loadInventories(inventoryStore.pagination.page - 1, inventoryStore.pagination.size),
      loadAlerts(),
      inventoryStore.loadSelectableInventories(),
    ])
  } catch {
    message.error('库存操作失败')
  }
}

const getStockColor = (inventory: Inventory) => {
  if (inventory.outOfStock) return 'volcano'
  if (inventory.lowStock) return 'orange'
  return 'green'
}

const formatDateTime = (value?: string) => value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '--'

onMounted(async () => {
  await loadInventories(0, inventoryStore.pagination.size)
  await resolveRouteIntent()
})

watch(
  () => [route.query.action, route.query.productId],
  async ([nextAction, nextProductId], [prevAction, prevProductId]) => {
    if (nextAction === prevAction && nextProductId === prevProductId) {
      return
    }

    await resolveRouteIntent()
  },
)
</script>

<style scoped>
.inventory-page {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  margin-bottom: 20px;
  gap: 16px;
}

.page-header-main {
  flex: 1 1 260px;
  min-width: 0;
}

.page-header-actions {
  display: flex;
  flex: 1 1 560px;
  justify-content: flex-end;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.header-action-primary {
  margin-left: 8px;
}

.header-action-back {
  margin-left: 0;
}

.page-title {
  margin: 0;
}

.page-subtitle {
  color: #8c8c8c;
  margin-top: 4px;
}

.search-card {
  margin-bottom: 16px;
}

.search-form :deep(.ant-form-item) {
  margin-bottom: 0;
}

.search-grid {
  display: grid;
  grid-template-columns: minmax(240px, 2.2fr) minmax(180px, 1.2fr) minmax(260px, 1.6fr) minmax(160px, auto) auto;
  gap: 16px;
  align-items: end;
}

.search-field {
  min-width: 0;
}

.search-field :deep(.ant-select),
.search-field :deep(.ant-input),
.search-field :deep(.ant-input-number),
.stock-range-inputs {
  width: 100%;
}

.stock-range-input {
  width: 50%;
}

.search-actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
}

.product-cell {
  display: flex;
  flex-direction: column;
}

.product-name {
  font-weight: 500;
}

.product-code {
  color: #8c8c8c;
  font-size: 12px;
}

.alert-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

@media (max-width: 1200px) {
  .page-header-actions {
    flex-basis: 100%;
  }

  .search-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .search-field-stock-range {
    grid-column: span 2;
  }

  .search-actions {
    grid-column: 1 / -1;
  }
}

@media (max-width: 768px) {
  .inventory-page {
    padding: 16px;
  }

  .page-header-actions {
    width: 100%;
    justify-content: flex-start;
    gap: 8px;
  }

  .page-header-actions > * {
    flex: 0 1 auto;
  }

  .header-action-primary {
    margin-left: 0;
  }

  .search-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .search-field-stock-range,
  .search-actions {
    grid-column: auto;
  }

  .search-actions {
    justify-content: stretch;
  }

  .search-actions > * {
    flex: 1 1 0;
  }
}
</style>
