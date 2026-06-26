<template>
  <div class="dashboard">
    <div class="dashboard-header">
      <div>

        <p class="dashboard-desc">库存管理系统概览</p>
      </div>
      <div class="dashboard-actions">

      </div>
    </div>

    <a-row :gutter="[16, 16]" class="mb-4">
      <a-col :xs="12" :md="6" v-for="stat in statCards" :key="stat.key">
        <a-card :bordered="false" class="stat-card" :style="{ borderLeftColor: stat.color }">
          <div class="stat-body">
            <div class="stat-info">
              <div class="stat-label">{{ stat.label }}</div>
              <div class="stat-value">{{ stat.value }}</div>
            </div>
            <div class="stat-icon-wrap" :style="{ background: stat.bg }">
              <component :is="stat.icon" :style="{ color: stat.color }" />
            </div>
          </div>
          <div class="stat-footer">
            <span :style="{ color: stat.trendColor }">
              <arrow-up-outlined v-if="stat.trend > 0" />{{ stat.trend > 0 ? '+' : '' }}{{ stat.trend }}%
            </span>
            <span class="text-secondary">{{ stat.period }}</span>
          </div>
        </a-card>
      </a-col>
    </a-row>

    <a-row :gutter="[16, 16]" class="mb-4">
      <a-col :xs="24" :lg="16">
        <a-card :bordered="false" title="出入库趋势">
          <template #extra>
            <a-radio-group v-model:value="chartDays" size="small" button-style="solid">
              <a-radio-button value="7">7天</a-radio-button>
              <a-radio-button value="30">30天</a-radio-button>
            </a-radio-group>
          </template>
          <div class="chart-wrap">
            <div ref="trendChartRef" class="chart"></div>
          </div>
        </a-card>
      </a-col>
      <a-col :xs="24" :lg="8">
        <a-card :bordered="false" title="库存占比">
          <div class="chart-wrap">
            <div ref="pieChartRef" class="chart"></div>
          </div>
        </a-card>
      </a-col>
    </a-row>

    <a-row :gutter="[16, 16]">
      <a-col :xs="24" :lg="12">
        <a-card :bordered="false" title="待处理">
          <template #extra>
            <a-button type="link" size="small" @click="router.push('/inventory')">更多</a-button>
          </template>
          <div v-if="alerts.length === 0" class="empty-list">
            <a-empty description="暂无待处理事项" />
          </div>
          <a-list v-else :data-source="alerts" size="small">
            <template #renderItem="{ item }">
              <a-list-item class="alert-item" @click="router.push('/inventory')">
                <template #extra>
                  <a-tag :color="item.level === 'DANGER' ? 'red' : 'orange'">
                    {{ item.level === 'DANGER' ? '高危' : '预警' }}
                  </a-tag>
                </template>
                <a-list-item-meta>
                  <template #title>
                    <span class="alert-title">{{ item.productName }}</span>
                  </template>
                  <template #description>
                    <span class="text-secondary">库存 {{ item.currentStock }} / 安全线 {{ item.safeStock }}</span>
                  </template>
                </a-list-item-meta>
              </a-list-item>
            </template>
          </a-list>
        </a-card>
      </a-col>
      <a-col :xs="24" :lg="12">
        <a-card :bordered="false" title="最近订单">
          <template #extra>
            <a-button type="link" size="small" @click="router.push('/orders')">更多</a-button>
          </template>
          <div v-if="recentOrders.length === 0" class="empty-list">
            <a-empty description="暂无订单数据" />
          </div>
          <a-list v-else :data-source="recentOrders" size="small">
            <template #renderItem="{ item }">
              <a-list-item>
                <a-list-item-meta>
                  <template #title>
                    <span class="order-title">{{ item.customerName }}</span>
                  </template>
                  <template #description>
                    <span class="text-secondary">{{ item.orderTime?.slice(0, 16) }} · {{ item.items?.length || 0 }} 件商品</span>
                  </template>
                </a-list-item-meta>
                <template #extra>
                  <span class="order-amount">¥{{ item.finalAmount.toFixed(2) }}</span>
                </template>
              </a-list-item>
            </template>
          </a-list>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  ShoppingOutlined, UserOutlined, GiftOutlined, WarningOutlined,
  ArrowUpOutlined,
} from '@ant-design/icons-vue'
import * as echarts from 'echarts'
import { productApi } from '@/api/product'
import { customerApi } from '@/api/customer'
import { orderApi } from '@/api/order'
import { aiApi } from '@/api/ai'
import type { StockAlert } from '@/types'

const router = useRouter()

const chartDays = ref(7)
const trendChartRef = ref<HTMLElement>()
const pieChartRef = ref<HTMLElement>()
let trendChart: echarts.ECharts | null = null
let pieChart: echarts.ECharts | null = null

