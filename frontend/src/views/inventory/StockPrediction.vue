<template>
  <div class="prediction-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="page-header-main">
        <h1 class="page-title">🤖 AI 库存预测</h1>
        <div class="page-subtitle">
          基于 OLS 线性回归模型的出库趋势预测，自动识别缺货风险并推荐补货量
          <span v-if="lastRunTime" class="last-run">上次预测：{{ lastRunTime }}</span>
        </div>
      </div>
      <div class="page-header-actions">
        <a-button :loading="runningPrediction" type="primary" @click="handleRunPrediction">
          <template #icon><SyncOutlined /></template>
          执行全量预测
        </a-button>
        <a-button @click="goToInventory">返回库存总览</a-button>
      </div>
    </div>

    <!-- 概览统计卡片 -->
    <a-row :gutter="[16, 16]" class="mb-6">
      <a-col :xs="12" :sm="6">
        <a-card class="stat-card stat-danger" :bordered="false" :loading="loading.summary">
          <div class="stat-value">{{ summary.dangerCount }}</div>
          <div class="stat-label">🔴 高危商品</div>
          <div class="stat-hint">预计 ≤7 天耗尽</div>
        </a-card>
      </a-col>
      <a-col :xs="12" :sm="6">
        <a-card class="stat-card stat-warning" :bordered="false" :loading="loading.summary">
          <div class="stat-value">{{ summary.warningCount }}</div>
          <div class="stat-label">🟠 预警商品</div>
          <div class="stat-hint">预计 8-14 天耗尽</div>
        </a-card>
      </a-col>
      <a-col :xs="12" :sm="6">
        <a-card class="stat-card stat-restock" :bordered="false" :loading="loading.summary">
          <div class="stat-value">{{ summary.totalSuggestedRestockQty }}</div>
          <div class="stat-label">📦 建议补货总量</div>
          <div class="stat-hint">共 {{ summary.totalPredicted }} 个商品已分析</div>
        </a-card>
      </a-col>
      <a-col :xs="12" :sm="6">
        <a-card class="stat-card stat-normal" :bordered="false" :loading="loading.summary">
          <div class="stat-value">{{ summary.normalCount }}</div>
          <div class="stat-label">🟢 正常商品</div>
          <div class="stat-hint">库存充足</div>
        </a-card>
      </a-col>
    </a-row>

    <!-- 搜索 + 表格 -->
    <a-card :bordered="false">
      <div class="table-toolbar">
        <a-space-compact>
          <a-input
            v-model:value="searchKeyword"
            placeholder="搜索商品名称 / 编码"
            style="width: 280px"
            @pressEnter="handleSearch"
          />
          <a-button type="primary" @click="handleSearch">搜索</a-button>
        </a-space-compact>
        <div class="toolbar-right">
          <a-select v-model:value="alertFilter" style="width: 140px" @change="handleFilterChange">
            <a-select-option value="ALL">全部级别</a-select-option>
            <a-select-option value="DANGER">🔴 高危</a-select-option>
            <a-select-option value="WARNING">🟠 预警</a-select-option>
            <a-select-option value="NORMAL">🟢 正常</a-select-option>
          </a-select>
        </div>
      </div>

      <a-table
        row-key="productId"
        :columns="columns"
        :data-source="filteredPredictions"
        :pagination="pagination"
        :loading="loading.table"
        :expanded-row-keys="expandedRowKeys"
        @expand="handleExpand"
        @change="handleTableChange"
        size="small"
        :scroll="{ x: 1000, y: 480 }"
      >
        <!-- 商品信息 -->
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'product'">
            <div class="product-cell">
              <div class="product-name">{{ record.productName }}</div>
              <div class="product-code">{{ record.productCode }}</div>
            </div>
          </template>

          <!-- 库存 -->
          <template v-if="column.key === 'stock'">
            <div class="stock-cell">
              <span class="stock-value">{{ record.currentStock }}</span>
              <span class="stock-unit">{{ record.unit }}</span>
              <a-tooltip title="安全库存">
                <span class="safe-stock">/ {{ record.safeStock }}</span>
              </a-tooltip>
            </div>
          </template>

          <!-- 日均出库 -->
          <template v-if="column.key === 'avgOut'">
            <div class="avg-out-cell">
              <div class="avg-7d">{{ record.avgDailyOut7d?.toFixed(1) ?? '-' }}</div>
              <div class="avg-hint">近7日</div>
            </div>
          </template>

          <!-- 预计耗尽天数 -->
          <template v-if="column.key === 'daysToEmpty'">
            <a-tag v-if="record.estimatedDaysToEmpty >= 0" :color="getDaysColor(record.estimatedDaysToEmpty)">
              {{ record.estimatedDaysToEmpty }} 天
            </a-tag>
            <span v-else class="text-gray-400">无出库记录</span>
          </template>

          <!-- 模型置信度 -->
          <template v-if="column.key === 'modelInfo'">
            <a-tooltip v-if="record.slope !== undefined" :title="getModelTooltip(record)">
              <span class="model-info-cell">
                <a-tag :color="getConfidenceColor(record.rSquared ?? 0)" style="font-size:11px">
                  R²={{ (record.rSquared ?? 0).toFixed(2) }}
                </a-tag>
                <span :class="getTrendClass(record.trendDirection)">
                  {{ getTrendIcon(record.trendDirection) }}
                </span>
              </span>
            </a-tooltip>
            <span v-else class="text-gray-400">—</span>
          </template>

          <!-- 预警级别 -->
          <template v-if="column.key === 'alertLevel'">
            <a-tag :color="getAlertColor(record.alertLevel)">
              {{ getAlertLabel(record.alertLevel) }}
            </a-tag>
          </template>

          <!-- 建议补货量 -->
          <template v-if="column.key === 'suggestRestock'">
            <span v-if="record.suggestedRestockQty > 0" class="restock-value">
              +{{ record.suggestedRestockQty }} {{ record.unit }}
            </span>
            <span v-else class="text-gray-400">—</span>
          </template>

          <!-- 趋势图 -->
          <template v-if="column.key === 'trend'">
            <a-button type="link" size="small" @click="toggleTrend(record.productId)">
              {{ expandedRowKeys.includes(record.productId) ? '收起' : '趋势' }}
            </a-button>
          </template>

          <!-- 操作 -->
          <template v-if="column.key === 'action'">
            <a-button type="link" size="small" @click="goToStockIn(record)">
              补货
            </a-button>
          </template>
        </template>

        <!-- 展开行：趋势图 -->
        <template #expandedRowRender="{ record }">
          <div v-if="expandedRowKeys.includes(record.productId)" class="trend-row">
            <div class="trend-header">
              <h4>{{ record.productName }} — 出库趋势（近30天）</h4>
              <div class="trend-legend">
                <span class="legend-item"><span class="legend-dot bar-color"></span> 每日出库量</span>
                <span class="legend-item"><span class="legend-dot avg7-color"></span> 近7日均线 ({{ record.avgDailyOut7d?.toFixed(1) ?? '-' }})</span>
                <span class="legend-item"><span class="legend-dot avg30-color"></span> 近30日均线 ({{ record.avgDailyOut30d?.toFixed(1) ?? '-' }})</span>
                <span class="legend-item"><span class="legend-dot safe-color"></span> 安全库存线 ({{ record.safeStock }})</span>
              </div>
            </div>
            <div :id="'trend-chart-' + record.productId" class="trend-chart" ref="chartRefs"></div>
          </div>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { SyncOutlined } from '@ant-design/icons-vue'
