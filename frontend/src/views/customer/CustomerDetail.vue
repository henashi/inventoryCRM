<template>
  <div class="customer-detail-page">
    <div class="page-header">
      <div>
        <h1 class="page-title">客户详情</h1>
        <p class="page-subtitle">查看客户档案、推荐关系与最近礼品记录。</p>
      </div>
      <a-space wrap>
        <a-button @click="goBack">返回</a-button>
        <a-button v-if="customer" @click="goToEdit">编辑客户</a-button>
        <a-button v-if="customer" @click="goToGiftLogs">查看礼品记录</a-button>
        <a-button v-if="customer" type="primary" @click="issueGift">发放礼品</a-button>
      </a-space>
    </div>

    <a-card>
      <a-spin :spinning="pageLoading">
        <a-result
          v-if="pageError"
          status="error"
          title="客户详情加载失败"
          :sub-title="pageError"
        >
          <template #extra>
            <a-space>
              <a-button @click="goBack">返回</a-button>
              <a-button type="primary" @click="reloadPage">重新加载</a-button>
            </a-space>
          </template>
        </a-result>

        <a-result
          v-else-if="!customer"
          status="404"
          title="未找到客户"
          sub-title="该客户可能已被删除，或当前链接无效。"
        >
          <template #extra>
            <a-space>
              <a-button @click="goBack">返回</a-button>
              <a-button type="primary" @click="reloadPage">重新加载</a-button>
            </a-space>
          </template>
        </a-result>

        <template v-else>
          <div class="summary-hero">
            <div class="hero-main">
              <a-avatar :size="72" :style="{ backgroundColor: getCustomerColor(customer.name) }">
                {{ getFirstChar(customer.name) }}
              </a-avatar>
              <div>
                <div class="hero-title-row">
                  <h2>{{ customer.name }}</h2>
                  <a-tag :color="giftLevelColors[customer.giftLevel]">
                    {{ getGiftLevelText(customer.giftLevel) }}
                  </a-tag>
                  <a-tag :color="customer.status === 1 ? 'green' : 'red'">
                    {{ customer.status === 1 ? '正常' : '停用' }}
                  </a-tag>
                </div>
                <div class="hero-meta">
                  <span>{{ customer.phone }}</span>
                  <span>{{ customer.email || '未填写邮箱' }}</span>
                  <span>注册于 {{ formatDateTime(customer.registeredAt || customer.createdAt) }}</span>
                </div>
              </div>
            </div>

            <div class="hero-stats">
              <div class="hero-stat-item">
                <span class="hero-stat-label">礼品记录</span>
                <strong>{{ relatedGiftLogs.length }}</strong>
              </div>
              <div class="hero-stat-item">
                <span class="hero-stat-label">已发放</span>
                <strong>{{ issuedGiftCount }}</strong>
              </div>
              <div class="hero-stat-item">
                <span class="hero-stat-label">待处理</span>
                <strong>{{ pendingGiftCount }}</strong>
              </div>
            </div>
          </div>

          <a-row :gutter="[16, 16]">
            <a-col :xs="24" :xl="16">
              <a-card title="档案信息" class="section-card">
                <a-descriptions :column="2" bordered>
                  <a-descriptions-item label="姓名">{{ customer.name }}</a-descriptions-item>
                  <a-descriptions-item label="手机号">{{ customer.phone }}</a-descriptions-item>
                  <a-descriptions-item label="邮箱">{{ customer.email || '--' }}</a-descriptions-item>
                  <a-descriptions-item label="性别">{{ getGenderText(customer.gender) }}</a-descriptions-item>
                  <a-descriptions-item label="生日">{{ formatDate(customer.birthday) }}</a-descriptions-item>
                  <a-descriptions-item label="礼品等级">{{ getGiftLevelText(customer.giftLevel) }}</a-descriptions-item>
                  <a-descriptions-item label="地址" :span="2">{{ customer.address || '--' }}</a-descriptions-item>
                  <a-descriptions-item label="推荐人" :span="2">
                    <template v-if="referrerSummary">
                      <a-space>
                        <span>{{ referrerSummary }}</span>
                        <a-button v-if="customer.referrerId" type="link" size="small" @click="viewReferrer">
                          查看推荐人
                        </a-button>
                      </a-space>
                    </template>
                    <span v-else>--</span>
                  </a-descriptions-item>
                  <a-descriptions-item label="注册时间">{{ formatDateTime(customer.registeredAt || customer.createdAt) }}</a-descriptions-item>
                  <a-descriptions-item label="最近更新">{{ formatDateTime(customer.updatedAt) }}</a-descriptions-item>
                  <a-descriptions-item label="备注" :span="2">{{ customer.remark || '--' }}</a-descriptions-item>
                </a-descriptions>
              </a-card>
            </a-col>

            <a-col :xs="24" :xl="8">
              <a-card title="关系与摘要" class="section-card">
                <div class="summary-list">
                  <div class="summary-item">
                    <span class="summary-label">客户状态</span>
                    <a-tag :color="customer.status === 1 ? 'green' : 'red'">
                      {{ customer.status === 1 ? '正常' : '停用' }}
                    </a-tag>
                  </div>
                  <div class="summary-item">
                    <span class="summary-label">礼品等级</span>
                    <a-tag :color="giftLevelColors[customer.giftLevel]">
                      {{ getGiftLevelText(customer.giftLevel) }}
                    </a-tag>
                  </div>
                  <div class="summary-item">
                    <span class="summary-label">推荐来源</span>
                    <span>{{ referrerSummary || '暂无推荐人' }}</span>
                  </div>
                  <div class="summary-item">
                    <span class="summary-label">联系方式</span>
                    <span>{{ customer.phone }}</span>
                  </div>
                </div>
                <a-divider />
                <a-space direction="vertical" style="width: 100%">
                  <a-button block @click="goToEdit">编辑客户</a-button>
                  <a-button block @click="goToGiftLogs">查看礼品记录</a-button>
                  <a-button block type="primary" @click="issueGift">继续发放礼品</a-button>
                </a-space>
              </a-card>
            </a-col>

            <a-col :xs="24">
              <a-card title="最近礼品记录" class="section-card">
                <template #extra>
                  <a-button type="link" @click="goToGiftLogs">查看全部</a-button>
                </template>

                <a-alert
                  v-if="giftLogError"
                  type="warning"
                  show-icon
                  :message="giftLogError"
                  class="section-alert"
                />

                <a-spin :spinning="giftLogLoading">
                  <a-empty v-if="!relatedGiftLogs.length" description="暂无礼品记录" />
                  <div v-else class="gift-log-list">
                    <div v-for="giftLog in relatedGiftLogs" :key="giftLog.id || `${giftLog.giftId}-${giftLog.createdTime}`" class="gift-log-item">
                      <div>
                        <div class="gift-log-title-row">
                          <strong>{{ giftLog.giftName }}</strong>
                          <a-tag :color="getGiftLogStatusColor(giftLog.status)">
                            {{ getGiftLogStatusText(giftLog.status) }}
                          </a-tag>
                        </div>
                        <div class="gift-log-meta">
                          <span>数量 {{ giftLog.quantity }}</span>
                          <span>发放时间 {{ formatDateTime(giftLog.issuedAt || giftLog.createdTime) }}</span>
                          <span>操作人 {{ giftLog.operator || '--' }}</span>
                        </div>
                        <div class="gift-log-note">{{ giftLog.issueNotes || '暂无发放说明' }}</div>
                      </div>
                    </div>
                  </div>
                </a-spin>
              </a-card>
            </a-col>
          </a-row>
        </template>
      </a-spin>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import { giftLogApi } from '@/api/giftLog'
