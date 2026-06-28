<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <div class="page-subtitle">基于多维度指标的客户价值评估</div>
      </div>
      <div class="page-header-actions">
        <a-button @click="goToRecommendations">礼品推荐</a-button>
        <a-button type="primary" :loading="runningScoring" @click="handleRunScoring">
          <template #icon><SyncOutlined /></template>
          执行评分
        </a-button>
      </div>
    </div>

    <a-row :gutter="[16, 16]" class="mb-6">
      <a-col :xs="8" :sm="8">
        <a-card class="stat-card stat-high" :bordered="false">
          <div class="stat-body">
            <div class="stat-inner">
              <div class="stat-label">高价值客户</div>
              <div class="stat-value">{{ segmentCounts.high }}</div>
            </div>
          </div>
          <div class="stat-footer">≥ 80 分</div>
        </a-card>
      </a-col>
      <a-col :xs="8" :sm="8">
        <a-card class="stat-card stat-growing" :bordered="false">
          <div class="stat-body">
            <div class="stat-inner">
              <div class="stat-label">成长客户</div>
              <div class="stat-value">{{ segmentCounts.growing }}</div>
            </div>
          </div>
          <div class="stat-footer">60 ~ 79 分</div>
        </a-card>
      </a-col>
      <a-col :xs="8" :sm="8">
        <a-card class="stat-card stat-inactive" :bordered="false">
          <div class="stat-body">
            <div class="stat-inner">
              <div class="stat-label">待激活客户</div>
              <div class="stat-value">{{ segmentCounts.inactive }}</div>
            </div>
          </div>
          <div class="stat-footer">&lt; 60 分</div>
        </a-card>
      </a-col>
    </a-row>

    <a-row :gutter="16" class="equal-row">
      <a-col :xs="24" :lg="14">
        <a-card :bordered="false">
          <template #title>
            <span>评分排行</span>
          </template>
          <div class="card-toolbar">
            <a-radio-group
              v-model:value="segmentFilter"
              size="small"
              button-style="solid"
              @change="handleFilterChange"
            >
              <a-radio-button value="ALL">全部</a-radio-button>
              <a-radio-button value="HIGH_VALUE">高价值</a-radio-button>
              <a-radio-button value="GROWING">成长</a-radio-button>
              <a-radio-button value="INACTIVE">待激活</a-radio-button>
            </a-radio-group>
          </div>
          <a-table
            row-key="customerId"
            :columns="columns"
            :data-source="displayList"
            :pagination="pagination"
            :loading="loading.table"
            size="small"
            :scroll="{ x: 600 }"
            @change="handleTableChange"
            :row-class="
              (_r: CustomerScore) => (_r.customerId === selectedCustomerId ? 'row-selected' : '')
            "
            :custom-row="(record) => ({ onClick: () => handleRowClick(record) })"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'rank'">
                <span class="rank-num">{{ getRank(record) }}</span>
              </template>
              <template v-if="column.key === 'customer'">
                <div>
                  <div class="name-cell">{{ record.customerName }}</div>
                  <div class="meta-cell">{{ record.phone }}</div>
                </div>
              </template>
              <template v-if="column.key === 'score'">
                <div class="score-bar">
                  <a-progress
                    :percent="record.totalScore"
                    :stroke-color="getScoreColor(record.totalScore)"
                    :show-info="false"
                    size="small"
                  />
                  <span class="score-text" :style="{ color: getScoreColor(record.totalScore) }">
                    {{ record.totalScore }}
                  </span>
                </div>
              </template>
              <template v-if="column.key === 'segment'">
                <a-tag :color="getSegmentColor(record.segment)">{{
                  getSegmentLabel(record.segment)
                }}</a-tag>
              </template>
              <template v-if="column.key === 'giftLevel'">
                <a-rate :value="record.giftLevel" :count="3" disabled />
              </template>
              <template v-if="column.key === 'birthday'"> </template>
            </template>
          </a-table>
        </a-card>
      </a-col>

      <a-col :xs="24" :lg="10">
        <a-card :bordered="false" class="radar-card">
          <template #title>
            <span>维度分析</span>
            <template v-if="selectedCustomer">
              <a-tag :color="getSegmentColor(selectedCustomer.segment)" class="ml-2">
                {{ getSegmentLabel(selectedCustomer.segment) }}
              </a-tag>
            </template>
          </template>
          <div v-if="selectedCustomer" class="radar-body">
            <div class="radar-chart" ref="radarRef"></div>
            <a-divider style="margin: 8px 0" />
            <div class="dim-breakdown">
              <div
                v-for="(score, key) in selectedCustomer.dimensionScores"
                :key="key"
                class="dim-row"
              >
                <span class="dim-name">{{ getDimLabel(key as string) }}</span>
                <a-progress
                  :percent="score"
                  :stroke-color="getScoreColor(score)"
                  :format="() => Math.round(score) + '分'"
                  size="small"
                />
              </div>
            </div>
          </div>
          <div v-else class="radar-empty">
            <a-empty description="点击左侧客户查看详情" />
          </div>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
  import { onMounted, onUnmounted, ref, watch, nextTick } from 'vue'
  import { useRouter } from 'vue-router'
  import { message } from 'ant-design-vue'
  import { SyncOutlined } from '@ant-design/icons-vue'
  import * as echarts from 'echarts'
  import { aiApi } from '@/api/ai'
  import type { CustomerScore } from '@/types'

  const router = useRouter()
  const radarRef = ref<HTMLElement>()
  let radarChart: echarts.ECharts | null = null

  const allScores = ref<CustomerScore[]>([])
  const displayList = ref<CustomerScore[]>([])
  const selectedCustomerId = ref<number | null>(null)
  const selectedCustomer = ref<CustomerScore | null>(null)
  const segmentFilter = ref<string>('ALL')
  const loading = ref({ table: false })
  const runningScoring = ref(false)

  const segmentCounts = ref({ high: 0, growing: 0, inactive: 0 })
  const pagination = ref({
    current: 1,
    pageSize: 5,
    total: 0,
    showSizeChanger: false,
    showTotal: (total: number) => `共 ${total} 人`,
  })

  const columns = [
    { title: '#', key: 'rank', width: 40 },
    { title: '客户', key: 'customer', width: 150 },
    { title: '总分', key: 'score', width: 120 },
    { title: '分段', key: 'segment', width: 80 },
    { title: '礼品等级', key: 'giftLevel', width: 100 },
  ]

  function getRank(record: CustomerScore) {
    const idx = allScores.value.findIndex((s) => s.customerId === record.customerId)
    return idx >= 0 ? idx + 1 : '-'
  }

  function getScoreColor(score: number) {
    if (score >= 80) return '#52c41a'
    if (score >= 60) return '#faad14'
    return '#f5222d'
  }

  function getSegmentColor(seg: string) {
    switch (seg) {
      case 'HIGH_VALUE':
        return 'green'
      case 'GROWING':
        return 'orange'
      default:
        return 'default'
    }
  }

  function getSegmentLabel(seg: string) {
    switch (seg) {
      case 'HIGH_VALUE':
        return '高价值'
      case 'GROWING':
        return '成长'
      case 'INACTIVE':
        return '待激活'
      default:
        return seg
    }
  }

  function getGiftLevelLabel(level: number) {
    return ['未领取', '一级', '二级', '三级'][level] || `等级${level}`
  }

  async function loadData() {
    loading.value.table = true
    try {
      const pageRes = await aiApi.getCustomerScores({ page: 0, size: 999 })
      allScores.value = pageRes.content
      segmentCounts.value = {
        high: allScores.value.filter((s) => s.segment === 'HIGH_VALUE').length,
        growing: allScores.value.filter((s) => s.segment === 'GROWING').length,
        inactive: allScores.value.filter((s) => s.segment === 'INACTIVE').length,
      }
      applyFilters()
    } catch {
      message.error('加载失败')
    } finally {
      loading.value.table = false
    }
  }

  function applyFilters() {
    let filtered = allScores.value
    if (segmentFilter.value !== 'ALL') {
      filtered = filtered.filter((s) => s.segment === segmentFilter.value)
    }

    pagination.value.total = filtered.length
    const start = (pagination.value.current - 1) * pagination.value.pageSize
    displayList.value = filtered.slice(start, start + pagination.value.pageSize)
  }

  function handleFilterChange() {
    pagination.value.current = 1
    applyFilters()
  }

  function handleTableChange(pag: { current?: number }) {
    if (pag.current) pagination.value.current = pag.current
    applyFilters()
  }

  function handleRowClick(record: CustomerScore) {
    selectedCustomerId.value = record.customerId
    selectedCustomer.value = record
  }

  async function handleRunScoring() {
    runningScoring.value = true
    try {
      const result = await aiApi.runCustomerScoring()
      message.success(`评分完成，耗时 ${result.executionTimeMs}ms`)
      await loadData()
    } catch {
      message.error('评分执行失败')
    } finally {
      runningScoring.value = false
    }
  }

  function goToRecommendations() {
    router.push('/ai/customers/gift-recommendations')
  }
  function renderRadar(customer: CustomerScore) {
    if (!radarRef.value) return
    if (radarChart) radarChart.dispose()

    radarChart = echarts.init(radarRef.value)
    const dims = customer.dimensionScores || {}
    const indicators = Object.keys(dims).map((key) => ({
      name: getDimLabel(key),
      max: 100,
    }))
    const values = Object.values(dims)

    radarChart.setOption({
      radar: {
        indicator: indicators,
        shape: 'circle',
        center: ['50%', '50%'],
        radius: '65%',
      },
      series: [
        {
          type: 'radar',
          data: [{ value: values, name: customer.customerName }],
          areaStyle: { opacity: 0.2 },
          lineStyle: { width: 2 },
        },
      ],
    })
  }

  function getDimLabel(key: string): string {
    const map: Record<string, string> = {
      totalSpent: '消费总额',
      frequency: '消费频率',
      recency: '活跃度',
      giftLevel: '客户等级',
      tenure: '注册时长',
      referral: '推荐贡献',
    }
    return map[key] || key
  }

  watch(selectedCustomer, (val) => {
    if (val) nextTick(() => renderRadar(val))
  })

  onMounted(() => {
    loadData()
  })
  onUnmounted(() => {
    if (radarChart) radarChart.dispose()
  })