import * as echarts from 'echarts'
import { aiApi } from '@/api/ai'
import type { DailyOutRecord, PredictionSummary, StockPrediction } from '@/types'

const router = useRouter()

// ===== 状态 =====
const predictions = ref<StockPrediction[]>([])
const filteredPredictions = ref<StockPrediction[]>([])
const summary = ref<PredictionSummary>({
  totalPredicted: 0,
  dangerCount: 0,
  warningCount: 0,
  normalCount: 0,
  totalSuggestedRestockQty: 0,
  executionTimeMs: 0,
})
const lastRunTime = ref<string>('')

const loading = ref({
  table: false,
  summary: false,
})
const runningPrediction = ref(false)

const searchKeyword = ref('')
const alertFilter = ref<string>('ALL')

const pagination = ref({
  current: 1,
  pageSize: 5,
  total: 0,
  showSizeChanger: false,
  showQuickJumper: false,
  showTotal: (total: number) => `共 ${total} 条`,
})

const expandedRowKeys = ref<number[]>([])
const chartRefs = ref<Record<string, HTMLElement>>({})
let chartInstances: Record<string, echarts.ECharts> = {}

// ===== 表格列定义 =====
const columns = [
  {
    title: '商品',
    key: 'product',
    width: 180,
    fixed: 'left' as const,
  },
  {
    title: '当前库存',
    key: 'stock',
    width: 120,
    align: 'center' as const,
    sorter: (a: StockPrediction, b: StockPrediction) => a.currentStock - b.currentStock,
  },
  {
    title: '日均出库',
    key: 'avgOut',
    width: 100,
    align: 'center' as const,
  },
  {
    title: '预计耗尽',
    key: 'daysToEmpty',
    width: 120,
    align: 'center' as const,
    sorter: (a: StockPrediction, b: StockPrediction) => {
      const da = a.estimatedDaysToEmpty >= 0 ? a.estimatedDaysToEmpty : 9999
      const db = b.estimatedDaysToEmpty >= 0 ? b.estimatedDaysToEmpty : 9999
      return da - db
    },
  },
  {
    title: '模型',
    key: 'modelInfo',
    width: 100,
    align: 'center' as const,
  },
  {
    title: '预警级别',
    key: 'alertLevel',
    width: 100,
    align: 'center' as const,
  },
  {
    title: '建议补货',
    key: 'suggestRestock',
    width: 110,
    align: 'center' as const,
  },
  {
    title: '趋势',
    key: 'trend',
    width: 70,
    align: 'center' as const,
  },
  {
    title: '操作',
    key: 'action',
    width: 70,
    align: 'center' as const,
  },
]

