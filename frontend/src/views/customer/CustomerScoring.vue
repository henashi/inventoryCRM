<template>
  <div class="scoring-page">
    <div class="page-header">
      <div class="page-header-main">
        <h1 class="page-title">🤖 AI 客户评分</h1>
        <div class="page-subtitle">六维加权评分模型自动评估客户价值，精准匹配礼品推荐</div>
      </div>
      <div class="page-header-actions">
        <a-button :loading="runningScoring" type="primary" @click="handleRunScoring">
          <template #icon><SyncOutlined /></template>
          全量评分
        </a-button>
        <a-button @click="goToRecommendations">🎁 礼品推荐</a-button>
      </div>
    </div>

    <!-- 分段统计卡片 -->
    <a-row :gutter="[16, 16]" class="mb-6">
      <a-col :xs="8" :sm="6">
        <a-card class="segment-card segment-high" :bordered="false">
          <div class="seg-value">{{ segmentCounts.high }}</div>
          <div class="seg-label">🥇 高价值客户</div>
          <div class="seg-hint">≥ 80 分</div>
        </a-card>
      </a-col>
      <a-col :xs="8" :sm="6">
        <a-card class="segment-card segment-growing" :bordered="false">
          <div class="seg-value">{{ segmentCounts.growing }}</div>
          <div class="seg-label">🌱 成长客户</div>
          <div class="seg-hint">60-79 分</div>
        </a-card>
      </a-col>
      <a-col :xs="8" :sm="6">
        <a-card class="segment-card segment-inactive" :bordered="false">
          <div class="seg-value">{{ segmentCounts.inactive }}</div>
          <div class="seg-label">💤 待激活客户</div>
          <div class="seg-hint">&lt; 60 分</div>
        </a-card>
      </a-col>
      <a-col :xs="24" :sm="6">
        <a-card class="segment-card segment-birthday" :bordered="false" @click="showBirthdayOnly = !showBirthdayOnly" :style="{ cursor: 'pointer' }">
          <div class="seg-value">🎂 {{ birthdayCustomers.length }}</div>
          <div class="seg-label">未来7天生日的客户</div>
          <div class="seg-hint">点击{{ showBirthdayOnly ? '取消' : '' }}筛选</div>
        </a-card>
      </a-col>
    </a-row>

    <!-- 分段过滤标签 -->
    <a-card :bordered="false" class="mb-6">
      <a-radio-group v-model:value="segmentFilter" button-style="solid" @change="handleFilterChange">
        <a-radio-button value="ALL">全部</a-radio-button>
        <a-radio-button value="HIGH_VALUE">🥇 高价值</a-radio-button>
        <a-radio-button value="GROWING">🌱 成长</a-radio-button>
        <a-radio-button value="INACTIVE">💤 待激活</a-radio-button>
      </a-radio-group>
    </a-card>

    <a-row :gutter="16">
      <!-- 左侧：评分排行榜 -->
      <a-col :xs="24" :lg="14">
        <a-card title="评分排行榜" :bordered="false">
          <a-table
            row-key="customerId"
            :columns="columns"
            :data-source="displayList"
            :pagination="pagination"
            :loading="loading.table"
            size="small"
            :scroll="{ y: 480 }"
            @change="handleTableChange"
            @row-click="handleRowClick"
            :row-class="(record: CustomerScore) => record.customerId === selectedCustomerId ? 'selected-row' : ''"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'rank'">
                {{ getRank(record) }}
              </template>
              <template v-if="column.key === 'customer'">
                <div>
                  <div class="font-medium">{{ record.customerName }}</div>
                  <div class="text-xs text-gray-400">{{ record.phone }}</div>
                </div>
              </template>
              <template v-if="column.key === 'score'">
                <div class="score-cell">
                  <a-progress
                    :percent="record.totalScore"
                    :stroke-color="getScoreColor(record.totalScore)"
                    :show-info="false"
                    size="small"
                  />
                  <span class="score-value" :style="{ color: getScoreColor(record.totalScore) }">
                    {{ record.totalScore }}
                  </span>
                </div>
              </template>
              <template v-if="column.key === 'segment'">
                <a-tag :color="getSegmentColor(record.segment)">{{ getSegmentLabel(record.segment) }}</a-tag>
              </template>
              <template v-if="column.key === 'giftLevel'">
                <a-rate :value="record.giftLevel" :count="3" disabled />
              </template>
              <template v-if="column.key === 'birthday'">
                <span v-if="record.isBirthdaySoon" class="birthday-badge">🎂 {{ record.daysToBirthday }}天后</span>
                <span v-else class="text-gray-400">—</span>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-col>

      <!-- 右侧：雷达图 -->
      <a-col :xs="24" :lg="10">
        <a-card title="六维评分雷达图" :bordered="false">
          <template #extra>
            <a-tag v-if="selectedCustomer" :color="getSegmentColor(selectedCustomer.segment)">
              {{ getSegmentLabel(selectedCustomer.segment) }}
            </a-tag>
          </template>
          <div v-if="selectedCustomer" class="radar-container">
            <div class="radar-chart" ref="radarRef"></div>
            <a-descriptions :column="1" size="small" class="mt-4">
              <a-descriptions-item label="客户">
                {{ selectedCustomer.customerName }} ({{ selectedCustomer.phone }})
              </a-descriptions-item>
              <a-descriptions-item label="总分">
                <span :style="{ color: getScoreColor(selectedCustomer.totalScore), fontWeight: 600 }">
                  {{ selectedCustomer.totalScore }}
                </span>
              </a-descriptions-item>
              <a-descriptions-item label="礼品等级">
                {{ getGiftLevelLabel(selectedCustomer.giftLevel) }}
              </a-descriptions-item>
              <a-descriptions-item label="生日提醒">
                <span v-if="selectedCustomer.isBirthdaySoon">🎂 {{ selectedCustomer.daysToBirthday }} 天后生日</span>
                <span v-else>无</span>
              </a-descriptions-item>
            </a-descriptions>
          </div>
          <div v-else class="radar-empty">
            <a-empty description="请从左侧选择一个客户" />
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
const showBirthdayOnly = ref(false)

