<!-- frontend/src/views/inventory/InventoryLogList.vue -->
<template>
  <div class="inventory-log-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1 class="page-title">库存操作日志</h1>
      <div class="page-actions">
        <a-button @click="handleRefresh" :loading="isLoading">
          <reload-outlined />
          刷新
        </a-button>
        <a-button @click="handleExport" :loading="exportLoading" style="margin-left:8px">
          <export-outlined />
          导出
        </a-button>
        <a-button @click="handleBack" style="margin-left:8px">
          <template #icon>
            <home-outlined />
          </template>
          返回仪表盘
        </a-button>
      </div>
    </div>

    <!-- 搜索和筛选区域
    <a-card class="search-card">
      <a-form layout="inline" :model="searchForm" @finish="handleSearch">
        <a-row :gutter="[16, 16]" style="width: 100%">
          <a-col :xs="24" :sm="12" :md="6">
            <a-form-item label="商品">
              <a-select
                v-model:value="searchForm.productId"
                placeholder="请选择商品"
                allow-clear
                show-search
                :filter-option="filterProductOption"
                style="width: 100%"
              >
                <a-select-option
                  v-for="product in productOptions"
                  :key="product.id"
                  :value="product.id"
                >
                  {{ product.name }} ({{ product.code }})
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>

          <a-col :xs="24" :sm="12" :md="6">
            <a-form-item label="操作类型">
              <a-select
                v-model:value="searchForm.type"
                placeholder="请选择操作类型"
                allow-clear
                style="width: 100%"
              >
                <a-select-option value="CREATE">新建商品</a-select-option>
                <a-select-option value="IN">入库</a-select-option>
                <a-select-option value="OUT">出库</a-select-option>
                <a-select-option value="ADJUST">调整</a-select-option>
                <a-select-option value="TRANSFER">调拨</a-select-option>
                <a-select-option value="CHECK">盘点</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>

          <a-col :xs="24" :sm="12" :md="6">
            <a-form-item label="操作人">
              <a-input
                v-model:value="searchForm.operator"
                placeholder="请输入操作人"
                allow-clear
              />
            </a-form-item>
          </a-col>

          <a-col :xs="24" :sm="12" :md="6">
            <a-form-item label="操作时间">
              <a-range-picker
                v-model:value="searchForm.dateRange"
                style="width: 100%"
                :placeholder="['开始时间', '结束时间']"
                show-time
                format="YYYY-MM-DD HH:mm"
                value-format="YYYY-MM-DD HH:mm"
              />
            </a-form-item>
          </a-col>
        </a-row>

        <a-row style="width: 100%; margin-top: 8px">
          <a-col :xs="24" style="display: flex; justify-content: flex-end">
            <a-space>
              <a-button type="primary" html-type="submit" :loading="isLoading">
                搜索
              </a-button>
              <a-button @click="handleReset">
                重置
              </a-button>
              <a-button @click="handleAdvancedSearch" type="link">
                高级搜索
              </a-button>
            </a-space>
          </a-col>
        </a-row>
      </a-form>
    </a-card> -->
    <!-- 搜索栏 -->
    <a-card class="search-card" :bordered="true" size="small">
      <a-form
        :model="searchForm"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 18 }"
        @submit.prevent="handleSearch"
      >
        <a-row :gutter="16">
          <!-- 商品选择 -->
          <a-col :xs="24" :sm="12" :md="8" :lg="6" :xl="6">
            <a-form-item label="商品" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }">
              <a-select
                v-model:value="searchForm.productId"
                placeholder="请选择商品"
                allow-clear
                show-search
                :filter-option="filterProductOption"
              >
                <a-select-option v-for="product in productOptions" :key="product.id" :value="product.id">
                  {{ product.name }} ({{ product.code }})
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>

          <!-- 操作类型 -->
          <a-col :xs="24" :sm="12" :md="8" :lg="6" :xl="6">
            <a-form-item label="操作类型" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }">
              <a-select
                v-model:value="searchForm.type"
                placeholder="请选择操作类型"
                allow-clear
              >
                <a-select-option value="CREATE">新建商品</a-select-option>
                <a-select-option value="IN">入库</a-select-option>
                <a-select-option value="OUT">出库</a-select-option>
                <a-select-option value="ADJUST">调整</a-select-option>
                <a-select-option value="TRANSFER">调拨</a-select-option>
                <a-select-option value="CHECK">盘点</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>

          <!-- 操作时间 -->
          <a-col :xs="24" :sm="12" :md="16" :lg="12" :xl="12">
            <a-form-item label="操作时间" :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }">
              <a-range-picker
                v-model:value="searchForm.dateRange"
                :placeholder="['开始时间', '结束时间']"
                style="width: 100%"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
              />
            </a-form-item>
          </a-col>

          <!-- 操作人 -->
          <a-col :xs="24" :sm="12" :md="8" :lg="6" :xl="6">
            <a-form-item label="操作人" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }">
              <a-input
                v-model:value="searchForm.operator"
                placeholder="请输入操作人"
                allow-clear
                @keyup.enter="handleSearch"
              >
                <template #prefix>
                  <user-outlined style="color: rgba(0, 0, 0, 0.25)" />
                </template>
              </a-input>
            </a-form-item>
          </a-col>
        </a-row>

        <div class="search-actions" style="display: flex; justify-content: flex-end">
          <a-space :size="12">
            <a-button
              type="primary"
              html-type="submit"
              :loading="isLoading"
              @click="handleSearch"
            >
              <template #icon>
                <search-outlined />
              </template>
              搜索
            </a-button>
            <a-button
              @click="handleReset"
            >
              <template #icon>
                <reload-outlined />
              </template>
              重置
            </a-button>
          </a-space>
        </div>
      </a-form>
    </a-card>
    <!-- 统计卡片 -->
    <a-row :gutter="16" class="stats-cards">
      <a-col :xs="12" :sm="6" class="text-center">
        <a-statistic
          title="入库次数"
          :value="stats.inCount"
          :value-style="{ color: '#3f8600' }"
        >
          <template #prefix>
            <arrow-up-outlined />
          </template>
        </a-statistic>
      </a-col>
      <a-col :xs="12" :sm="6" class="text-center">
        <a-statistic
          title="出库次数"
          :value="stats.outCount"
          :value-style="{ color: '#d3adf7' }"
        >
          <template #prefix>
            <arrow-down-outlined />
          </template>
        </a-statistic>
      </a-col>
      <a-col :xs="12" :sm="6" class="text-center">
        <a-statistic
          title="入库总量"
          :value="stats.inQuantity"
          :value-style="{ color: '#3f8600' }"
        />
      </a-col>
      <a-col :xs="12" :sm="6" class="text-center">
        <a-statistic
          title="出库总量"
          :value="stats.outQuantity"
          :value-style="{ color: '#d3adf7' }"
        />
      </a-col>
    </a-row>

    <!-- 库存日志表格 -->
    <a-card class="table-card">
      <a-table
        :columns="columns"
        :data-source="inventoryLogs"
        :loading="isLoading"
        :pagination="pagination"
        :row-key="record => record.id"
        @change="handleTableChange"
        :scroll="{ x: 1000 }"
      >
        <!-- 操作时间 -->
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'createdAt'">
            <div class="time-cell">
              <div class="date">{{ formatDate(record.createdAt, 'MM-DD') }}</div>
              <div class="time">{{ formatDate(record.createdAt, 'HH:mm:ss') }}</div>
            </div>
          </template>

          <!-- 商品信息 -->
          <template v-else-if="column.dataIndex === 'productInfo'">
            <div class="product-info">
              <div class="product-name">
                <a-tag :color="getOperationTypeColor(record.type)" size="small">
                  {{ getSimplifyTypeText(record.type) }}
                </a-tag>
                {{ record.productName }}
              </div>
              <div class="product-code">{{ record.productCode }}</div>
            </div>
          </template>

          <!-- 操作类型 -->
          <template v-else-if="column.dataIndex === 'type'">
            <a-tag :color="getOperationTypeColor(record.type)">
              {{ getOperationTypeText(record.type) }}
            </a-tag>
          </template>

          <!-- 库存变化 -->
          <template v-else-if="column.dataIndex === 'stockChange'">
            <div class="stock-change">
              <div class="change-quantity">
                <span :class="getChangeClass(record.afterStock - record.beforeStock)">
                  {{ formatChangeQuantity(record.afterStock - record.beforeStock) }}
                </span>
                <span class="unit">{{ record.productUnit }}</span>
              </div>
              <div class="stock-range">
                {{ record.beforeStock }} → {{ record.afterStock }}
              </div>
            </div>
          </template>

          <!-- 操作人 -->
          <template v-else-if="column.dataIndex === 'operator'">
            <div class="operator-info">
              <a-avatar :size="24" style="background-color: #1890ff">
                {{ getFirstChar(record.operator) }}
              </a-avatar>
              <span class="operator-name">{{ record.operator }}</span>
            </div>
          </template>

          <!-- 原因 -->
          <template v-else-if="column.dataIndex === 'reason'">
            <div class="reason-cell" :title="record.reason">
              {{ record.reason }}
            </div>
          </template>

          <!-- 操作状态 -->
          <!-- <template v-else-if="column.dataIndex === 'success'">
            <a-tag :color="record.success ? 'green' : 'red'">
              {{ record.success ? '成功' : '失败' }}
            </a-tag>
            <div v-if="!record.success && record.errorMessage" class="error-msg">
              {{ record.errorMessage }}
            </div>
          </template> -->

          <!-- 操作 -->
          <template v-else-if="column.dataIndex === 'actions'">
            <a-space size="small">
              <a-button
                type="link"
                size="small"
                @click="handleViewDetail(record)"
              >
                详情
              </a-button>
              <a-button
                v-if="record.type === 'CREATE'"
                type="link"
                size="small"
                @click="handleViewProduct(record.productId)"
              >
                查看商品
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 日志详情抽屉 -->
    <a-drawer
      v-model:open="detailVisible"
      title="操作日志详情"
      width="500"
      placement="right"
    >
      <template v-if="currentLog">
        <div class="log-detail">
          <!-- 基本信息 -->
          <a-descriptions title="基本信息" bordered size="small">
            <a-descriptions-item label="操作ID" span="3">
              {{ currentLog.id }}
            </a-descriptions-item>
            <a-descriptions-item label="操作时间" span="3">
              {{ formatDateTime(currentLog.createdAt) }}
            </a-descriptions-item>
            <a-descriptions-item label="操作人" span="3">
              {{ currentLog.operator }}
            </a-descriptions-item>
            <a-descriptions-item label="操作类型" span="3">
              <a-tag :color="getOperationTypeColor(currentLog.type)">
                {{ getOperationTypeText(currentLog.type) }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="操作状态" span="3">
              <a-tag :color="currentLog.success ? 'green' : 'red'">
                {{ currentLog.success ? '成功' : '失败' }}
              </a-tag>
            </a-descriptions-item>
          </a-descriptions>

          <!-- 商品信息 -->
          <a-descriptions title="商品信息" bordered size="small" style="margin-top: 16px">
            <a-descriptions-item label="商品名称" span="3">
              {{ currentLog.productName }}
            </a-descriptions-item>
            <a-descriptions-item label="商品编码" span="3">
              {{ currentLog.productCode }}
            </a-descriptions-item>
            <a-descriptions-item label="商品单位" span="3">
              {{ currentLog.productUnit }}
            </a-descriptions-item>
          </a-descriptions>

          <!-- 库存变化 -->
          <a-descriptions title="库存变化" bordered size="small" style="margin-top: 16px">
            <a-descriptions-item label="变更前库存" span="3">
              {{ currentLog.beforeStock }} {{ currentLog.productUnit }}
            </a-descriptions-item>
            <a-descriptions-item label="变更数量" span="3">
              <span :class="getChangeClass(currentLog.afterStock - currentLog.beforeStock)">
                {{ formatChangeQuantity(currentLog.afterStock - currentLog.beforeStock) }}
              </span>
              {{ currentLog.productUnit }}
            </a-descriptions-item>
            <a-descriptions-item label="变更后库存" span="3">
              {{ currentLog.afterStock }} {{ currentLog.productUnit }}
            </a-descriptions-item>
          </a-descriptions>

          <!-- 其他信息 -->
          <a-descriptions title="其他信息" bordered size="small" style="margin-top: 16px">
            <a-descriptions-item label="操作原因" span="3">
              {{ currentLog.reason || '无' }}
            </a-descriptions-item>
            <a-descriptions-item v-if="!currentLog.success && currentLog.errorMessage" label="错误信息" span="3">
              <div style="color: #ff4d4f;">
                {{ currentLog.errorMessage }}
              </div>
            </a-descriptions-item>
            <a-descriptions-item label="创建时间" span="3">
              {{ formatDateTime(currentLog.createdAt) }}
            </a-descriptions-item>
          </a-descriptions>
        </div>
      </template>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  ReloadOutlined,
  ExportOutlined,
  ArrowUpOutlined,
  ArrowDownOutlined
} from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import { useInventoryLogStore } from '@/stores/inventoryLog'
import { useProductStore } from '@/stores/product'
import type { InventoryLogRecord, PageParams } from '@/types'