</script>

<style scoped>
  .page-container {
    padding: 20px;
    background: var(--bg-page);
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
  .page-header-actions {
    display: flex;
    gap: 8px;
    flex-shrink: 0;
  }
  .mb-6 {
    margin-bottom: 16px;
  }
  .ml-2 {
    margin-left: 8px;
  }

  .stat-card {
    border-radius: 8px;
    border-left: 4px solid transparent;
    cursor: default;
  }
  .stat-card :deep(.ant-card-body) {
    padding: 0;
  }
  .stat-body {
    padding: 16px 20px 12px;
  }
  .stat-inner {
    display: flex;
    justify-content: space-between;
    align-items: baseline;
  }
  .stat-value {
    font-size: 28px;
    font-weight: 700;
    line-height: 1.2;
  }
  .stat-label {
    font-size: 15px;
    color: var(--text-secondary);
    font-weight: 500;
  }
  .stat-footer {
    padding: 6px 20px;
    font-size: 13px;
    color: var(--text-tertiary);
    border-top: 1px solid var(--border-color);
  }
  .stat-high {
    border-left-color: #52c41a;
  }
  .stat-high .stat-value {
    color: #52c41a;
  }
  .stat-growing {
    border-left-color: #faad14;
  }
  .stat-growing .stat-value {
    color: #fa8c16;
  }
  .stat-inactive {
    border-left-color: #f5222d;
  }
  .stat-inactive .stat-value {
    color: #f5222d;
  }

  .name-cell {
    font-weight: 500;
    color: #1f2937;
  }
  .meta-cell {
    color: #9ca3af;
    font-size: 12px;
  }
  .rank-num {
    color: #9ca3af;
    font-feature-settings: 'tnum';
  }

  .score-bar {
    display: flex;
    align-items: center;
    gap: 8px;
  }
  .score-bar :deep(.ant-progress) {
    flex: 1;
  }
  .score-text {
    font-weight: 700;
    font-size: 15px;
    min-width: 32px;
    text-align: right;
  }

  .radar-card :deep(.ant-card-body) {
    display: flex;
    flex-direction: column;
    flex: 1;
    height: 100%;
  }
  .radar-body {
    flex: 1;
    display: flex;
    flex-direction: column;
  }
  .radar-chart {
    flex: 1;
    min-height: 280px;
  }
  .card-toolbar {
    padding: 0 0 12px;
    border-bottom: 1px solid #f0f0f0;
    margin-bottom: 12px;
  }
  .equal-row {
    display: flex;
    align-items: stretch;
  }
  .equal-row > :deep(.ant-col) {
    display: flex;
  }
  .equal-row > :deep(.ant-col) {
    display: flex;
  }
  .equal-row > :deep(.ant-col > .ant-card) {
    flex: 1;
  }
  .radar-card {
    display: flex;
    flex-direction: column;
  }
  .radar-empty {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .dim-breakdown {
    display: flex;
    flex-direction: column;
    gap: 6px;
    padding: 0 4px;
  }
  .dim-row {
    display: flex;
    align-items: center;
    gap: 8px;
  }
  .dim-name {
    font-size: 13px;
    color: var(--text-primary);
    min-width: 56px;
    flex-shrink: 0;
    font-weight: 500;
  }
  .dim-row :deep(.ant-progress) {
    flex: 1;
    margin-bottom: 0;
  }

  .row-selected {
    background-color: rgba(24, 144, 255, 0.1);
  }
  :deep(.ant-table-row) {
    cursor: pointer;
  }
</style>
