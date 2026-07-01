<template>
  <div class="admin-page">
    <div class="page-header">
      <div>
        <p class="page-subtitle">聚合系统概览、模块入口与运行提醒，便于演示和日常巡检。</p>
      </div>
      <a-space wrap>
        <a-button @click="router.push('/users')">用户管理</a-button>
        <a-button @click="router.push('/roles')">角色管理</a-button>
        <a-button @click="goDataDicts">配置管理</a-button>
      </a-space>
    </div>

    <a-alert
      v-if="overviewNotice"
      class="page-alert"
      type="warning"
      show-icon
      :message="overviewNotice"
    />

    <a-row :gutter="[16, 16]" class="summary-row">
      <a-col v-for="card in summaryCards" :key="card.key" :xs="24" :sm="12" :xl="6">
        <a-card :loading="loading" class="summary-card">
          <div class="summary-card-label">{{ card.label }}</div>
          <div class="summary-card-value">{{ card.value }}</div>
          <div class="summary-card-helper">{{ card.helper }}</div>
        </a-card>
      </a-col>
    </a-row>

    <a-row :gutter="[16, 16]">
      <a-col :xs="24">
        <a-card title="角色与权限说明" class="section-card">
          <div class="role-header">
            <span>当前登录</span>
            <a-tag :color="currentRoleColor">{{ currentRoleText }}</a-tag>
          </div>
          <div class="permission-list">
            <div v-for="item in permissionItems" :key="item.title" class="permission-item">
              <div class="permission-title-row">
                <strong>{{ item.title }}</strong>
                <a-tag :color="item.color">{{ item.roleName }}</a-tag>
              </div>
              <div class="permission-desc">{{ item.description }}</div>
            </div>
          </div>
        </a-card>
      </a-col>

      <a-col :xs="24" :xl="12">
        <a-card title="库存与日志提醒" class="section-card" :loading="loading">
          <template #extra>
            <a-button type="link" @click="router.push('/inventory/logs')">查看库存日志</a-button>
          </template>

          <div class="runtime-list">
            <div class="runtime-item">
              <span class="runtime-label">库存操作总数</span>
              <span>{{ inventoryStats.totalOperations }}</span>
            </div>
            <div class="runtime-item">
              <span class="runtime-label">操作成功率</span>
              <span>{{ formatSuccessRate(inventoryStats.successRate) }}</span>
            </div>
            <div class="runtime-item">
              <span class="runtime-label">低库存商品</span>
              <span>{{ lowStockProducts.length }} 个</span>
            </div>
            <div class="runtime-item">
              <span class="runtime-label">最近刷新</span>
              <span>{{ lastUpdatedText }}</span>
            </div>
          </div>

          <a-divider />

          <a-empty v-if="!lowStockProducts.length" description="当前没有低库存告警" />
          <div v-else class="warning-list">
            <div
              v-for="product in lowStockProducts"
              :key="product.id || product.code"
              class="warning-item"
            >
              <div>
                <div class="warning-title">{{ product.name }}</div>
                <div class="warning-desc">
                  {{ product.code }} · 当前 {{ product.currentStock }} {{ product.unit }} / 安全库存
                  {{ product.safeStock }} {{ product.unit }}
                </div>
              </div>
              <a-button type="link" @click="openInventory(product.id)">处理</a-button>
            </div>
          </div>
        </a-card>
      </a-col>

      <a-col :xs="24" :xl="12">
        <a-card title="配置摘要" class="section-card" :loading="loading">
          <template #extra>
            <a-button type="link" @click="goDataDicts">进入配置管理</a-button>
          </template>

          <div class="runtime-list config-runtime-list">
            <div class="runtime-item">
              <span class="runtime-label">配置项总数</span>
              <span>{{ configTotal }}</span>
            </div>
            <div class="runtime-item">
              <span class="runtime-label">当前用户</span>
              <span>{{ authStore.userName || '管理员' }}</span>
            </div>
            <div class="runtime-item">
              <span class="runtime-label">当前时间</span>
              <span>{{ currentTimeText }}</span>
            </div>
            <div class="runtime-item">
              <span class="runtime-label">可演示模块</span>
              <span>{{ shortcuts.length }} 个</span>
            </div>
          </div>

          <a-divider />

          <a-empty v-if="!recentConfigs.length" description="暂无配置数据" />
          <div v-else class="config-list">
            <div v-for="item in recentConfigs" :key="item.id" class="config-item">
              <div>
                <div class="config-title">{{ item.paramName || item.paramCode }}</div>
                <div class="config-desc">
                  {{ item.groupName || item.groupCode }} · {{ item.paramValue || '--' }}
                </div>
              </div>
              <a-tag
                :color="
                  String(item.status) === 'ACTIVE' || String(item.status) === 'DICT_STATUS_ACTIVE'
                    ? 'green'
                    : 'default'
                "
              >
                {{
                  String(item.status) === 'ACTIVE' || String(item.status) === 'DICT_STATUS_ACTIVE'
                    ? '生效中'
                    : '已停用'
                }}
              </a-tag>
            </div>
          </div>
        </a-card>
      </a-col>

      <a-col :xs="24" :xl="12">
        <a-card title="操作日志预览" class="section-card" :loading="loading">
          <div class="runtime-list">
            <div class="runtime-item">
              <span class="runtime-label">最近日志数</span>
              <span>{{ operationLogSummary.total }}</span>
            </div>
            <div class="runtime-item">
              <span class="runtime-label">成功 / 失败</span>
              <span
                >{{ operationLogSummary.successCount }} /
                {{ operationLogSummary.failureCount }}</span
              >
            </div>
            <div class="runtime-item">
              <span class="runtime-label">成功率</span>
              <span>{{ formatSuccessRate(operationLogSummary.successRate) }}</span>
            </div>
            <div class="runtime-item">
              <span class="runtime-label">平均耗时</span>
              <span>{{ formatExecutionTime(operationLogSummary.avgExecutionTime) }}</span>
            </div>
          </div>

          <a-divider />

          <a-empty v-if="!recentOperationLogs.length" description="暂无操作日志数据" />
          <div v-else class="log-list">
            <div v-for="item in recentOperationLogs" :key="item.id" class="log-item">
              <div>
                <div class="log-title-row">
                  <span class="warning-title">{{ item.module || '系统模块' }}</span>
                  <a-tag :color="Number(item.status) === 1 ? 'green' : 'red'">
                    {{ Number(item.status) === 1 ? '成功' : '失败' }}
                  </a-tag>
                </div>
                <div class="log-desc">
                  {{ item.operator || '系统' }} · {{ item.description || item.operationType }} ·
                  {{ formatDateTime(item.operationTime) }}
                </div>
              </div>
              <span class="log-meta">{{ formatExecutionTime(item.executionTime) }}</span>
            </div>
          </div>
        </a-card>
      </a-col>

      <a-col :xs="24" :xl="12">
        <a-card title="账号管理预留" class="section-card" :loading="loading">
          <template #extra>
            <a-button type="link" @click="router.push('/account')">进入个人中心</a-button>
          </template>

          <div class="runtime-list">
            <div class="runtime-item">
              <span class="runtime-label">当前账号</span>
              <span>{{ accountUser?.username || authStore.userName || '--' }}</span>
            </div>
            <div class="runtime-item">
              <span class="runtime-label">角色</span>
              <span>{{ currentRoleText }}</span>
            </div>
            <div class="runtime-item">
              <span class="runtime-label">账号状态</span>
              <span>{{ accountStatusText }}</span>
            </div>
            <div class="runtime-item">
              <span class="runtime-label">最近登录</span>
              <span>{{ formatDateTime(accountUser?.lastLoginAt) }}</span>
            </div>
          </div>

          <a-divider />

          <div class="capability-list">
            <div v-for="item in accountCapabilityItems" :key="item.title" class="capability-item">
              <div>
                <div class="warning-title">{{ item.title }}</div>
                <div class="warning-desc">{{ item.description }}</div>
              </div>
              <a-tag :color="item.color">{{ item.status }}</a-tag>
            </div>
          </div>
        </a-card>
      </a-col>

      <a-col :xs="24">
        <a-card title="配置分类统计" class="section-card" :loading="loading">
          <template #extra>
            <a-button type="link" @click="goDataDicts">进入配置管理</a-button>
          </template>

          <a-alert class="section-note" type="info" show-icon :message="configCategoryNotice" />

          <a-empty v-if="!configCategoryStats.length" description="暂无配置分类数据" />
          <div v-else class="category-grid">
            <div v-for="item in configCategoryStats" :key="item.key" class="category-item">
              <div class="category-header">
                <span class="category-title">{{ item.label }}</span>
                <span class="category-count">{{ item.count }}</span>
              </div>
              <div class="category-desc">生效 {{ item.activeCount }} / 总计 {{ item.count }}</div>
              <div class="category-desc">最近更新 {{ formatDateTime(item.latestUpdatedAt) }}</div>
            </div>
          </div>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
  import { computed, onMounted, ref } from 'vue'
  import { useRouter } from 'vue-router'
  import dayjs from 'dayjs'
  import {
    AppstoreOutlined,
    GiftOutlined,
    InboxOutlined,
    ProfileOutlined,
    SettingOutlined,
    TeamOutlined,
  } from '@ant-design/icons-vue'
  import { authApi } from '@/api/auth'
  import { customerApi } from '@/api/customer'
  import { dataDictApi } from '@/api/dataDict'
  import { inventoryLogApi } from '@/api/inventoryLog'
  import { operationLogApi, type OperationLogRecord } from '@/api/operationLog'
  import { productApi } from '@/api/product'
  import { useAuthStore } from '@/stores/auth'
  import type { DataDict, InventoryLogStats, PageResult, Product } from '@/types'
  import type { User } from '@/types/auth'
  import {
    buildConfigCategoryStats,
    buildOperationLogSummary,
    type CustomerStatistics,
    type ProductStockStatistics,
  } from '@/utils/featureEnhancements'

  const router = useRouter()
  const authStore = useAuthStore()

  const loading = ref(false)
  const overviewNotice = ref('')
  const lastUpdatedAt = ref<string>('')
  const customerStats = ref<CustomerStatistics>({
    totalCustomers: 0,
    normalCustomers: 0,
    disabledCustomers: 0,
    giftLevelDistribution: {},
  })
  const productStats = ref<ProductStockStatistics>({
    totalProducts: 0,
    activeProducts: 0,
    lowStockProducts: 0,
    outOfStockProducts: 0,
    totalStockQuantity: 0,
    totalStockValue: 0,
  })
  const inventoryStats = ref<InventoryLogStats>({
    inCount: 0,
    outCount: 0,
    inQuantity: 0,
    outQuantity: 0,
    totalOperations: 0,
    successCount: 0,
    failureCount: 0,
    successRate: 0,
    avgCostTime: 0,
  })
  const lowStockProducts = ref<Product[]>([])
  const configEntries = ref<DataDict[]>([])
  const recentConfigs = ref<DataDict[]>([])
  const configTotal = ref(0)
  const recentOperationLogs = ref<OperationLogRecord[]>([])
  const currentAccount = ref<User | null>(null)

  const accountUser = computed<User | null>(() => {
    if (currentAccount.value) {
      return currentAccount.value
    }

    if (!authStore.userName) {
      return null
    }

    return {
      username: authStore.userName,
      realName: authStore.userName,
      email: '',
      role: (authStore.userRole || 'ADMIN') as User['role'],
      status: 1,
    }
  })
  const currentRoleText = computed(
    () =>
      ({
        ADMIN: '管理员',
        MANAGER: '经理',
        USER: '普通用户',
      })[accountUser.value?.role || authStore.userRole || 'ADMIN'] || '管理员',
  )

  const currentRoleColor = computed(
    () =>
      ({
        ADMIN: 'red',
        MANAGER: 'blue',
        USER: 'green',
      })[accountUser.value?.role || authStore.userRole || 'ADMIN'] || 'blue',
  )
  const currentTimeText = computed(() => dayjs().format('YYYY-MM-DD HH:mm'))
  const lastUpdatedText = computed(() =>
    lastUpdatedAt.value ? dayjs(lastUpdatedAt.value).format('YYYY-MM-DD HH:mm:ss') : '--',
  )
  const operationLogSummary = computed(() => buildOperationLogSummary(recentOperationLogs.value))
  const configCategoryStats = computed(() => buildConfigCategoryStats(configEntries.value))
  const accountStatusText = computed(() => {
    if (!accountUser.value) {
      return '未获取'
    }

    return accountUser.value.status === 0 ? '已停用' : '正常'
  })
  const configCategoryNotice = computed(() => {
    const loadedCount = configEntries.value.length
    if (!loadedCount) {
      return '当前暂无可聚合的配置记录，后续可直接切换到系统配置接口。'
    }

    if (configTotal.value > loadedCount) {
      return `当前基于最近 ${loadedCount} 条配置记录聚合分类，后续可直接切换到完整系统配置接口。`
    }

    return '当前基于现有配置管理数据聚合分类，后续可直接切换到系统配置接口。'
  })
  const accountCapabilityItems = computed(() => [
    {
      title: '个人资料与密码维护',
      description: '复用个人中心，已支持查看资料、修改资料与密码。',
      status: '已接通',
      color: 'green',
    },
    {
      title: '当前账号画像',
      description: accountUser.value
        ? `已同步 ${accountUser.value.username} 的角色、状态与最近登录信息。`
        : '待登录后同步当前账号信息。',
      status: accountUser.value ? '已接通' : '待同步',
      color: accountUser.value ? 'blue' : 'default',
    },
    {
      title: '用户目录与批量管理',
      description: '后端暂未提供分页用户目录接口，页面结构已预留，后续可直接扩展列表与筛选。',
      status: '待接入',
      color: 'gold',
    },
    {
      title: '角色授权与重置密码',
      description: '待账号列表与管理员操作接口接入后，可在当前模块继续扩展授权与安全操作。',
      status: '待接入',
      color: 'default',
    },
  ])
  const summaryCards = computed(() => [
    {
      key: 'customers',
      label: '客户总量',
      value: customerStats.value.totalCustomers,
      helper: `正常 ${customerStats.value.normalCustomers} / 停用 ${customerStats.value.disabledCustomers}`,
    },
    {
      key: 'products',
      label: '商品概况',
      value: productStats.value.totalProducts,
      helper: `在售 ${productStats.value.activeProducts} / 低库存 ${productStats.value.lowStockProducts}`,
    },
    {
      key: 'inventory',
      label: '库存操作',
      value: inventoryStats.value.totalOperations,
      helper: `成功 ${inventoryStats.value.successCount} / 失败 ${inventoryStats.value.failureCount}`,
    },
    {
      key: 'configs',
      label: '系统配置',
      value: configTotal.value,
      helper: `${lowStockProducts.value.length} 个库存提醒待关注`,
    },
  ])
  const shortcuts = [
    {
      title: '客户管理',
      description: '查看客户列表、详情与推荐关系',
      path: '/customers',
      icon: TeamOutlined,
      type: 'blue',
    },
    {
      title: '库存总览',
      description: '处理库存预警、入库与调整',
      path: '/inventory',
      icon: InboxOutlined,
      type: 'orange',
    },
    {
      title: '库存日志',
      description: '查看库存变更记录与操作明细',
      path: '/inventory/logs',
      icon: ProfileOutlined,
      type: 'purple',
    },
    {
      title: '礼品发放',
      description: '查看礼品记录并继续发放流程',
      path: '/gift-logs',
      icon: GiftOutlined,
      type: 'green',
    },
    {
      title: '配置管理',
      description: '维护字典项与系统参数',
      path: '/data-dicts',
      icon: SettingOutlined,
      type: 'cyan',
    },
    {
      title: '商品管理',
      description: '查看商品、库存与低库存状态',
      path: '/products',
      icon: AppstoreOutlined,
      type: 'gold',
    },
  ]
  const permissionItems = [
    {
      title: '系统管理员',
      role: 'ADMIN',
      roleName: '管理员',
      color: 'red',
      description: '可访问系统概览、配置管理、库存与业务模块，适合演示全局管理能力。',
    },
    {
      title: '业务经理',
      role: 'MANAGER',
      roleName: '经理',
      color: 'blue',
      description: '聚焦客户、商品、礼品与库存流程，不暴露系统级配置入口。',
    },
    {
      title: '普通用户',
      role: 'USER',
      roleName: '普通用户',
      color: 'green',
      description: '可查看商品和礼品相关页面，避免进入无权限的系统管理能力。',
    },
  ]

  const formatSuccessRate = (value: number) => `${Number(value || 0).toFixed(1)}%`

  const formatExecutionTime = (value?: number) => {
    if (!value) {
      return '--'
    }

    return `${value} ms`
  }

  const formatDateTime = (value?: string) => {
    if (!value) {
      return '--'
    }

    const dateValue = dayjs(value)
    return dateValue.isValid() ? dateValue.format('YYYY-MM-DD HH:mm:ss') : '--'
  }

  const goDashboard = () => {
    router.push('/dashboard')
  }

  const goDataDicts = () => {
    router.push('/data-dicts')
  }

  const openInventory = (id?: number) => {
    if (!id) {
      router.push('/inventory')
      return
    }

    router.push(`/inventory/${id}`)
  }

  const loadOverview = async () => {
    loading.value = true
    overviewNotice.value = ''

    const results = await Promise.allSettled([
      customerApi.getStatistics() as Promise<CustomerStatistics>,
      productApi.getStockStatistics() as Promise<ProductStockStatistics>,
      inventoryLogApi.getStats() as Promise<InventoryLogStats>,
      productApi.getLowStockProducts() as Promise<Product[]>,
      dataDictApi.loadDataDicts({ page: 0, size: 200 }) as Promise<PageResult<DataDict>>,
      operationLogApi.searchLogs({ page: 0, size: 6 }) as Promise<{
        content: OperationLogRecord[]
      }>,
      authApi.getCurrentUser() as Promise<User>,
    ])

    const failedModules: string[] = []

    if (results[0].status === 'fulfilled') {
      customerStats.value = {
        totalCustomers: results[0].value.totalCustomers || 0,
        normalCustomers: results[0].value.normalCustomers || 0,
        disabledCustomers: results[0].value.disabledCustomers || 0,
        giftLevelDistribution: results[0].value.giftLevelDistribution || {},
      }
    } else {
      failedModules.push('客户统计')
    }

    if (results[1].status === 'fulfilled') {
      productStats.value = {
        totalProducts: results[1].value.totalProducts || 0,
        activeProducts: results[1].value.activeProducts || 0,
        lowStockProducts: results[1].value.lowStockProducts || 0,
        outOfStockProducts: results[1].value.outOfStockProducts || 0,
        totalStockQuantity: results[1].value.totalStockQuantity || 0,
        totalStockValue: Number(results[1].value.totalStockValue || 0),
      }
    } else {
      failedModules.push('商品统计')
    }

    if (results[2].status === 'fulfilled') {
      const raw = results[2].value
      inventoryStats.value = {
        ...inventoryStats.value,
        ...raw,
        totalOperations: (raw.inCount || 0) + (raw.outCount || 0),
      }
    } else {
      failedModules.push('库存日志统计')
    }

    if (results[3].status === 'fulfilled') {
      lowStockProducts.value = results[3].value.slice(0, 5)
    } else {
      lowStockProducts.value = []
      failedModules.push('低库存提醒')
    }

    if (results[4].status === 'fulfilled') {
      configEntries.value = results[4].value.content || []
      recentConfigs.value = configEntries.value.slice(0, 5)
      configTotal.value = results[4].value.totalElements || configEntries.value.length
    } else {
      configEntries.value = []
      recentConfigs.value = []
      configTotal.value = 0
      failedModules.push('配置摘要')
    }

    if (results[5].status === 'fulfilled') {
      recentOperationLogs.value = results[5].value.content || []
    } else {
      recentOperationLogs.value = []
      failedModules.push('操作日志预览')
    }

    if (results[6].status === 'fulfilled') {
      currentAccount.value = results[6].value
    } else {
      currentAccount.value = null
      failedModules.push('当前账号')
    }

    if (failedModules.length) {
      overviewNotice.value = `部分概览数据暂未加载完成：${failedModules.join('、')}。页面仍保留可用入口用于演示。`
    }

    lastUpdatedAt.value = new Date().toISOString()
    loading.value = false
  }

  onMounted(() => {
    void loadOverview()
  })