// ===== 方法 =====

/** 加载预测数据 */
const loadData = async () => {
  loading.value.table = true
  loading.value.summary = true

  try {
    const [pageRes, summaryRes] = await Promise.all([
      aiApi.getPredictions({
        page: pagination.value.current - 1,
        size: pagination.value.pageSize,
        keyword: searchKeyword.value || undefined,
      }),
      aiApi.getPredictionSummary(),
    ])

    predictions.value = pageRes.content
    pagination.value.total = pageRes.totalElements
    summary.value = summaryRes
    lastRunTime.value = new Date().toLocaleString('zh-CN')

    applyFilter()
  } catch {
    message.error('加载预测数据失败')
  } finally {
    loading.value.table = false
    loading.value.summary = false
  }
}

/** 搜索 */
const handleSearch = () => {
  pagination.value.current = 1
  loadData()
}

/** 级别过滤 */
const handleFilterChange = () => {
  applyFilter()
}

const applyFilter = () => {
  if (alertFilter.value === 'ALL') {
    filteredPredictions.value = predictions.value
  } else {
    filteredPredictions.value = predictions.value.filter(
      p => p.alertLevel === alertFilter.value
    )
  }
}

/** 表格翻页/排序 */
const handleTableChange = (pag: { current?: number }) => {
  if (pag.current) {
    pagination.value.current = pag.current
  }
  loadData()
}

/** 表格展开行事件 */
const handleExpand = (expanded: boolean, record: StockPrediction) => {
  if (expanded) {
    expandedRowKeys.value = [record.productId]
    nextTick(() => renderChart(record.productId))
  } else {
    disposeChart(record.productId)
    expandedRowKeys.value = []
  }
}

/** 展开/收起趋势图（按钮点击） */
const toggleTrend = async (productId: number) => {
  const isExpanded = expandedRowKeys.value.includes(productId)
  if (isExpanded) {
    disposeChart(productId)
    expandedRowKeys.value = []
  } else {
    // 收起旧的
    if (expandedRowKeys.value.length > 0) {
      disposeChart(expandedRowKeys.value[0])
    }
    expandedRowKeys.value = [productId]
    await nextTick()
    renderChart(productId)
  }
}

