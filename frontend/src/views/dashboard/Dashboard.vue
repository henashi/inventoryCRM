<!-- frontend/src/views/dashboard/Dashboard.vue -->
<template>
  <div class="dashboard-container">
    <!-- 顶部欢迎区域 -->
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
        <a-tooltip title="退出登录">
          <a-button class="logout-icon" type="default" shape="circle" size="small" @click="handleLogout">
            <logout-outlined />
          </a-button>
        </a-tooltip>
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

    <!-- 卡片区域（带灰色背景） -->
    <div class="cards-panel">
      <!-- 统计卡片 -->
      <a-row :gutter="[16, 16]" class="row-card mb-6">
        <a-col :xs="24" :sm="12" :md="6" v-for="stat in stats" :key="stat.title">
          <a-card class="stat-card" :class="[stat.type, 'clickable']" hoverable @click="() => handleStatClick(stat)">
            <div class="flex items-center justify-between">
              <div>
                <div class="text-sm text-gray-500">{{ stat.title }}</div>
                <div class="text-2xl font-bold mt-2">{{ stat.value }}</div>
                <div class="text-xs mt-1" :class="stat.trendClass">
                  <caret-up-outlined v-if="stat.trend === 'up'" />
                  <caret-down-outlined v-else />
                  {{ stat.change }} {{ stat.unit }}
                </div>
              </div>
              <div class="stat-icon" :class="stat.type">
                <component :is="stat.icon" />
              </div>
            </div>
          </a-card>
        </a-col>
      </a-row>

      <!-- 图表和表格区域 -->
      <a-row :gutter="[16, 16]" class="row-card mb-6">
        <!-- 客户增长趋势 -->
        <a-col :xs="24" :lg="16">
          <a-card title="客户增长趋势" class="h-full">
            <template #extra>
              <a-select v-model:value="chartPeriod" size="small" style="width: 100px">
                <a-select-option value="week">本周</a-select-option>
                <a-select-option value="month">本月</a-select-option>
                <a-select-option value="quarter">本季</a-select-option>
              </a-select>
            </template>
            <div class="h-64">
              <LineChart v-if="chartData" :data="chartData" />
              <div v-else class="flex items-center justify-center h-full">
                <a-spin />
              </div>
            </div>
          </a-card>
        </a-col>

        <!-- 礼品等级分布 -->
        <a-col :xs="24" :lg="8">
          <a-card title="礼品等级分布" class="h-full">
            <div class="h-64">
              <PieChart v-if="giftDistribution" :data="giftDistribution" />
              <div v-else class="flex items-center justify-center h-full">
                <a-spin />
              </div>
            </div>
          </a-card>
        </a-col>
      </a-row>

      <!-- 最近客户和库存预警 -->
      <a-row :gutter="[16, 16]" class="row-card">
        <!-- 最近添加的客户 -->
        <a-col :xs="24" :lg="12">
          <a-card title="最近添加的客户" class="h-full">
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
                        {{ item.phone }} • {{ formatDate(item.createdAt) }}
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

        <!-- 库存预警 -->
        <a-col :xs="24" :lg="12">
          <a-card title="库存预警" class="h-full">
            <template #extra>
              <a-button type="link" @click="router.push('/inventory')">管理库存</a-button>
            </template>
            <a-list
              :data-source="lowStockProducts"
              :loading="loading.inventory"
              item-layout="horizontal"
              size="small"
            >
              <template #renderItem="{ item }">
                <a-list-item class="hover:bg-gray-50 cursor-pointer" @click="viewProduct(item)">
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
                    <a-button
                      type="link"
                      size="small"
                      danger
                      @click.stop="handleReplenish(item)"
                    >
                      补货
                    </a-button>
                  </template>
                </a-list-item>
              </template>
            </a-list>
          </a-card>
        </a-col>
      </a-row>

        <!-- 快捷操作 -->
        <a-card title="快捷操作" class="row-card mt-6">
        <a-row :gutter="[16, 16]">
          <a-col
            v-for="action in quickActions"
            :key="action.title"
            :xs="12"
            :sm="8"
            :md="6"
            :lg="4"
          >
            <a-card
              hoverable
              class="text-center quick-action-card"
              @click="handleQuickAction(action)"
            >
              <div class="action-icon mb-2" :class="action.type">
                <component :is="action.icon" />
              </div>
              <div class="font-medium">{{ action.title }}</div>
              <div class="text-xs text-gray-500 mt-1">{{ action.description }}</div>
            </a-card>
          </a-col>
        </a-row>
      </a-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  UserOutlined,
  TeamOutlined,
  ShoppingOutlined,
  InboxOutlined,
  GiftOutlined,
  RiseOutlined,
  FallOutlined,
  PhoneOutlined,
  UserAddOutlined,
  PlusCircleOutlined,
  FileAddOutlined,
  ShoppingCartOutlined,
  BarChartOutlined,
  SettingOutlined,
  LogoutOutlined
} from '@ant-design/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useCustomerStore } from '@/stores/customer'
import { useProductStore } from '@/stores/product'
import { customerApi } from '@/api/customer'
import { productApi } from '@/api/product'
import { inventoryApi } from '@/api/inventory'
import type { Customer, Product } from '@/types'
import dayjs from 'dayjs'

