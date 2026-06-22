<template>
  <div class="dashboard-container">
    <a-card class="welcome-card row-card mb-6">
      <div class="user-area">
        <a-avatar :size="40" class="avatar-mini">
          <template #icon>
            <user-outlined />
          </template>
        </a-avatar>
        <div class="user-meta">
          <div class="user-name">{{ userName }}</div>
          <div class="user-role">{{ userRoleText }}</div>
        </div>
        <a-dropdown>
          <a-button class="account-trigger" type="default">
            账号
            <down-outlined />
          </a-button>
          <template #overlay>
            <a-menu @click="handleAccountMenuClick">
              <a-menu-item key="profile">个人资料</a-menu-item>
              <a-menu-item key="password">修改密码</a-menu-item>
              <a-menu-divider />
              <a-menu-item key="logout">退出登录</a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
      </div>
      <div class="flex items-center justify-between">
        <div class="welcome-inner">
          <div class="welcome-left">
            <h1 class="welcome-title">欢迎回来，{{ userName }}！ <span class="wave">👋</span></h1>
            <p class="welcome-sub">{{ greetingMessage }}，今天是 {{ currentDate }}</p>
          </div>
        </div>
      </div>
    </a-card>

    <div class="cards-panel">
      <a-row :gutter="[16, 16]" class="row-card mb-6">
        <a-col :xs="24" :sm="12" :md="8" v-for="stat in visibleStats" :key="stat.title">
          <a-card class="stat-card clickable" :class="stat.type" hoverable @click="handleStatClick(stat)">
            <div class="stat-card-content">
              <div class="stat-copy">
                <div class="stat-primary-line">
                  <span class="stat-title">{{ stat.title }}</span>
                  <span class="stat-value-inline">{{ stat.value }}</span>
                </div>
                <div class="stat-secondary-line text-xs" :class="stat.trendClass">{{ stat.change }}</div>
              </div>
              <div class="stat-icon" :class="stat.type">
                <component :is="stat.icon" />
              </div>
            </div>
          </a-card>
        </a-col>
      </a-row>

      <a-row :gutter="[16, 16]" class="row-card mb-6">
        <a-col :xs="24" :lg="16">
          <a-card title="客户增长趋势" class="h-full" :loading="loading.trend">
            <template #extra>
              <a-select v-model:value="chartPeriod" size="small" style="width: 100px">
                <a-select-option value="week">本周</a-select-option>
                <a-select-option value="month">本月</a-select-option>
                <a-select-option value="quarter">本季</a-select-option>
              </a-select>
            </template>
            <div class="h-64">
              <LineChart :data="chartData" />
            </div>
          </a-card>
        </a-col>

        <a-col :xs="24" :lg="8">
          <a-card title="礼品等级分布" class="h-full" :loading="loading.stats">
            <div class="h-64">
              <PieChart :data="giftDistribution" />
            </div>
          </a-card>
        </a-col>
      </a-row>

      <a-row :gutter="[16, 16]" class="row-card dashboard-equal-row">
        <a-col v-if="showCustomerSection" :xs="24" :lg="12" class="dashboard-equal-col">
          <a-card title="最近添加的客户" class="h-full dashboard-equal-card">
            <template #extra>
              <a-button type="link" @click="router.push('/customers')">查看全部</a-button>
            </template>
            <a-list
              :data-source="recentCustomers"
              :loading="loading.customers"
              item-layout="horizontal"
              size="small"
            >
              <template #renderItem="{ item }">
                <a-list-item class="hover:bg-gray-50 cursor-pointer" @click="viewCustomer(item)">
                  <a-list-item-meta>
                    <template #title>
                      <div class="flex items-center">
                        {{ item.name }}
                        <a-tag
                          v-if="item.giftLevel > 0"
                          :color="giftLevelColors[item.giftLevel]"
                          class="ml-2"
                        >
                          等级{{ item.giftLevel }}
                        </a-tag>
                      </div>
                    </template>
                    <template #description>
                      <div class="text-gray-500 text-sm">
                        {{ item.phone }} • {{ formatDate(item.createdAt || item.registeredAt) }}
                      </div>
                    </template>
                  </a-list-item-meta>
                  <template #actions>
                    <a-button type="link" size="small" @click.stop="callCustomer(item.phone)">
                      <phone-outlined />
                    </a-button>
                  </template>
                </a-list-item>
              </template>
            </a-list>
          </a-card>
        </a-col>

        <a-col :xs="24" :lg="showCustomerSection ? 12 : 24" class="dashboard-equal-col">
          <a-card title="低库存商品" class="h-full dashboard-equal-card">
            <template #extra>
              <a-button type="link" @click="router.push('/products')">查看全部</a-button>
            </template>
            <a-list
              :data-source="lowStockProducts"
              :loading="loading.inventory"
              item-layout="horizontal"
              size="small"
            >
              <template #renderItem="{ item }">
                <a-list-item class="hover:bg-gray-50 cursor-pointer" @click="viewLowStockProduct(item)">
                  <a-list-item-meta>
                    <template #title>
                      <div class="flex items-center justify-between">
                        <span>{{ item.name }}</span>
                        <a-tag :color="getStockColor(item)" class="ml-2">
                          {{ item.currentStock }} {{ item.unit }}
                        </a-tag>
                      </div>
                    </template>
                    <template #description>
                      <div class="text-gray-500 text-sm">
                        {{ item.code }} • 安全库存: {{ item.safeStock }} {{ item.unit }}
                      </div>
                    </template>
                  </a-list-item-meta>
                  <template #actions>
                    <a-button v-if="showInventorySection" type="link" size="small" @click.stop="goToRestock(item)">
                      补货
                    </a-button>
                  </template>
                </a-list-item>
              </template>
            </a-list>
          </a-card>
        </a-col>
      </a-row>

      <a-card title="快捷操作" class="row-card mt-6">
        <div class="quick-actions-grid">
          <div
            v-for="action in visibleQuickActions"
            :key="action.title"
            class="quick-action-grid-item"
          >
            <a-card
              hoverable
              class="quick-action-card"
              @click="handleQuickAction(action)"
            >
              <div class="quick-action-content">
                <div class="action-icon" :class="action.type">
                  <component :is="action.icon" />
                </div>
                <div class="quick-action-copy">
                  <div class="quick-action-title">{{ action.title }}</div>
                  <div class="quick-action-description">{{ action.description }}</div>
                </div>
              </div>
            </a-card>
          </div>
        </div>
      </a-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  BarChartOutlined,
  DownOutlined,
  FileAddOutlined,
  GiftOutlined,
  InboxOutlined,
  PhoneOutlined,
  PlusCircleOutlined,
  SettingOutlined,
  ShoppingOutlined,
  TeamOutlined,
  UserAddOutlined,
  UserOutlined,
} from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import PieChart from '@/components/charts/PieChart.vue'
import LineChart from '@/components/charts/LineChart.vue'
import { customerApi } from '@/api/customer'
import { giftLogApi } from '@/api/giftLog'
import { productApi } from '@/api/product'
import { useAuthStore } from '@/stores/auth'
import type { Customer, GiftLogDTO, PageResult, Product } from '@/types'
import {
  filterDashboardStats,
  filterQuickActions,
  shouldShowCustomerSection,
  shouldShowInventorySection,
} from '@/router/accessControl'
import {
  buildDashboardStats,
  buildDashboardTrendChart,
  buildGiftDistribution,
  type CustomerStatistics,
  type ProductStockStatistics,
  type TrendPeriod,
} from '@/utils/featureEnhancements'