const loading = ref({ table: false })
const runningScoring = ref(false)

const segmentCounts = ref({ high: 0, growing: 0, inactive: 0 })
const birthdayCustomers = ref<any[]>([])

const pagination = ref({
  current: 1,
  pageSize: 8,
  total: 0,
  showSizeChanger: false,
  showTotal: (total: number) => `共 ${total} 人`,
})

const columns = [
  { title: '#', key: 'rank', width: 40 },
  { title: '客户', key: 'customer', width: 160 },
  { title: '总分', key: 'score', width: 120 },
  { title: '分段', key: 'segment', width: 90 },
  { title: '礼品等级', key: 'giftLevel', width: 100 },
  { title: '生日', key: 'birthday', width: 80 },
]

const getRank = (record: CustomerScore) => {
  const idx = allScores.value.findIndex(s => s.customerId === record.customerId)
  return idx >= 0 ? idx + 1 : '-'
}

const getScoreColor = (score: number) => {
  if (score >= 80) return '#52c41a'
  if (score >= 60) return '#faad14'
  return '#f5222d'
}

const getSegmentColor = (seg: string) => {
  switch (seg) {
    case 'HIGH_VALUE': return 'green'
    case 'GROWING': return 'orange'
    default: return 'default'
  }
}

const getSegmentLabel = (seg: string) => {
  switch (seg) {
    case 'HIGH_VALUE': return '高价值'
    case 'GROWING': return '成长'
    case 'INACTIVE': return '待激活'
    default: return seg
  }
}

const getGiftLevelLabel = (level: number) => {
  const map = ['未领取', '一级', '二级', '三级']
  return map[level] || `等级${level}`
}

const loadData = async () => {
  loading.value.table = true
  try {
    const [pageRes, birthdayRes] = await Promise.all([
      aiApi.getCustomerScores({ page: 0, size: 999 }),
      aiApi.getUpcomingBirthdayCustomers(),
    ])
    allScores.value = pageRes.content
    birthdayCustomers.value = birthdayRes

    segmentCounts.value = {
      high: allScores.value.filter(s => s.segment === 'HIGH_VALUE').length,
      growing: allScores.value.filter(s => s.segment === 'GROWING').length,
      inactive: allScores.value.filter(s => s.segment === 'INACTIVE').length,
    }

    applyFilters()
  } catch {
    message.error('加载评分数据失败')
  } finally {
    loading.value.table = false
  }
}

