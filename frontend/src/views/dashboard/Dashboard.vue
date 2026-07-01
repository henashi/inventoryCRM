<template>
  <div class="dashboard">
    <div class="dashboard-header">
      <div>
        <p class="dashboard-desc">库存管理系统概览</p>
      </div>
      <div class="dashboard-actions"></div>
    </div>

    <a-row :gutter="[16, 16]" class="mb-4">
      <a-col :xs="24" :sm="12" :md="8" v-for="card in summaryCards" :key="card.key">
        <a-card
          :bordered="false"
          class="summary-card clickable"
          :loading="loading"
          @click="router.push(card.path)"
        >
          <div class="summary-card-label">{{ card.label }}</div>
          <div class="summary-card-value">{{ card.value }}</div>
          <div class="summary-card-helper">{{ card.helper }}</div>
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
                    <span class="text-secondary"
                      >库存 {{ item.currentStock }} / 安全线 {{ item.safeStock }}</span
                    >
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
                    <span class="text-secondary"
                      >{{ item.orderTime?.slice(0, 16) }} ·
                      {{ item.items?.length || 0 }} 件商品</span
                    >
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
// @ts-nocheck
  import { onMounted, onUnmounted, ref, nextTick, watch } from 'vue'
  import { useRouter } from 'vue-router'
  import * as echarts from 'echarts'
  import { productApi } from '@/api/product'
  import { customerApi } from '@/api/customer'
  import { orderApi } from '@/api/order'
  import { aiApi } from '@/api/ai'
  import { inventoryLogApi } from '@/api/inventoryLog'
  import { giftApi } from '@/api/gift'
  import { dataDictApi } from '@/api/dataDict'
  import type { StockAlert, InventoryLog } from '@/types'
  import dayjs from 'dayjs'

  const router = useRouter()

  const chartDays = ref(7)
  const trendChartRef = ref<HTMLElement>()
  const pieChartRef = ref<HTMLElement>()
  let trendChart: echarts.ECharts | null = null
  let pieChart: echarts.ECharts | null = null

  const loading = ref(false)

  const summaryCards = ref([
    {
      key: 'customers',
      label: '客户总量',
      value: 0,
      helper: '正常 0 / 停用 0',
      path: '/customers',
    },
    {
      key: 'products',
      label: '商品概况',
      value: 0,
      helper: '在售 0 / 低库存 0',
      path: '/products',
    },
    {
      key: 'inventory',
      label: '库存操作',
      value: 0,
      helper: '成功 0 / 失败 0',
      path: '/inventory',
    },
    { key: 'orders', label: '订单总数', value: 0, helper: '--', path: '/orders' },
    { key: 'gifts', label: '礼品管理', value: 0, helper: '--', path: '/gifts' },
    { key: 'configs', label: '系统配置', value: 0, helper: '配置项 0', path: '/data-dicts' },
  ])

  const alerts = ref<StockAlert[]>([])
  const recentOrders = ref<any[]>([])

  const trendData = ref<{ date: string; inCount: number; outCount: number }[]>([])

  async function loadSummary() {
    loading.value = true
    const results = await Promise.allSettled([
      customerApi.getStatistics(),
      productApi.getStockStatistics(),
      inventoryLogApi.getStats(),
      orderApi.list({ page: 0, size: 5 }),
      giftApi.loadGifts({ page: 0, size: 1 }),
      aiApi.getAlerts('DANGER'),
      aiApi.getAlerts('WARNING'),
    ])

    if (results[0].status === 'fulfilled') {
      const d = results[0].value
      summaryCards.value[0].value = d.totalCustomers || 0
      summaryCards.value[0].helper = `正常 ${d.normalCustomers || 0} / 停用 ${d.disabledCustomers || 0}`
    }
    if (results[1].status === 'fulfilled') {
      const d = results[1].value
      summaryCards.value[1].value = d.totalProducts || 0
      summaryCards.value[1].helper = `在售 ${d.activeProducts || 0} / 低库存 ${d.lowStockProducts || 0}`
    }
    if (results[2].status === 'fulfilled') {
      const d = results[2].value
      const total = (d.inCount || 0) + (d.outCount || 0)
      summaryCards.value[2].value = total
      summaryCards.value[2].helper = `成功 ${d.successCount || 0} / 失败 ${d.failureCount || 0}`
    }
    if (results[3].status === 'fulfilled') {
      const res = results[3].value
      summaryCards.value[3].value = (res as any).totalElements || 0
      summaryCards.value[3].helper = `最近 ${((res as any).content || []).length} 笔`
      recentOrders.value = ((res as any).content || []).slice(0, 5)
    }
    if (results[4].status === 'fulfilled') {
      const res = results[4].value
      summaryCards.value[4].value = (res as any).totalElements || 0
      summaryCards.value[4].helper = `共 ${((res as any).content || []).length} 页`
    }
    if (results[5].status === 'fulfilled' || results[6].status === 'fulfilled') {
      const danger = results[5].status === 'fulfilled' ? results[5].value : []
      const warning = results[6].status === 'fulfilled' ? results[6].value : []
      alerts.value = [...(danger || []), ...(warning || [])]
    }
    // 系统配置
    try {
      const res = await dataDictApi.loadDataDicts({ page: 0, size: 1 })
      const total = (res as any).totalElements || 0
      summaryCards.value[5].value = total
      summaryCards.value[5].helper = `共 ${total} 项`
    } catch {
      summaryCards.value[5].value = 0
      summaryCards.value[5].helper = '--'
    }
    loading.value = false
  }

  async function loadTrendData() {
    const days = chartDays.value
    const startTime = dayjs().subtract(days, 'day').format('YYYY-MM-DD')
    const endTime = dayjs().format('YYYY-MM-DD')
    try {
      const res = (await inventoryLogApi.getLogs({ page: 0, size: 500, startTime, endTime })) as any
      const logs: InventoryLog[] = res.content || []
      const dateMap: Record<string, { inCount: number; outCount: number }> = {}
      for (let i = 0; i < days; i++) {
        const d = dayjs()
          .subtract(days - 1 - i, 'day')
          .format('YYYY-MM-DD')
        dateMap[d] = { inCount: 0, outCount: 0 }
      }
      logs.forEach((log) => {
        const d = dayjs(log.logTime).format('YYYY-MM-DD')
        if (dateMap[d]) {
          if (log.logType === 'IN' || log.logType === 'CREATE')
            dateMap[d].inCount += log.quantity || 0
          if (log.logType === 'OUT') dateMap[d].outCount += log.quantity || 0
        }
      })
      trendData.value = Object.entries(dateMap).map(([date, v]) => ({ date, ...v }))
    } catch {
      trendData.value = []
    }
    renderTrendChart()
  }

  function isDarkMode() {
    return document.documentElement.getAttribute('data-theme') === 'dark'
  }
  function getChartTextColor() {
    return isDarkMode() ? '#c0c0c0' : '#666666'
  }
  function getChartColors() {
    if (isDarkMode()) {
      return {
        green: '#4ade80',
        blue: '#60a5fa',
        yellow: '#fbbf24',
        red: '#f87171',
        areaAlpha: '0.08',
      }
    }
    return {
      green: '#52c41a',
      blue: '#1890ff',
      yellow: '#faad14',
      red: '#f5222d',
      areaAlpha: '0.1',
    }
  }

  function renderTrendChart() {
    if (!trendChartRef.value) return
    if (trendChart) trendChart.dispose()
    trendChart = echarts.init(trendChartRef.value)
    const data = trendData.value
    const xData = data.map((d) => dayjs(d.date).format('MM-DD'))
    const inData = data.map((d) => d.inCount)
    const outData = data.map((d) => d.outCount)
    const textColor = getChartTextColor()
    const isDark = isDarkMode()

    trendChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['入库', '出库'], bottom: 0, textStyle: { color: textColor } },
      grid: { left: 40, right: 16, top: 8, bottom: 36 },
      xAxis: { type: 'category', data: xData, axisLabel: { fontSize: 11, color: textColor } },
      yAxis: { type: 'value', axisLabel: { color: textColor } },
      series: [
        {
          name: '入库',
          type: 'line',
          data: inData,
          smooth: true,
          lineStyle: { color: isDark ? '#4ade80' : '#52c41a' },
          areaStyle: { color: isDark ? 'rgba(74,222,128,0.08)' : 'rgba(82,196,26,0.1)' },
        },
        {
          name: '出库',
          type: 'line',
          data: outData,
          smooth: true,
          lineStyle: { color: isDark ? '#60a5fa' : '#1890ff' },
          areaStyle: { color: isDark ? 'rgba(96,165,250,0.08)' : 'rgba(24,144,255,0.1)' },
        },
      ],
    })
  }

  const stockPieData = ref<{ name: string; value: number; color: string }[]>([])

  async function loadPieData() {
    try {
      const res = (await productApi.getProducts({ page: 0, size: 999 })) as any
      const products: { currentStock: number; safeStock: number }[] = res.content || []
      let normal = 0,
        lowStock = 0,
        outOfStock = 0
      products.forEach((p) => {
        const stock = p.currentStock || 0
        const safe = p.safeStock || 0
        if (stock <= 0) outOfStock++
        else if (stock < safe) lowStock++
        else normal++
      })
      stockPieData.value = [
        { name: '库存充足', value: normal, color: isDarkMode() ? '#60a5fa' : '#1890ff' },
        { name: '低库存', value: lowStock, color: isDarkMode() ? '#facc15' : '#eab308' },
        { name: '缺货', value: outOfStock, color: isDarkMode() ? '#f87171' : '#f5222d' },
      ]
    } catch {
      stockPieData.value = []
    }
    renderPieChart()
  }

  function renderPieChart() {
    if (!pieChartRef.value) return
    if (pieChart) pieChart.dispose()
    const data = stockPieData.value
    if (data.length === 0) return
    pieChart = echarts.init(pieChartRef.value)
    const textColor = getChartTextColor()
    pieChart.setOption({
      tooltip: { trigger: 'item' },
      series: [
        {
          type: 'pie',
          radius: ['40%', '65%'],
          center: ['50%', '50%'],
          data: data.map((d) => ({ name: d.name, value: d.value, itemStyle: { color: d.color } })),
          label: {
            show: true,
            formatter: '{b}\n{d}%',
            fontSize: 11,
            color: textColor,
            textBorderColor: 'transparent',
          },
        },
      ],
    })
  }

  watch(chartDays, () => loadTrendData())

  onMounted(() => {
    loadSummary()
    loadTrendData()
    loadPieData()
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
  .dashboard-title {
    font-size: 20px;
    font-weight: 600;
    margin: 0;
    color: var(--text-primary);
  }
  .dashboard-desc {
    font-size: 14px;
    color: var(--text-secondary);
    margin-top: 2px;
  }
  .dashboard-actions {
    display: flex;
    gap: 8px;
  }
  .mb-4 {
    margin-bottom: 16px;
  }

  .summary-card {
    min-height: 140px;
    border-radius: 8px;
  }
  .summary-card.clickable {
    cursor: pointer;
    transition:
      transform 0.2s ease,
      box-shadow 0.2s ease;
  }
  .summary-card.clickable:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  }
  .summary-card-label {
    font-size: 14px;
    color: var(--text-secondary);
  }
  .summary-card-value {
    font-size: 30px;
    font-weight: 700;
    color: var(--text-primary);
    line-height: 1.2;
    margin-top: 8px;
  }
  .summary-card-helper {
    margin-top: 8px;
    color: var(--text-tertiary);
    font-size: 13px;
  }

  .chart-wrap {
    height: 280px;
  }
  .chart {
    width: 100%;
    height: 100%;
  }

  .text-secondary {
    color: var(--text-tertiary);
    font-size: 12px;
  }

  .alert-item {
    cursor: pointer;
  }
  .alert-item:hover {
    background: var(--bg-chat-avatar);
  }
  .alert-title {
    font-weight: 500;
    color: var(--text-primary);
  }
  .order-title {
    font-weight: 500;
  }
  .order-amount {
    font-weight: 600;
    color: #f5222d;
  }

  .empty-list {
    padding: 40px 0;
    display: flex;
    justify-content: center;
  }
</style>