type DashboardStat = {
  title: string
  value: string
  change: string
  icon: unknown
  trendClass: string
  type: 'customers' | 'products' | 'inventory'
}

type QuickAction = {
  title: string
  description: string
  icon: unknown
  type: string
  action: 'addCustomer' | 'addInventory' | 'createOrder' | 'distributeGift' | 'exportReport' | 'systemSettings'
}

const router = useRouter()
const authStore = useAuthStore()

const currentDate = ref(dayjs().format('YYYY年MM月DD日'))
const chartPeriod = ref<TrendPeriod>('month')
const loading = ref({
  customers: false,
  inventory: false,
  stats: false,
  trend: false,
})

const customerStats = ref<CustomerStatistics>({
  totalCustomers: 0,
  normalCustomers: 0,
  disabledCustomers: 0,
  giftLevelDistribution: {},
})
const stockStats = ref<ProductStockStatistics>({
  totalProducts: 0,
  activeProducts: 0,
  lowStockProducts: 0,
  outOfStockProducts: 0,
  totalStockQuantity: 0,
  totalStockValue: 0,
})
const recentCustomers = ref<Customer[]>([])
const lowStockProducts = ref<Product[]>([])
const trendCustomers = ref<Customer[]>([])
const trendGiftLogs = ref<GiftLogDTO[]>([])