const statCards = ref([
  { key: 'products', label: '商品总数', value: 0, icon: ShoppingOutlined, color: '#1890ff', bg: '#e6f7ff', trend: 0, period: '较上月' },
  { key: 'customers', label: '客户总数', value: 0, icon: UserOutlined, color: '#52c41a', bg: '#f6ffed', trend: 0, period: '较上月' },
  { key: 'gifts', label: '礼品数', value: 0, icon: GiftOutlined, color: '#faad14', bg: '#fffbe6', trend: 0, period: '较上月' },
  { key: 'alerts', label: '库存预警', value: 0, icon: WarningOutlined, color: '#f5222d', bg: '#fff2f0', trend: 0, period: '待处理' },
])

const alerts = ref<StockAlert[]>([])
const recentOrders = ref<any[]>([])

const trendColor = ref('#52c41a')

function loadStats() {
  Promise.all([
    productApi.getProducts({ page: 0, size: 1 }),
    customerApi.getCustomers({ page: 0, size: 1 }),
    orderApi.list({ page: 0, size: 5 }),
    aiApi.getAlerts('DANGER'),
    aiApi.getAlerts('WARNING'),
  ]).then(([products, customers, orders, danger, warning]) => {
    statCards.value[0].value = products.totalElements || 0
    statCards.value[1].value = customers.totalElements || 0
    statCards.value[2].value = 0 // gift count
    statCards.value[3].value = (danger?.length || 0) + (warning?.length || 0)
    recentOrders.value = (orders as any).content || []
    alerts.value = [...(danger || []), ...(warning || [])]
  }).catch(() => {})
}

function renderTrendChart() {
  if (!trendChartRef.value) return
  if (trendChart) trendChart.dispose()
  trendChart = echarts.init(trendChartRef.value)
  const days = chartDays.value
  const xData = Array.from({ length: days }, (_, i) => `${i + 1}日`)
  const inData = xData.map(() => Math.round(Math.random() * 50 + 10))
  const outData = xData.map(() => Math.round(Math.random() * 40 + 5))

  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['入库', '出库'], bottom: 0 },
    grid: { left: 40, right: 16, top: 8, bottom: 36 },
    xAxis: { type: 'category', data: xData, axisLabel: { fontSize: 11 } },
    yAxis: { type: 'value' },
    series: [
      { name: '入库', type: 'line', data: inData, smooth: true, lineStyle: { color: '#52c41a' }, areaStyle: { color: 'rgba(82,196,26,0.1)' } },
      { name: '出库', type: 'line', data: outData, smooth: true, lineStyle: { color: '#1890ff' }, areaStyle: { color: 'rgba(24,144,255,0.1)' } },
    ],
  })
}

function renderPieChart() {
  if (!pieChartRef.value) return
  if (pieChart) pieChart.dispose()
  pieChart = echarts.init(pieChartRef.value)
  pieChart.setOption({
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie',
      radius: ['40%', '65%'],
      center: ['50%', '50%'],
      data: [
        { name: '高价值', value: 30, itemStyle: { color: '#52c41a' } },
        { name: '成长', value: 45, itemStyle: { color: '#faad14' } },
        { name: '待激活', value: 25, itemStyle: { color: '#d9d9d9' } },
      ],
      label: { show: true, formatter: '{b}\n{d}%', fontSize: 11 },
    }],
  })
}

watch(chartDays, () => nextTick(renderTrendChart))

onMounted(() => {
  loadStats()
  nextTick(() => { renderTrendChart(); renderPieChart() })
})

onUnmounted(() => {
  if (trendChart) trendChart.dispose()
  if (pieChart) pieChart.dispose()
})
</script>

<style scoped>
.dashboard {
  padding: 24px;
  background: var(--bg-page);
  min-height: 100vh;
}
.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}
.dashboard-title { font-size: 20px; font-weight: 600; margin: 0; color: var(--text-primary); }
.dashboard-desc { font-size: 14px; color: var(--text-secondary); margin-top: 2px; }
.dashboard-actions { display: flex; gap: 8px; }
.mb-4 { margin-bottom: 16px; }

.stat-card { border-radius: 8px; border-left: 4px solid; }
.stat-card :deep(.ant-card) { background: var(--bg-card); }
.stat-card :deep(.ant-card-body) { padding: 16px 20px 0; }
.stat-body { display: flex; justify-content: space-between; align-items: flex-start; }
.stat-info { flex: 1; }
.stat-label { font-size: 14px; color: var(--text-secondary); }
.stat-value { font-size: 30px; font-weight: 700; color: var(--text-primary); line-height: 1.2; margin-top: 4px; }
.stat-icon-wrap { width: 48px; height: 48px; border-radius: 12px; display: flex; align-items: center; justify-content: center; font-size: 22px; flex-shrink: 0; }
.stat-footer { display: flex; justify-content: space-between; padding: 8px 0 0; font-size: 12px; border-top: 1px solid var(--border-color); margin-top: 12px; }

.chart-wrap { height: 280px; }
.chart { width: 100%; height: 100%; }

.text-secondary { color: var(--text-tertiary); font-size: 12px; }

.alert-item { cursor: pointer; }
.alert-item:hover { background: var(--bg-chat-avatar); }
.alert-title { font-weight: 500; color: var(--text-primary); }
.order-title { font-weight: 500; }
.order-amount { font-weight: 600; color: #f5222d; }

.empty-list { padding: 40px 0; display: flex; justify-content: center; }
</style>