/** 渲染 ECharts 趋势图 */
const renderChart = (productId: number) => {
  const record = predictions.value.find(p => p.productId === productId)
  if (!record || !record.dailyOutRecords || record.dailyOutRecords.length === 0) return

  const chartDom = document.getElementById('trend-chart-' + productId)
  if (!chartDom) return

  if (chartInstances[productId]) {
    chartInstances[productId].dispose()
  }

  const chart = echarts.init(chartDom)
  chartInstances[productId] = chart

  const dates = record.dailyOutRecords.map((d: DailyOutRecord) => d.date.slice(5))
  const quantities = record.dailyOutRecords.map((d: DailyOutRecord) => d.quantity)
  const avg7 = record.avgDailyOut7d || 0
  const avg30 = record.avgDailyOut30d || 0
  const safeStock = record.safeStock

  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '8%',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: dates,
      axisLabel: { rotate: 45, fontSize: 10 },
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
    },
    series: [
      {
        name: '每日出库量',
        type: 'bar',
        data: quantities,
        itemStyle: { color: '#91cc75' },
        barMaxWidth: 16,
      },
      {
        name: `近7日均线 (${avg7.toFixed(1)})`,
        type: 'line',
        data: Array(dates.length).fill(avg7),
        lineStyle: { color: '#faad14', width: 2, type: 'dashed' },
        symbol: 'none',
      },
      {
        name: `近30日均线 (${avg30.toFixed(1)})`,
        type: 'line',
        data: Array(dates.length).fill(avg30),
        lineStyle: { color: '#1890ff', width: 2, type: 'dashed' },
        symbol: 'none',
      },
      {
        name: `安全库存线 (${safeStock})`,
        type: 'line',
        data: Array(dates.length).fill(safeStock),
        lineStyle: { color: '#f5222d', width: 2, type: 'solid' },
        symbol: 'none',
      },
    ],
  }

  chart.setOption(option)

  // 响应窗口变化
  const handleResize = () => chart.resize()
  window.addEventListener('resize', handleResize)
  ;(chart as any)._resizeHandler = handleResize
}

/** 销毁图表 */
const disposeChart = (productId: number) => {
  const chart = chartInstances[productId]
  if (chart) {
    if ((chart as any)._resizeHandler) {
      window.removeEventListener('resize', (chart as any)._resizeHandler)
    }
    chart.dispose()
    delete chartInstances[productId]
  }
}

/** 手动触发全量预测 */
const handleRunPrediction = async () => {
  runningPrediction.value = true
  try {
    const result = await aiApi.runPrediction()
    message.success(`全量预测完成！分析 ${result.totalPredicted} 个商品，${result.dangerCount} 个高危，耗时 ${result.executionTimeMs}ms`)
    await loadData()
  } catch {
    message.error('全量预测执行失败')
  } finally {
    runningPrediction.value = false
  }
}

/** 跳转到补货页面 */
const goToStockIn = (record: StockPrediction) => {
  router.push({
    path: '/inventory',
    query: { action: 'in', productId: String(record.productId) },
  })
}

/** 返回库存总览 */
const goToInventory = () => {
  router.push('/inventory')
}

// ===== 工具函数 =====

const getDaysColor = (days: number): string => {
  if (days <= 7) return 'red'
  if (days <= 14) return 'orange'
  return 'green'
}

const getModelTooltip = (record: StockPrediction): string => {
  const trend = getTrendLabel(record.trendDirection)
  const r2 = (record.rSquared ?? 0).toFixed(2)
  const slope = (record.slope ?? 0).toFixed(4)
  return `OLS 线性回归 | 斜率=${slope} | R²=${r2} | 趋势=${trend}`
}

const getTrendLabel = (dir?: string): string => {
  switch (dir) {
    case 'UP': return '出库加速 ↑'
    case 'DOWN': return '出库减速 ↓'
    default: return '平稳 →'
  }
}