const chartData = computed(() => buildDashboardTrendChart({
  period: chartPeriod.value,
  customers: trendCustomers.value,
  giftLogs: trendGiftLogs.value,
}))

const giftLevelColors: Record<number, string> = {
  1: 'blue',
  2: 'green',
  3: 'orange',
}

const quickActions = ref<QuickAction[]>([
  {
    title: '新增客户',
    description: '快速添加新客户',
    icon: UserAddOutlined,
    type: 'primary',
    action: 'addCustomer',
  },
  {
    title: '商品入库',
    description: '商品进货登记',
    icon: PlusCircleOutlined,
    type: 'success',
    action: 'addInventory',
  },
  {
    title: '创建订单',
    description: '新建销售订单',
    icon: FileAddOutlined,
    type: 'warning',
    action: 'createOrder',
  },
  {
    title: '礼品发放',
    description: '发放客户礼品',
    icon: GiftOutlined,
    type: 'danger',
    action: 'distributeGift',
  },
  {
    title: '报表导出',
    description: '导出统计报表',
    icon: BarChartOutlined,
    type: 'info',
    action: 'exportReport',
  },
  {
    title: '系统设置',
    description: '系统参数配置',
    icon: SettingOutlined,
    type: 'default',
    action: 'systemSettings',
  },
])

const userName = computed(() => authStore.userName || '管理员')
const userRole = computed(() => authStore.userRole || 'ADMIN')
const greetingMessage = computed(() => {
  const hour = new Date().getHours()
  if (hour < 12) return '早上好'
  if (hour < 18) return '下午好'
  return '晚上好'
})
const userRoleText = computed(() => ({
  ADMIN: '系统管理员',
  MANAGER: '经理',
  USER: '普通用户',
}[userRole.value] || '用户'))

const stats = computed<DashboardStat[]>(() => {
  const baseStats = buildDashboardStats(customerStats.value, stockStats.value)
  const customerCard = baseStats[0]!
  const productCard = baseStats[1]!
  const inventoryCard = baseStats[2]!

  return [
    {
      ...customerCard,
      icon: TeamOutlined,
      trendClass: 'text-green-500',
      type: 'customers',
    },
    {
      ...productCard,
      icon: ShoppingOutlined,
      trendClass: 'text-green-500',
      type: 'products',
    },
    {
      ...inventoryCard,
      icon: InboxOutlined,
      trendClass: 'text-orange-500',
      type: 'inventory',
    },
  ]
})
const giftDistribution = computed(() => buildGiftDistribution(customerStats.value))
const visibleStats = computed(() => filterDashboardStats(stats.value, userRole.value))
const visibleQuickActions = computed(() => filterQuickActions(quickActions.value, userRole.value))
const showCustomerSection = computed(() => shouldShowCustomerSection(userRole.value))
const showInventorySection = computed(() => shouldShowInventorySection(userRole.value))

const resolveTrendRequestSize = (period: TrendPeriod) => {
  switch (period) {
    case 'week':
      return 30
    case 'quarter':
      return 240
    default:
      return 120
  }
}

const handleLogout = async () => {
  await authStore.logout()
  router.replace('/login')
}

const handleAccountMenuClick = ({ key }: { key: string }) => {
  if (key === 'profile') {
    router.push('/account')
    return
  }

  if (key === 'password') {
    router.push({ path: '/account', query: { tab: 'password' } })
    return
  }

  if (key === 'logout') {
    void handleLogout()
  }
}

const formatDate = (dateStr?: string) => {
  if (!dateStr) return '--'
  return dayjs(dateStr).format('MM-DD')
}

const getStockColor = (product: Product) => {
  if (product.currentStock <= 0) return 'red'
  if (product.currentStock < product.safeStock) return 'orange'
  return 'green'
}

const viewCustomer = (customer: Customer) => {
  router.push({
    path: `/customers/${customer.id}`,
    query: { from: 'dashboard' },
  })
}

const viewLowStockProduct = (product: Product) => {
  router.push(`/products/${product.code || product.name}`)
}

const goToRestock = (product: Product) => {
  router.push({
    path: '/inventory',
    query: { action: 'in', productId: product.id },
  })
}