const applyFilters = () => {
  let filtered = allScores.value
  if (segmentFilter.value !== 'ALL') {
    filtered = filtered.filter(s => s.segment === segmentFilter.value)
  }
  if (showBirthdayOnly.value) {
    filtered = filtered.filter(s => s.isBirthdaySoon)
  }
  pagination.value.total = filtered.length
  const start = (pagination.value.current - 1) * pagination.value.pageSize
  displayList.value = filtered.slice(start, start + pagination.value.pageSize)
}

const handleFilterChange = () => {
  pagination.value.current = 1
  applyFilters()
}

const handleTableChange = (pag: { current?: number }) => {
  if (pag.current) pagination.value.current = pag.current
  applyFilters()
}

const handleRowClick = (record: CustomerScore) => {
  selectedCustomerId.value = record.customerId
  selectedCustomer.value = record
  nextTick(() => renderRadar(record))
}

const handleRunScoring = async () => {
  runningScoring.value = true
  try {
    const result = await aiApi.runCustomerScoring()
    message.success(`全量评分完成，耗时 ${result.executionTimeMs}ms`)
    await loadData()
  } catch {
    message.error('评分执行失败')
  } finally {
    runningScoring.value = false
  }
}

const goToRecommendations = () => {
  router.push('/ai/customers/gift-recommendations')
}

const renderRadar = (customer: CustomerScore) => {
  if (!radarRef.value) return
  if (radarChart) radarChart.dispose()

  radarChart = echarts.init(radarRef.value)
  const dims = customer.dimensionScores || {}
  const indicators = Object.keys(dims).map(key => ({
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
    series: [{
      type: 'radar',
      data: [{ value: values, name: customer.customerName }],
      areaStyle: { opacity: 0.2 },
      lineStyle: { width: 2 },
    }],
  })
}

const getDimLabel = (key: string): string => {
  const map: Record<string, string> = {
    giftLevel: '客户等级',
    recency: '活跃度',
    tenure: '注册时长',
    frequency: '领取频率',
    referral: '推荐贡献',
    birthday: '生日临近',
  }
  return map[key] || key
}

watch(showBirthdayOnly, () => {
  pagination.value.current = 1
  applyFilters()
})

onMounted(() => { loadData() })
onUnmounted(() => { if (radarChart) radarChart.dispose() })
</script>

<style scoped>
.scoring-page {
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
.page-title { font-size: 24px; font-weight: 700; margin: 0; color: #111827; }
.page-subtitle { font-size: 14px; color: #6b7280; margin-top: 4px; }
.mb-6 { margin-bottom: 16px; }
.font-medium { font-weight: 500; }
.text-xs { font-size: 12px; }
.text-gray-400 { color: #9ca3af; }

.segment-card { border-radius: 8px; text-align: center; }
.segment-card :deep(.ant-card-body) { padding: 16px; }
.seg-value { font-size: 28px; font-weight: 700; line-height: 1.2; }
.seg-label { font-size: 14px; margin-top: 4px; }
.seg-hint { font-size: 11px; color: #9ca3af; margin-top: 2px; }
.segment-high .seg-value { color: #52c41a; }
.segment-growing .seg-value { color: #fa8c16; }
.segment-inactive .seg-value { color: #f5222d; }
.segment-birthday:hover { border-color: #1890ff; }
.segment-birthday .seg-value { color: #1890ff; }

.score-cell { display: flex; align-items: center; gap: 8px; }
.score-cell :deep(.ant-progress) { flex: 1; }
.score-value { font-weight: 700; font-size: 16px; min-width: 32px; text-align: right; }

.radar-container { padding: 8px; }
.radar-chart { width: 100%; height: 320px; }
.radar-empty { height: 320px; display: flex; align-items: center; justify-content: center; }
.mt-4 { margin-top: 16px; }
.birthday-badge { font-weight: 500; color: #1890ff; }

.selected-row { background-color: #e6f7ff !important; }
:deep(.ant-table-row) { cursor: pointer; }
</style>