// 如果需要图表组件
import LineChart from '@/components/charts/LineChart.vue'
import PieChart from '@/components/charts/PieChart.vue'

const router = useRouter()
const authStore = useAuthStore()
const customerStore = useCustomerStore()
const productStore = useProductStore()

// 响应式数据
const currentDate = ref(dayjs().format('YYYY年MM月DD日'))
const chartPeriod = ref<'week' | 'month' | 'quarter'>('month')
const loading = ref({
  customers: false,
  inventory: false,
  stats: false
})

// 模拟数据
const stats = ref([
  {
    title: '总客户数',
    value: '1,248',
    change: '+12',
    unit: '人',
    trend: 'up',
    icon: TeamOutlined,
    type: 'customers'
  },
  {
    title: '活跃商品',
    value: '86',
    change: '+3',
    unit: '个',
    trend: 'up',
    icon: ShoppingOutlined,
    type: 'products'
  },
  {
    title: '库存总量',
    value: '2,450',
    change: '-45',
    unit: '件',
    trend: 'down',
    icon: InboxOutlined,
    type: 'inventory'
  },
  {
    title: '礼品发放',
    value: '156',
    change: '+23',
    unit: '次',
    trend: 'up',
    icon: GiftOutlined,
    type: 'gift-logs'
  },
  {
    title: '礼品配置',
    value: '156',
    change: '+23',
    unit: '次',
    trend: 'up',
    icon: GiftOutlined,
    type: 'gifts'
  },
  {
    title: '配置管理',
    value: '156',
    change: '+23',
    unit: '次',
    trend: 'up',
    icon: GiftOutlined,
    type: 'dataDicts'
  }
])

const recentCustomers = ref<Customer[]>([
  { id: 1, name: '张三', phone: '13800138000', giftLevel: 3, status: 1, createdAt: '2024-01-15' },
  { id: 2, name: '李四', phone: '13900139000', giftLevel: 2, status: 1, createdAt: '2024-01-14' },
  { id: 3, name: '王五', phone: '13700137000', giftLevel: 1, status: 1, createdAt: '2024-01-13' },
  { id: 4, name: '赵六', phone: '13600136000', giftLevel: 0, status: 1, createdAt: '2024-01-12' },
  { id: 5, name: '钱七', phone: '13500135000', giftLevel: 3, status: 1, createdAt: '2024-01-11' }
])

const lowStockProducts = ref<Product[]>([
  { id: 1, name: 'iPhone 15', code: 'P001', currentStock: 5, safeStock: 10, unit: '台' },
  { id: 2, name: '华为 Mate 60', code: 'P002', currentStock: 8, safeStock: 15, unit: '台' },
  { id: 3, name: '小米13', code: 'P003', currentStock: 12, safeStock: 20, unit: '台' },
  { id: 4, name: '三星 Galaxy', code: 'P004', currentStock: 3, safeStock: 8, unit: '台' }
])

const chartData = ref({
  labels: ['1月', '2月', '3月', '4月', '5月', '6月', '7月'],
  datasets: [
    {
      label: '新增客户',
      data: [65, 59, 80, 81, 56, 55, 40],
      borderColor: '#1890ff',
      backgroundColor: 'rgba(24, 144, 255, 0.1)',
      tension: 0.4
    },
    {
      label: '礼品发放',
      data: [28, 48, 40, 19, 86, 27, 90],
      borderColor: '#52c41a',
      backgroundColor: 'rgba(82, 196, 26, 0.1)',
      tension: 0.4
    }
  ]
})

const giftDistribution = ref({
  labels: ['等级1', '等级2', '等级3', '未领取'],
  datasets: [
    {
      data: [300, 150, 50, 100],
      backgroundColor: ['#ff7875', '#ffc069', '#95de64', '#d9d9d9']
    }
  ]
})

const giftLevelColors: Record<number, string> = {
  1: 'blue',
  2: 'green',
  3: 'orange'
}