import { useCustomerStore } from '@/stores/customer'
import type { Customer, GiftLogDTO } from '@/types'
import { buildCustomerDetailBackTarget } from '@/utils/featureEnhancements'

const route = useRoute()
const router = useRouter()
const customerStore = useCustomerStore()

const pageLoading = ref(false)
const pageError = ref('')
const giftLogLoading = ref(false)
const giftLogError = ref('')
const customer = ref<Customer | null>(null)
const referrer = ref<Customer | null>(null)
const relatedGiftLogs = ref<GiftLogDTO[]>([])

const giftLevelColors: Record<number, string> = {
  0: 'default',
  1: 'blue',
  2: 'green',
  3: 'orange',
}

const customerId = computed(() => {
  const value = Number(route.params.id)
  return Number.isFinite(value) && value > 0 ? value : undefined
})
const referrerSummary = computed(() => {
  if (referrer.value) {
    return `${referrer.value.name}${referrer.value.phone ? `（${referrer.value.phone}）` : ''}`
  }

  return customer.value?.referrerName || ''
})
const issuedGiftCount = computed(() => relatedGiftLogs.value.filter((item) => item.status === 'ISSUED').length)
const pendingGiftCount = computed(() => relatedGiftLogs.value.filter((item) => item.status === 'PENDING').length)

const getFirstChar = (name: string) => name.charAt(0).toUpperCase()
const getCustomerColor = (name: string) => {
  const colors = ['#1677ff', '#52c41a', '#722ed1', '#fa8c16']
  return colors[name.charCodeAt(0) % colors.length]
}
const formatDate = (value?: string) => value ? dayjs(value).format('YYYY-MM-DD') : '--'
const formatDateTime = (value?: string) => value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '--'
const getGenderText = (gender?: 0 | 1) => {
  if (gender === 1) return '男'
  if (gender === 0) return '女'
  return '--'
}
const getGiftLevelText = (level: number) => ['普通客户', '等级1', '等级2', '等级3'][level] || '普通客户'
const getGiftLogStatusText = (status: GiftLogDTO['status']) => {
  if (status === 'PENDING') return '待发放'
  if (status === 'CANCELLED') return '已取消'
  return '已发放'
}
const getGiftLogStatusColor = (status: GiftLogDTO['status']) => {
  if (status === 'PENDING') return 'orange'
  if (status === 'CANCELLED') return 'red'
  return 'green'
}