</script>

<style scoped>
  .admin-page {
    min-height: 100vh;
    padding: 20px;
    background: var(--bg-page);
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

  .page-alert,
  .summary-row {
    margin-bottom: 16px;
  }

  .summary-card {
    min-height: 140px;
  }

  .summary-card-label {
    color: #8c8c8c;
    font-size: 13px;
  }

  .summary-card-value {
    margin-top: 8px;
    font-size: 32px;
    font-weight: 700;
    color: #262626;
  }

  .summary-card-helper {
    margin-top: 8px;
    color: #595959;
  }

  .section-card {
    height: 100%;
  }

  .shortcut-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 16px;
  }

  .shortcut-item {
    display: flex;
    gap: 16px;
    padding: 16px;
    border-radius: 12px;
    background: #fafafa;
    cursor: pointer;
    transition:
      transform 0.2s ease,
      box-shadow 0.2s ease;
  }

  .shortcut-item:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 20px rgba(15, 23, 42, 0.08);
  }

  .shortcut-icon {
    width: 44px;
    height: 44px;
    border-radius: 12px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    font-size: 20px;
    color: #fff;
  }

  .shortcut-icon.blue {
    background: linear-gradient(135deg, #1677ff, #69b1ff);
  }

  .shortcut-icon.orange {
    background: linear-gradient(135deg, #fa8c16, #ffc069);
  }

  .shortcut-icon.purple {
    background: linear-gradient(135deg, #722ed1, #b37feb);
  }

  .shortcut-icon.green {
    background: linear-gradient(135deg, #389e0d, #95de64);
  }

  .shortcut-icon.cyan {
    background: linear-gradient(135deg, #08979c, #5cdbd3);
  }

  .shortcut-icon.gold {
    background: linear-gradient(135deg, #d48806, #ffd666);
  }

  .shortcut-title {
    font-weight: 600;
    color: #262626;
  }

  .shortcut-description {
    margin-top: 4px;
    color: #8c8c8c;
  }

  .role-header {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 16px;
  }

  .permission-list,
  .runtime-list,
  .warning-list,
  .config-list,
  .log-list,
  .capability-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .permission-item,
  .warning-item,
  .config-item,
  .log-item,
  .capability-item,
  .category-item {
    padding: 14px 16px;
    border: 1px solid #f0f0f0;
    border-radius: 12px;
  }

  .permission-title-row,
  .log-title-row,
  .category-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
  }

  .permission-desc,
  .warning-desc,
  .config-desc,
  .log-desc,
  .category-desc {
    margin-top: 8px;
    color: #8c8c8c;
  }

  .runtime-item,
  .warning-item,
  .config-item,
  .log-item,
  .capability-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
  }

  .runtime-label {
    color: #8c8c8c;
  }

  .warning-title,
  .config-title,
  .category-title {
    font-weight: 600;
    color: #262626;
  }

  .log-meta {
    color: #8c8c8c;
    white-space: nowrap;
  }

  .category-grid {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 16px;
  }

  .category-count {
    font-size: 28px;
    font-weight: 700;
    color: #1677ff;
  }

  .section-note {
    margin-bottom: 16px;
  }

  .config-runtime-list {
    margin-bottom: 0;
  }

  @media (max-width: 1200px) {
    .category-grid {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }
  }

  @media (max-width: 992px) {
    .shortcut-grid,
    .category-grid {
      grid-template-columns: 1fr;
    }
  }

  @media (max-width: 768px) {
    .page-header,
    .runtime-item,
    .warning-item,
    .config-item,
    .permission-title-row,
    .log-item,
    .capability-item,
    .log-title-row,
    .category-header {
      flex-direction: column;
      align-items: flex-start;
    }
  }
</style>