const getTrendIcon = (dir?: string): string => {
  switch (dir) {
    case 'UP': return '📈'
    case 'DOWN': return '📉'
    default: return '➡️'
  }
}

const getTrendClass = (dir?: string): string => {
  switch (dir) {
    case 'UP': return 'trend-up'
    case 'DOWN': return 'trend-down'
    default: return 'trend-stable'
  }
}

const getConfidenceColor = (r2: number): string => {
  if (r2 >= 0.7) return 'green'
  if (r2 >= 0.3) return 'orange'
  return 'red'
}

const getAlertColor = (level: string): string => {
  switch (level) {
    case 'DANGER': return 'red'
    case 'WARNING': return 'orange'
    default: return 'green'
  }
}

const getAlertLabel = (level: string): string => {
  switch (level) {
    case 'DANGER': return '高危'
    case 'WARNING': return '预警'
    default: return '正常'
  }
}

// ===== 生命周期 =====
onMounted(() => {
  loadData()
})

onUnmounted(() => {
  // 清理所有图表实例
  Object.keys(chartInstances).forEach(id => {
    disposeChart(Number(id))
  })
})

// 搜索或过滤变化时收起展开行
watch([searchKeyword, alertFilter], () => {
  if (expandedRowKeys.value.length > 0) {
    disposeChart(expandedRowKeys.value[0])
    expandedRowKeys.value = []
  }
})
</script>

<style scoped>
.prediction-page {
  padding: 20px;
  background: #f5f7fa;
  min-height: 100vh;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  margin: 0;
  color: #111827;
}

.page-subtitle {
  font-size: 14px;
  color: #6b7280;
  margin-top: 4px;
}

.last-run {
  display: block;
  font-size: 12px;
  color: #9ca3af;
  margin-top: 2px;
}

.mb-6 {
  margin-bottom: 16px;
}

/* 统计卡片 */
.stat-card {
  border-radius: 8px;
  text-align: center;
}

.stat-card :deep(.ant-card-body) {
  padding: 20px 16px;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  line-height: 1.2;
}

.stat-label {
  font-size: 14px;
  font-weight: 500;
  margin-top: 4px;
}

.stat-hint {
  font-size: 11px;
  color: #9ca3af;
  margin-top: 2px;
}

.stat-danger .stat-value { color: #f5222d; }
.stat-warning .stat-value { color: #fa8c16; }
.stat-restock .stat-value { color: #1890ff; }
.stat-normal .stat-value { color: #52c41a; }

/* 表格工具栏 */
.table-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.toolbar-right {
  display: flex;
  gap: 8px;
}

/* 商品单元格 */
.product-cell .product-name {
  font-weight: 500;
  color: #111827;
}

.product-cell .product-code {
  font-size: 12px;
  color: #9ca3af;
}

/* 库存单元格 */
.stock-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.stock-value {
  font-weight: 600;
  font-size: 16px;
}

.stock-unit {
  font-size: 12px;
  color: #6b7280;
}

.safe-stock {
  font-size: 11px;
  color: #9ca3af;
}

/* 日均出库 */
.avg-out-cell {
  text-align: center;
}

.avg-7d {
  font-weight: 600;
}

.avg-hint {
  font-size: 11px;
  color: #9ca3af;
}

/* 补货量 */
.restock-value {
  font-weight: 600;
  color: #1890ff;
}

.text-gray-400 {
  color: #9ca3af;
}

/* 模型信息单元格 */
.model-info-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.trend-up {
  font-size: 14px;
}

.trend-down {
  font-size: 14px;
}

.trend-stable {
  font-size: 14px;
}

/* 趋势行 */
.trend-row {
  padding: 16px;
}

.trend-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.trend-header h4 {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
}

.trend-legend {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #6b7280;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.legend-dot {
  width: 12px;
  height: 3px;
  display: inline-block;
  border-radius: 2px;
}

.bar-color { background: #91cc75; }
.avg7-color { background: #faad14; }
.avg30-color { background: #1890ff; }
.safe-color { background: #f5222d; }

.trend-chart {
  width: 100%;
  height: 280px;
}
</style>