const router = useRouter()
const inventoryLogStore = useInventoryLogStore()
const productStore = useProductStore()

// 状态
const isLoading = ref(false)
const exportLoading = ref(false)
const detailVisible = ref(false)
const currentLog = ref<InventoryLogRecord | null>(null)

// 搜索表单
const searchForm = reactive({
  productId: undefined as number | undefined,
  type: undefined as string | undefined,
  operator: '',
  dateRange: [] as any[]
})

// 表格列定义
const columns = [
  {
    title: '操作时间',
    dataIndex: 'createdTime',
    key: 'createdTime',
    width: 100,
    sorter: true
  },
  {
    title: '商品信息',
    dataIndex: 'productInfo',
    key: 'productInfo',
    width: 200
  },
  {
    title: '操作类型',
    dataIndex: 'type',
    key: 'type',
    width: 100
  },
  {
    title: '库存变化',
    dataIndex: 'stockChange',
    key: 'stockChange',
    width: 150
  },
  {
    title: '操作人',
    dataIndex: 'operator',
    key: 'operator',
    width: 120
  },
  {
    title: '原因',
    dataIndex: 'reason',
    key: 'reason',
    width: 200
  },
  // {
  //   title: '状态',
  //   dataIndex: 'success',
  //   key: 'success',
  //   width: 80
  // },
  {
    title: '操作',
    dataIndex: 'actions',
    key: 'actions',
    width: 100,
    fixed: 'right'
  }
]