const callCustomer = (phone: string) => {
  Modal.confirm({
    title: '拨打电话',
    content: `确定要拨打 ${phone} 吗？`,
    onOk: () => {
      window.location.href = `tel:${phone}`
    },
  })
}

const handleQuickAction = (action: QuickAction) => {
  switch (action.action) {
    case 'addCustomer':
      router.push({ path: '/customers', query: { openCreate: '1' } })
      break
    case 'addInventory':
      router.push({ path: '/inventory', query: { action: 'in' } })
      break
    case 'distributeGift':
      router.push({ path: '/gift-logs', query: { openCreate: '1', from: 'dashboard' } })
      break
    case 'exportReport':
      void exportReport()
      break
    case 'systemSettings':
      router.push('/admin')
      break
  }
}

const handleStatClick = (stat: DashboardStat) => {
  switch (stat.type) {
    case 'customers':
      router.push('/customers')
      break
    case 'products':
      router.push('/products')
      break
    case 'inventory':
      router.push('/inventory')
      break
  }
}

const exportReport = async () => {
  try {
    const response = await customerApi.exportCustomers({}) as unknown as Blob
    const blob = response instanceof Blob ? response : new Blob([response], { type: 'text/csv;charset=utf-8' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `客户报表_${dayjs().format('YYYYMMDD')}.csv`
    link.click()
    window.URL.revokeObjectURL(url)
  } catch {
    message.error('导出失败')
  }
}

const emptyCustomerPage: PageResult<Customer> = {
  content: [],
  totalElements: 0,
  totalPages: 0,
  size: 0,
  number: 0,
  first: true,
  last: true,
  empty: true,
}

const loadTrendData = async () => {
  try {
    loading.value.trend = true
    const size = resolveTrendRequestSize(chartPeriod.value)

    const [customersRes, giftLogsRes] = await Promise.all([
      customerApi.getCustomers({ page: 0, size, sort: 'registeredAt', direction: 'desc' }) as unknown as Promise<PageResult<Customer>>,
      giftLogApi.loadGiftLogs({ page: 0, size }) as unknown as Promise<PageResult<GiftLogDTO>>,
    ])

    trendCustomers.value = customersRes.content || []
    trendGiftLogs.value = giftLogsRes.content || []
  } catch {
    trendCustomers.value = []
    trendGiftLogs.value = []
    message.warning('趋势图数据暂未加载，已保留其他看板数据')
  } finally {
    loading.value.trend = false
  }
}

const loadDashboardOverview = async () => {
  try {
    loading.value.stats = true
    loading.value.customers = showCustomerSection.value
    loading.value.inventory = true

    const customersRequest = showCustomerSection.value
      ? customerApi.getCustomers({ page: 0, size: 5, sort: 'registeredAt', direction: 'desc' }) as unknown as Promise<PageResult<Customer>>
      : Promise.resolve(emptyCustomerPage)

    const [customerStatRes, stockStatRes, customersRes, lowStockRes] = await Promise.all([
      customerApi.getStatistics() as unknown as Promise<CustomerStatistics>,
      productApi.getStockStatistics() as unknown as Promise<ProductStockStatistics>,
      customersRequest,
      productApi.getLowStockProducts() as unknown as Promise<Product[]>,
    ])

    customerStats.value = {
      totalCustomers: customerStatRes.totalCustomers || 0,
      normalCustomers: customerStatRes.normalCustomers || 0,
      disabledCustomers: customerStatRes.disabledCustomers || 0,
      giftLevelDistribution: customerStatRes.giftLevelDistribution || {},
    }
    stockStats.value = {
      totalProducts: stockStatRes.totalProducts || 0,
      activeProducts: stockStatRes.activeProducts || 0,
      lowStockProducts: stockStatRes.lowStockProducts || 0,
      outOfStockProducts: stockStatRes.outOfStockProducts || 0,
      totalStockQuantity: stockStatRes.totalStockQuantity || 0,
      totalStockValue: Number(stockStatRes.totalStockValue || 0),
    }
    recentCustomers.value = customersRes.content || []
    lowStockProducts.value = (lowStockRes || []).slice(0, 5)
  } catch {
    message.error('加载数据失败')
  } finally {
    loading.value.stats = false
    loading.value.customers = false
    loading.value.inventory = false
  }
}

const loadDashboardData = async () => {
  await Promise.all([loadDashboardOverview(), loadTrendData()])
}

let refreshInterval: ReturnType<typeof setInterval> | undefined

onMounted(() => {
  void loadDashboardData()
  refreshInterval = setInterval(() => {
    void loadDashboardData()
  }, 5 * 60 * 1000)
})

watch(chartPeriod, () => {
  void loadTrendData()
})

onUnmounted(() => {
  if (refreshInterval) {
    clearInterval(refreshInterval)
  }
})
</script>

<style scoped>
.dashboard-container {
  padding: 20px;
  background: #f5f7fa;
  min-height: 100vh;
}

.welcome-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
}

.welcome-card :deep(.ant-card-body) {
  padding: 16px;
}

.user-area {
  display: flex;
  align-items: center;
  gap: 12px;
  justify-content: flex-end;
  margin-bottom: 12px;
}

.user-meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
  text-align: right;
}