const goBack = () => {
  router.push(buildCustomerDetailBackTarget(route.query as Record<string, unknown>))
}

const goToEdit = () => {
  message.info('本轮已预留编辑入口，先复用客户列表页的编辑弹窗能力')
  router.push('/customers')
}

const goToGiftLogs = () => {
  if (!customer.value?.id) {
    return
  }

  router.push({
    path: '/gift-logs',
    query: {
      customerId: String(customer.value.id),
      customerName: customer.value.name,
      from: 'customer-detail',
    },
  })
}

const issueGift = () => {
  if (!customer.value?.id) {
    return
  }

  router.push({
    path: '/gift-logs',
    query: {
      customerId: String(customer.value.id),
      customerName: customer.value.name,
      from: 'customer-detail',
      openCreate: '1',
    },
  })
}

const viewReferrer = () => {
  if (!customer.value?.referrerId || !customer.value.id) {
    return
  }

  router.push({
    path: `/customers/${customer.value.referrerId}`,
    query: {
      from: 'customer-detail',
      customerId: String(customer.value.id),
    },
  })
}

const loadGiftLogs = async (id: number) => {
  giftLogLoading.value = true
  giftLogError.value = ''

  try {
    const page = await giftLogApi.getLogsByCustomerId(id, {
      page: 0,
      size: 5,
    })
    relatedGiftLogs.value = page.content || []
  } catch {
    relatedGiftLogs.value = []
    giftLogError.value = '礼品记录暂时无法加载，仍可通过下方入口进入礼品发放页查看。'
  } finally {
    giftLogLoading.value = false
  }
}

const loadPage = async () => {
  if (!customerId.value) {
    customer.value = null
    referrer.value = null
    relatedGiftLogs.value = []
    giftLogError.value = ''
    pageError.value = '客户编号无效，请从客户列表重新进入。'
    return
  }

  pageLoading.value = true
  pageError.value = ''
  giftLogError.value = ''
  customer.value = null
  referrer.value = null
  relatedGiftLogs.value = []

  try {
    const detail = await customerStore.getCustomer(customerId.value)
    customer.value = detail

    await Promise.allSettled([
      loadGiftLogs(customerId.value),
      detail.referrerId
        ? customerStore.findCustomerById(detail.referrerId).then((record) => {
          referrer.value = record || null
        })
        : Promise.resolve(null),
    ])
  } catch (error: any) {
    const status = error?.response?.status
    if (status === 404) {
      customer.value = null
      pageError.value = ''
    } else {
      pageError.value = error?.response?.data?.message || '加载客户详情失败，请稍后重试。'
    }
  } finally {
    pageLoading.value = false
  }
}

const reloadPage = () => {
  void loadPage()
}

watch(() => route.params.id, () => {
  void loadPage()
}, { immediate: true })
</script>

<style scoped>
.customer-detail-page {
  padding: 20px;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.page-title {
  margin: 0;
  font-size: 28px;
  color: #262626;
}

.page-subtitle {
  margin: 8px 0 0;
  color: #8c8c8c;
}

.summary-hero {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  padding: 8px 0 24px;
}

.hero-main {
  display: flex;
  align-items: center;
  gap: 16px;
}

.hero-title-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}

.hero-title-row h2 {
  margin: 0;
}

.hero-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  color: #8c8c8c;
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(88px, 1fr));
  gap: 12px;
  min-width: 300px;
}

.hero-stat-item {
  padding: 16px;
  border-radius: 12px;
  background: #fafafa;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.hero-stat-item strong {
  font-size: 24px;
  color: #262626;
}

.hero-stat-label {
  color: #8c8c8c;
}

.section-card {
  height: 100%;
}

.summary-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.summary-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.summary-label {
  color: #8c8c8c;
}

.section-alert {
  margin-bottom: 16px;
}

.gift-log-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.gift-log-item {
  padding: 16px;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
}

.gift-log-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.gift-log-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 8px;
  color: #8c8c8c;
}

.gift-log-note {
  margin-top: 8px;
  color: #595959;
}

@media (max-width: 992px) {
  .summary-hero {
    flex-direction: column;
  }

  .hero-stats {
    min-width: 0;
  }
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
  }

  .hero-stats {
    grid-template-columns: 1fr;
  }
}
</style>