const quickActions = ref([
  {
    title: '新增客户',
    description: '快速添加新客户',
    icon: UserAddOutlined,
    type: 'primary',
    action: 'addCustomer'
  },
  {
    title: '商品入库',
    description: '商品进货登记',
    icon: PlusCircleOutlined,
    type: 'success',
    action: 'addInventory'
  },
  {
    title: '创建订单',
    description: '新建销售订单',
    icon: FileAddOutlined,
    type: 'warning',
    action: 'createOrder'
  },
  {
    title: '礼品发放',
    description: '发放客户礼品',
    icon: GiftOutlined,
    type: 'danger',
    action: 'distributeGift'
  },
  {
    title: '报表导出',
    description: '导出统计报表',
    icon: BarChartOutlined,
    type: 'info',
    action: 'exportReport'
  },
  {
    title: '系统设置',
    description: '系统参数配置',
    icon: SettingOutlined,
    type: 'default',
    action: 'systemSettings'
  }
])

// 计算属性
const userName = computed(() => authStore.userName || '管理员')
const userRole = computed(() => authStore.userRole || 'ADMIN')
const greetingMessage = computed(() => {
  const hour = new Date().getHours()
  if (hour < 12) return '早上好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const userRoleText = computed(() => {
  const roles: Record<string, string> = {
    'ADMIN': '系统管理员',
    'MANAGER': '经理',
    'USER': '普通用户'
  }
  return roles[userRole.value] || '用户'
})

const lastLogin = computed(() => {
  const lastLoginTime = authStore.user?.lastLoginAt
  return lastLoginTime ? dayjs(lastLoginTime).format('MM-DD HH:mm') : '--'
})

const handleLogout = () => {
  authStore.logout()
  router.replace('/login')
}

// 方法
const formatDate = (dateStr?: string) => {
  if (!dateStr) return '--'
  return dayjs(dateStr).format('MM-DD')
}

const getStockColor = (product: Product) => {
  if (product.currentStock < product.safeStock) return 'red'
  if (product.currentStock < product.safeStock * 1.5) return 'orange'
  return 'green'
}

const viewCustomer = (customer: Customer) => {
  router.push(`/customers/${customer.id}`)
}

const viewProduct = (product: Product) => {
  router.push(`/products/${product.id}`)
}

const callCustomer = (phone: string) => {
  Modal.confirm({
    title: '拨打电话',
    content: `确定要拨打 ${phone} 吗？`,
    onOk: () => {
      window.location.href = `tel:${phone}`
    }
  })
}

const handleReplenish = (product: Product) => {
  router.push({
    path: '/inventory/in',
    query: { productId: product.id }
  })
}

const handleQuickAction = (action: any) => {
  switch (action.action) {
    case 'addCustomer':
      router.push('/customers/new')
      break
    case 'addInventory':
      router.push('/inventory/in')
      break
    case 'createOrder':
      router.push('/orders/new')
      break
    case 'distributeGift':
      router.push('/gifts/distribute')
      break
    case 'exportReport':
      exportReport()
      break
    case 'systemSettings':
      router.push('/settings')
      break
  }
}

const handleStatClick = (stat: any) => {
  if (!stat || !stat.type) return
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
    case 'gifts':
      router.push('/gifts')
      break
    case 'gift-logs':
      router.push('/gift-logs')
      break
    case 'dataDicts':
      router.push('/data-dicts')
      break
    default:
      // fallback: 跳转到客户列表作为默认行为
      router.push('/customers')
  }
}