.account-trigger {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.stat-card {
  border-radius: 8px;
  transition: all 0.3s;
  border: none;
  box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.03);
  position: relative;
  padding-right: 88px;
}

.stat-card-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 52px;
}

.stat-copy {
  min-width: 0;
}

.stat-primary-line {
  display: flex;
  align-items: baseline;
  justify-content: flex-start;
  gap: 10px;
  flex-wrap: wrap;
}

.stat-title {
  min-width: 64px;
  font-size: 13px;
  color: #9ca3af;
  white-space: nowrap;
}

.stat-value-inline {
  font-size: 30px;
  line-height: 1;
  font-weight: 700;
  color: #111827;
}

.stat-secondary-line {
  margin-top: 14px;
}

.row-card {
  margin-bottom: 16px;
}

.dashboard-equal-row {
  align-items: stretch;
}

.dashboard-equal-col {
  display: flex;
}

.dashboard-equal-card {
  width: 100%;
  height: 100%;
}

.dashboard-equal-card :deep(.ant-card-body) {
  display: flex;
  flex: 1;
  flex-direction: column;
  height: 100%;
}

.dashboard-equal-card :deep(.ant-list) {
  flex: 1;
}

.quick-actions-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
}

.quick-action-grid-item {
  min-width: 0;
}

.quick-action-card {
  border-radius: 14px;
  min-height: 96px;
  height: 100%;
  border: 1px solid #e8eef7;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.03);
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.quick-action-card :deep(.ant-card-body) {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 16px 18px;
}

.quick-action-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.07);
  border-color: #d6e4ff;
}

.quick-action-content {
  display: grid;
  grid-template-columns: 36px minmax(0, 1fr);
  column-gap: 28px;
  align-items: center;
  width: 100%;
  height: 100%;
}

.quick-action-copy {
  min-width: 0;
  width: max-content;
  max-width: 100%;
  justify-self: center;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 4px;
}

.action-icon {
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  border-radius: 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
}

.action-icon.primary {
  background: #eff6ff;
  color: #2563eb;
}

.action-icon.success {
  background: #f0fdf4;
  color: #16a34a;
}

.action-icon.warning {
  background: #fff7ed;
  color: #ea580c;
}

.action-icon.danger {
  background: #fef2f2;
  color: #dc2626;
}

.action-icon.info {
  background: #ecfeff;
  color: #0891b2;
}

.action-icon.default {
  background: #f8fafc;
  color: #4b5563;
}

.quick-action-title {
  font-size: 15px;
  font-weight: 600;
  line-height: 1.3;
  color: #111827;
}

.quick-action-description {
  font-size: 12px;
  line-height: 1.5;
  color: #6b7280;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px 0 rgba(0, 0, 0, 0.1);
}

.stat-card.customers {
  border-left: 4px solid #1890ff;
}

.stat-card.products {
  border-left: 4px solid #52c41a;
}

.stat-card.inventory {
  border-left: 4px solid #faad14;
}

.stat-card.clickable {
  cursor: pointer;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  position: absolute;
  right: 20px;
  top: 50%;
  transform: translateY(-50%);
}

.stat-icon.customers {
  background: #e6f7ff;
  color: #1890ff;
}

.stat-icon.products {
  background: #f6ffed;
  color: #52c41a;
}

.stat-icon.inventory {
  background: #fff7e6;
  color: #faad14;
}

@media (max-width: 768px) {
  .user-area {
    justify-content: space-between;
  }

  .stat-card {
    padding-right: 20px;
  }

  .stat-icon {
    position: static;
    transform: none;
    margin-top: 12px;
  }
}
</style>