// 计算属性
const inventoryLogs = computed(() => inventoryLogStore.logs)
const productOptions = computed(() => productStore.products.map(p => ({
  id: p.id,
  name: p.name,
  code: p.code
})))

const pagination = computed(() => ({
  current: inventoryLogStore.pagination.page,
  pageSize: inventoryLogStore.pagination.size,
  total: inventoryLogStore.pagination.total,
  pageSizeOptions: ['5', '10', '20'], // 可选的每页条数
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条记录`
}))

const stats = computed(() => ({
  inCount: inventoryLogStore.stats.inCount,
  outCount: inventoryLogStore.stats.outCount,
  inQuantity: inventoryLogStore.stats.inQuantity,
  outQuantity: inventoryLogStore.stats.outQuantity
}))

const handleBack = () => {
  router.push('/')
}

// 方法
const filterProductOption = (input: string, option: any) => {
  return (
    option.children.toLowerCase().indexOf(input.toLowerCase()) >= 0
  )
}

const formatDate = (dateStr: string, format = 'YYYY-MM-dd') => {
  if (!dateStr) return ''
  return dayjs(dateStr).format(format)
}

const formatDateTime = (dateStr: string) => {
  return formatDate(dateStr, 'YYYY-MM-DD HH:mm:ss')
}

const getFirstChar = (str: string) => {
  if (!str) return '?'
  return str.charAt(0).toUpperCase()
}

const getOperationTypeColor = (type: string) => {
  const colors: Record<string, string> = {
    CREATE: 'blue',
    IN: 'green',
    OUT: 'purple',
    ADJUST: 'orange',
    TRANSFER: 'pink',
    CHECK: 'cyan'
  }
  return colors[type] || 'default'
}

const getSimplifyTypeText = (type: string) => {
  const texts: Record<string, string> = {
    CREATE: '新建',
    IN: '入库',
    OUT: '出库',
    ADJUST: '调整',
    TRANSFER: '调拨',
    CHECK: '盘点'
  }
  return texts[type] || type
}

const getOperationTypeText = (type: string) => {
  const texts: Record<string, string> = {
    CREATE: '新建商品',
    IN: '商品入库',
    OUT: '商品出库',
    ADJUST: '商品调整',
    TRANSFER: '商品调拨',
    CHECK: '商品盘点'
  }
  return texts[type] || type
}

const getChangeClass = (change: number) => {
  if (change > 0) return 'change-positive'
  if (change < 0) return 'change-negative'
  return 'change-zero'
}

const formatChangeQuantity = (change: number) => {
  if (change > 0) return `+${change}`
  if (change < 0) return `${change}`
  return '0'
}

// 加载数据
const loadLogs = async (params?: PageParams) => {
  try {
    isLoading.value = true

    const queryParams: any = {
      page: params?.page || 0,
      size: params?.size || 5
    }

    // 构建查询参数
    if (searchForm.productId) {
      queryParams.productId = searchForm.productId
    }
    if (searchForm.type) {
      queryParams.type = searchForm.type
    }
    if (searchForm.operator) {
      queryParams.operator = searchForm.operator
    }
    if (searchForm.dateRange?.length === 2) {
      queryParams.startTime = formatDate(searchForm.dateRange[0], 'YYYY-MM-DD')
      queryParams.endTime = formatDate(searchForm.dateRange[1], 'YYYY-MM-DD')
    }

    await inventoryLogStore.loadLogs(queryParams)

  } catch (error) {
    message.error('加载日志失败')
  } finally {
    isLoading.value = false
  }
}

// 搜索
const handleSearch = () => {
  loadLogs({ page: 0 })
}

// 重置
const handleReset = () => {
  searchForm.productId = undefined
  searchForm.type = undefined
  searchForm.operator = ''
  searchForm.dateRange = []
  loadLogs({ page: 0 })
}

// 刷新
const handleRefresh = () => {
  loadLogs()
  message.success('刷新成功')
}

// 导出
const handleExport = async () => {
  try {
    exportLoading.value = true
    await inventoryLogStore.exportLogs(searchForm)
    message.success('导出成功')
  } catch (error) {
    message.error('导出失败')
  } finally {
    exportLoading.value = false
  }
}

// 表格变化
const handleTableChange = (pag: any, filters: any, sorter: any) => {
  const params: any = {
    page: pag.current - 1,
    size: pag.pageSize
  }

  if (sorter && sorter.field) {
    params.sort = sorter.field
    params.direction = sorter.order === 'ascend' ? 'asc' : 'desc'
  }

  loadLogs(params)
}

// 查看详情
const handleViewDetail = (record: InventoryLogRecord) => {
  currentLog.value = record
  detailVisible.value = true
}

// 查看商品
const handleViewProduct = (productId: number) => {
  router.push(`/products/${productId}`)
}

// 初始化
onMounted(() => {
  loadLogs()
  // 加载商品列表用于筛选
  productStore.loadProducts({ page: 0, size: 100 })
})
</script>

<style scoped>
.inventory-log-page {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.search-card {
  margin-bottom: 16px;
}

.stats-cards {
  margin-bottom: 16px;
}

.text-center {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.table-card {
  border-radius: 8px;
}

.time-cell {
  display: flex;
  flex-direction: column;
  font-size: 12px;
  line-height: 1.4;
}

.time-cell .date {
  font-weight: 500;
  color: #333;
}

.time-cell .time {
  color: #666;
}

.product-info {
  display: flex;
  flex-direction: column;
}

.product-name {
  font-weight: 500;
  color: #333;
  display: flex;
  align-items: center;
  gap: 6px;
}

.product-code {
  font-size: 12px;
  color: #666;
}

.stock-change {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.change-quantity {
  font-size: 16px;
  font-weight: 600;
  display: flex;
  align-items: baseline;
  gap: 2px;
}

.change-quantity .unit {
  font-size: 12px;
  color: #666;
  font-weight: normal;
}

.change-positive {
  color: #52c41a;
}

.change-negative {
  color: #d3adf7;
}

.change-zero {
  color: #999;
}

.stock-range {
  font-size: 12px;
  color: #666;
  background-color: #f5f5f5;
  padding: 2px 6px;
  border-radius: 4px;
  display: inline-block;
}

.operator-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.operator-name {
  color: #333;
}

.reason-cell {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: default;
}

.error-msg {
  font-size: 12px;
  color: #ff4d4f;
  margin-top: 4px;
  max-width: 150px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.log-detail {
  padding: 8px;
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .stats-cards {
    margin-bottom: 12px;
  }
}
</style>