const exportReport = async () => {
  try {
    const response = await customerApi.exportCustomers({})
    const blob = new Blob([response], { type: 'application/vnd.ms-excel' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `客户报表_${dayjs().format('YYYYMMDD')}.xlsx`
    link.click()
    window.URL.revokeObjectURL(url)
  } catch (error) {
    message.error('导出失败')
  }
}

// 加载数据
const loadDashboardData = async () => {
  try {
    loading.value.stats = true
    loading.value.customers = true
    loading.value.inventory = true

    // 并行加载数据
    const [customersRes, productsRes] = await Promise.all([
      customerApi.getCustomers({ page: 0, size: 5 }),
      productApi.getProducts({ page: 0, size: 10 })
    ])

    recentCustomers.value = customersRes.content
    // 筛选低库存商品
    lowStockProducts.value = productsRes.content
      .filter(p => p.currentStock < p.safeStock)
      .slice(0, 5)
  } catch (error) {
    message.error('加载数据失败')
  } finally {
    loading.value.stats = false
    loading.value.customers = false
    loading.value.inventory = false
  }
}

// 定时刷新
let refreshInterval: NodeJS.Timeout

// 生命周期
onMounted(() => {
  loadDashboardData()

  // 每5分钟自动刷新一次数据
  refreshInterval = setInterval(() => {
    loadDashboardData()
  }, 5 * 60 * 1000)
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
  background: #f5f7fa; /* 将整个仪表盘容器背景改为浅灰，与 .cards-panel 保持一致 */
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

.stat-card {
  border-radius: 8px;
  transition: all 0.3s;
  border: none;
  box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.03);
}

/* 给底部的各类卡片增加统一的下间距，参考 welcome-card 的视觉节奏 */
.row-card {
  margin-bottom: 16px;
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

.stat-card.gifts {
  border-left: 4px solid #f5222d;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

/* 确保统计卡的图标位于右侧并在垂直方向居中 */
.stat-card {
  position: relative;
  padding-right: 88px; /* 给右侧图标留白 */
}

/* 可点击卡片样式 */
.stat-card.clickable {
  cursor: pointer;
}
.stat-card.clickable:hover {
  transform: translateY(-4px);
}
.stat-card > .ant-card-body > .flex {
  align-items: center;
}
.stat-card .stat-icon {
  position: absolute;
  right: 20px;
  top: 50%;
  transform: translateY(-50%);
  margin-left: 0;
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

.stat-icon.gifts {
  background: #fff1f0;
  color: #f5222d;
}

.quick-action-card {
  border: 1px solid #f0f0f0;
  transition: all 0.3s;
}

.quick-action-card:hover {
  border-color: #1890ff;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.2);
}

.action-icon {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  margin: 0 auto;
}

.action-icon.primary {
  background: #e6f7ff;
  color: #1890ff;
}

.action-icon.success {
  background: #f6ffed;
  color: #52c41a;
}

.action-icon.warning {
  background: #fff7e6;
  color: #faad14;
}

.action-icon.danger {
  background: #fff1f0;
  color: #f5222d;
}

.action-icon.info {
  background: #f0f5ff;
  color: #2f54eb;
}

.action-icon.default {
  background: #fafafa;
  color: #8c8c8c;
}

/* 欢迎卡片的登出按钮样式 */
.welcome-card {
  position: relative;
}
.user-area {
  position: absolute;
  top: 28px; /* 与卡片内边距保持一致 */
  right: 28px; /* 与卡片内边距保持一致 */
  display: flex;
  align-items: center;
  gap: 10px;
}
.avatar-mini {
  background: rgba(255,255,255,0.12);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
}
.user-meta { text-align: left; color: #fff }
.user-name { font-weight: 600; color: #fff }
.user-role { font-size: 12px; color: rgba(255,255,255,0.85) }
.logout-icon {
  background: transparent;
  border: none;
  color: #ffffff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 8px;
  width: 40px;
  height: 40px;
  font-size: 22px;
  border-radius: 8px;
}
.logout-icon {
  border: 1px solid rgba(255,255,255,0.14);
}
.logout-icon:hover {
  background: rgba(255,255,255,0.12);
  color: #ffffff;
}
.logout-icon .anticon,
.logout-icon svg {
  color: #ffffff !important;
  fill: #ffffff !important;
  stroke: #ffffff !important;
}
/* 小屏幕微调 */
@media (max-width: 768px) {
  .logout-btn {
    top: 8px;
    right: 8px;
    padding: 4px 8px;
  }
}

/* 新的欢迎卡布局 */
.welcome-inner {
  display: flex;
  align-items: flex-start;
  padding: 8px 4px 0 0;
}
.welcome-left {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-width: 720px;
}
.welcome-title {
  font-size: 28px;
  font-weight: 700;
  margin: 0;
  color: #ffffff;
}
.wave { font-size: 22px; margin-left:6px }
.welcome-sub { color: rgba(255,255,255,0.9); margin:0 }
.user-block { display:flex; align-items:center; gap:12px; margin-top:6px }
.avatar-blue { background: rgba(255,255,255,0.1); color: #fff }
.user-details .role { color: rgba(255,255,255,0.95); font-weight:600 }
.user-details .last-login { color: rgba(255,255,255,0.85); font-size: 13px }

/* 调整卡片内边距，使视觉更紧凑 */
.welcome-card :deep(.ant-card-body) {
  padding: 28px 28px 20px 28px;
  background: linear-gradient(135deg, #6b82f7 0%, #7a4bb5 100%);
  border-radius: 12px;
}

@media (max-width: 768px) {
  .welcome-card :deep(.ant-card-body) {
    padding: 18px 16px;
  }
  .welcome-title { font-size: 20px }
}

/* 响应式调整 */
@media (max-width: 768px) {
  .dashboard-container {
    padding: 12px;
  }

  .welcome-card :deep(.ant-card-body) {
    padding: 16px;
  }

  .stat-card {
    margin-bottom: 12px;
  }
  .stat-card {
    padding-right: 24px; /* 小屏幕减少右侧留白，避免溢出 */
  }
  .stat-card .stat-icon {
    position: static;
    transform: none;
    margin-left: 8px;
  }
}
</style>
