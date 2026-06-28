<template>
  <div class="page-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1 class="page-title">系统日志</h1>
      <div class="page-header-actions"></div>
    </div>

    <!-- 统计卡片 -->
    <a-row :gutter="[16, 16]" class="stats-row">
      <a-col :xs="12" :sm="6">
        <a-card :bordered="false" class="stat-card stat-card-total">
          <div class="stat-card-inner">
            <div class="stat-icon-wrap" style="background: #e6f7ff">
              <file-text-outlined style="color: #1890ff; font-size: 20px" />
            </div>
            <div class="stat-info">
              <div class="stat-label">日志总数</div>
              <div class="stat-value">{{ pagination.total }}</div>
            </div>
          </div>
        </a-card>
      </a-col>
      <a-col :xs="12" :sm="6">
        <a-card :bordered="false" class="stat-card stat-card-success">
          <div class="stat-card-inner">
            <div class="stat-icon-wrap" style="background: #f6ffed">
              <check-circle-outlined style="color: #52c41a; font-size: 20px" />
            </div>
            <div class="stat-info">
              <div class="stat-label">成功</div>
              <div class="stat-value" style="color: #52c41a">{{ stats.successCount }}</div>
            </div>
          </div>
        </a-card>
      </a-col>
      <a-col :xs="12" :sm="6">
        <a-card :bordered="false" class="stat-card stat-card-fail">
          <div class="stat-card-inner">
            <div class="stat-icon-wrap" style="background: #fff2f0">
              <close-circle-outlined style="color: #ff4d4f; font-size: 20px" />
            </div>
            <div class="stat-info">
              <div class="stat-label">失败</div>
              <div class="stat-value" style="color: #ff4d4f">{{ stats.failCount }}</div>
            </div>
          </div>
        </a-card>
      </a-col>
      <a-col :xs="12" :sm="6">
        <a-card :bordered="false" class="stat-card stat-card-module">
          <div class="stat-card-inner">
            <div class="stat-icon-wrap" style="background: #fff7e6">
              <appstore-outlined style="color: #fa8c16; font-size: 20px" />
            </div>
            <div class="stat-info">
              <div class="stat-label">涉及模块</div>
              <div class="stat-value" style="color: #fa8c16">{{ stats.moduleCount }}</div>
            </div>
          </div>
        </a-card>
      </a-col>
    </a-row>

    <!-- 搜索栏 -->
    <a-card :bordered="true" class="search-card" size="small">
      <a-form layout="vertical" :model="searchForm" @finish="handleSearch">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="12" :md="8" :lg="6">
            <a-form-item label="模块">
              <a-select v-model:value="searchForm.module" placeholder="全部模块" allow-clear>
                <a-select-option v-for="mod in moduleOptions" :key="mod" :value="mod">
                  {{ mod }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12" :md="8" :lg="6">
            <a-form-item label="操作人">
              <a-input v-model:value="searchForm.operator" placeholder="输入操作人" allow-clear>
                <template #prefix>
                  <user-outlined style="color: rgba(0, 0, 0, 0.25)" />
                </template>
              </a-input>
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12" :md="8" :lg="6">
            <a-form-item label="操作状态">
              <a-select v-model:value="searchForm.status" placeholder="全部状态" allow-clear>
                <a-select-option :value="1">成功</a-select-option>
                <a-select-option :value="0">失败</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12" :md="12" :lg="6">
            <a-form-item label="操作时间">
              <a-range-picker
                v-model:value="searchForm.dateRange"
                :placeholder="['开始日期', '结束日期']"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
              />
            </a-form-item>
          </a-col>
        </a-row>

        <div class="search-actions">
          <a-space :size="12">
            <a-button type="primary" html-type="submit" :loading="isLoading">
              <template #icon><search-outlined /></template>
              搜索
            </a-button>
            <a-button @click="handleReset">
              <template #icon><reload-outlined /></template>
              重置
            </a-button>
          </a-space>
        </div>
      </a-form>
    </a-card>

    <!-- 日志表格 -->
    <a-card :bordered="false" class="table-card">
      <a-table
        :columns="columns"
        :data-source="logs"
        :loading="isLoading"
        :pagination="paginationConfig"
        row-key="id"
        @change="handleTableChange"
        :scroll="{ x: 1000 }"
        size="middle"
      >
        <template #bodyCell="{ column, record }">
          <!-- 操作时间 -->
          <template v-if="column.dataIndex === 'operationTime'">
            <div class="time-cell">
              <div class="time-date">{{ formatDate(record.operationTime, 'MM-DD') }}</div>
              <div class="time-time">{{ formatDate(record.operationTime, 'HH:mm:ss') }}</div>
            </div>
          </template>

          <!-- 模块 -->
          <template v-else-if="column.dataIndex === 'module'">
            <a-tag :color="getModuleColor(record.module)">{{ record.module }}</a-tag>
          </template>

          <!-- 操作类型 -->
          <template v-else-if="column.dataIndex === 'operationType'">
            <span class="op-type">{{ getOperationTypeText(record.operationType) }}</span>
          </template>

          <!-- 描述 -->
          <template v-else-if="column.dataIndex === 'description'">
            <div class="desc-cell" :title="record.description">
              {{ record.description }}
            </div>
          </template>

          <!-- 操作人 -->
          <template v-else-if="column.dataIndex === 'operator'">
            <div class="operator-cell">
              <a-avatar :size="22" style="background-color: #1890ff; font-size: 12px">
                {{ getFirstChar(record.operator) }}
              </a-avatar>
              <span>{{ record.operator || '-' }}</span>
            </div>
          </template>

          <!-- 执行时间 -->
          <template v-else-if="column.dataIndex === 'executionTime'">
            <span v-if="record.executionTime != null" class="exec-time">
              <field-time-outlined />
              {{
                record.executionTime < 1000
                  ? record.executionTime + 'ms'
                  : (record.executionTime / 1000).toFixed(2) + 's'
              }}
            </span>
            <span v-else class="exec-time-na">-</span>
          </template>

          <!-- 状态 -->
          <template v-else-if="column.dataIndex === 'status'">
            <a-tag :color="record.status === 0 ? 'red' : 'green'">
              {{ record.status === 0 ? '失败' : '成功' }}
            </a-tag>
          </template>

          <!-- 操作 -->
          <template v-else-if="column.dataIndex === 'actions'">
            <a-button type="link" size="small" @click="handleViewDetail(record)"> 详情 </a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 详情抽屉 -->
    <a-drawer v-model:open="detailVisible" title="操作日志详情" width="520" placement="right">
      <template v-if="currentLog">
        <div class="detail-content">
          <a-descriptions title="基本信息" bordered size="small" :column="2">
            <a-descriptions-item label="日志ID" :span="2">
              {{ currentLog.id }}
            </a-descriptions-item>
            <a-descriptions-item label="操作时间" :span="2">
              {{ formatDateTime(currentLog.operationTime) }}
            </a-descriptions-item>
            <a-descriptions-item label="模块">
              <a-tag :color="getModuleColor(currentLog.module)">{{ currentLog.module }}</a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="操作类型">
              {{ getOperationTypeText(currentLog.operationType) }}
            </a-descriptions-item>
            <a-descriptions-item label="操作人" :span="2">
              {{ currentLog.operator || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="IP 地址" :span="2">
              {{ currentLog.ipAddress || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="执行耗时" :span="2">
              <span v-if="currentLog.executionTime != null">
                {{ currentLog.executionTime }}ms
              </span>
              <span v-else>-</span>
            </a-descriptions-item>
            <a-descriptions-item label="状态" :span="2">
              <a-tag :color="currentLog.status === 0 ? 'red' : 'green'">
                {{ currentLog.status === 0 ? '失败' : '成功' }}
              </a-tag>
            </a-descriptions-item>
          </a-descriptions>

          <a-descriptions
            title="请求信息"
            bordered
            size="small"
            :column="1"
            style="margin-top: 16px"
          >
            <a-descriptions-item label="请求 URL">
              {{ currentLog.requestUrl || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="请求方法">
              <a-tag
                v-if="currentLog.requestMethod"
                :color="getMethodColor(currentLog.requestMethod)"
              >
                {{ currentLog.requestMethod }}
              </a-tag>
              <span v-else>-</span>
            </a-descriptions-item>
          </a-descriptions>

          <a-descriptions
            title="操作描述"
            bordered
            size="small"
            :column="1"
            style="margin-top: 16px"
          >
            <a-descriptions-item label="描述内容">
              {{ currentLog.description || '-' }}
            </a-descriptions-item>
          </a-descriptions>

          <a-descriptions
            v-if="currentLog.status === 0 && currentLog.errorMessage"
            title="错误信息"
            bordered
            size="small"
            :column="1"
            style="margin-top: 16px"
          >
            <a-descriptions-item label="错误详情">
              <div class="error-msg">{{ currentLog.errorMessage }}</div>
            </a-descriptions-item>
          </a-descriptions>
        </div>
      </template>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
  import { ref, reactive, computed, onMounted } from 'vue'
  import { message } from 'ant-design-vue'
  import {
    ReloadOutlined,
    SearchOutlined,
    UserOutlined,
    FileTextOutlined,
    CheckCircleOutlined,
    CloseCircleOutlined,
    AppstoreOutlined,
    FieldTimeOutlined,
  } from '@ant-design/icons-vue'
  import dayjs from 'dayjs'
  import { useOperationLogStore } from '@/stores/operationLog'
  import type { OperationLogRecord } from '@/api/operationLog'

  const store = useOperationLogStore()

  // ---- 状态 ----
  const isLoading = computed(() => store.isLoading)
  const logs = computed(() => store.logs)
  const pagination = computed(() => store.pagination)
  const stats = computed(() => store.stats)
  const moduleOptions = computed(() => store.moduleOptions)

  const detailVisible = ref(false)
  const currentLog = ref<OperationLogRecord | null>(null)

  // ---- 搜索表单 ----
  const searchForm = reactive({
    module: undefined as string | undefined,
    operator: '',
    status: undefined as number | undefined,
    dateRange: [] as string[],
  })

  // ---- 表格列定义 ----
  const columns = [
    { title: '操作时间', dataIndex: 'operationTime', key: 'operationTime', width: 110 },
    { title: '模块', dataIndex: 'module', key: 'module', width: 100 },
    { title: '操作类型', dataIndex: 'operationType', key: 'operationType', width: 100 },
    { title: '操作描述', dataIndex: 'description', key: 'description', width: 200, ellipsis: true },
    { title: '操作人', dataIndex: 'operator', key: 'operator', width: 110 },
    { title: '耗时', dataIndex: 'executionTime', key: 'executionTime', width: 90 },
    { title: '状态', dataIndex: 'status', key: 'status', width: 70 },
    { title: '操作', dataIndex: 'actions', key: 'actions', width: 70, fixed: 'right' },
  ]

  // ---- 分页配置 ----
  const paginationConfig = computed(() => ({
    current: store.pagination.page,
    pageSize: store.pagination.size,
    total: store.pagination.total,
    pageSizeOptions: ['10', '20', '50'],
    showSizeChanger: true,
    showQuickJumper: true,
    showTotal: (total: number) => `共 ${total} 条记录`,
  }))

  // ---- 工具方法 ----
  function formatDate(dateStr: string, fmt = 'YYYY-MM-DD') {
    if (!dateStr) return ''
    return dayjs(dateStr).format(fmt)
  }

  function formatDateTime(dateStr: string) {
    return formatDate(dateStr, 'YYYY-MM-DD HH:mm:ss')
  }

  function getFirstChar(str?: string) {
    if (!str) return '?'
    return str.charAt(0).toUpperCase()
  }

  function getModuleColor(module?: string) {
    const colors: Record<string, string> = {
      客户管理: 'blue',
      商品管理: 'cyan',
      库存管理: 'green',
      礼品管理: 'purple',
      订单管理: 'orange',
      系统管理: 'geekblue',
      用户管理: 'lime',
      数据字典: 'gold',
      登录认证: 'magenta',
    }
    return colors[module || ''] || 'default'
  }

  function getOperationTypeText(type?: string) {
    const map: Record<string, string> = {
      CREATE: '新增',
      CONTENT_UPDATE: '修改内容',
      STATUS_UPDATE: '修改状态',
      BOTH_UPDATE: '修改内容+状态',
      DELETE: '删除',
      OTHER: '其他',
    }
    return map[type || ''] || type || '-'
  }

  function getMethodColor(method?: string) {
    const map: Record<string, string> = {
      GET: 'blue',
      POST: 'green',
      PUT: 'orange',
      DELETE: 'red',
      PATCH: 'cyan',
    }
    return map[method || ''] || 'default'
  }

  // ---- 数据加载 ----
  async function loadData(params?: { page?: number; size?: number }) {
    const queryParams: Record<string, unknown> = {
      page: params?.page ?? 0,
      size: params?.size ?? store.pagination.size,
    }

    // 结构化筛选参数
    if (searchForm.module) queryParams.module = searchForm.module
    if (searchForm.operator) queryParams.operator = searchForm.operator
    if (searchForm.status !== undefined) queryParams.status = searchForm.status
    if (searchForm.dateRange?.length === 2) {
      queryParams.startTime = searchForm.dateRange[0]
      queryParams.endTime = searchForm.dateRange[1]
    }

    await store.loadLogs(queryParams as any)
  }

  async function handleSearch() {
    await loadData({ page: 0 })
  }

  async function handleReset() {
    searchForm.module = undefined
    searchForm.operator = ''
    searchForm.status = undefined
    searchForm.dateRange = []
    await loadData({ page: 0 })
  }

  async function handleRefresh() {
    await loadData()
  }

  function handleTableChange(pag: any) {
    loadData({ page: pag.current - 1, size: pag.pageSize })
  }

  function handleViewDetail(record: OperationLogRecord) {
    currentLog.value = record
    detailVisible.value = true
  }

  // ---- 初始化 ----
  onMounted(async () => {
    await loadData({ page: 0 })
  })
</script>

<style scoped>
  .page-container {
    padding: 20px;
    background: var(--bg-page);
    min-height: 100%;
  }

  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
  }

  .page-title {
    font-size: 24px;
    font-weight: 700;
    color: var(--text-primary);
    margin: 0;
    transition: color 0.3s ease;
  }

  /* 统计卡片 */
  .stats-row {
    margin-bottom: 16px;
  }

  .stat-card {
    border-radius: 8px;
    transition: box-shadow 0.3s ease;
  }

  .stat-card :deep(.ant-card-body) {
    padding: 16px 20px;
  }

  .stat-card-inner {
    display: flex;
    align-items: center;
    gap: 14px;
  }

  .stat-icon-wrap {
    width: 44px;
    height: 44px;
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  .stat-info {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  .stat-label {
    font-size: 13px;
    color: var(--text-secondary);
    line-height: 1.4;
    transition: color 0.3s ease;
  }

  .stat-value {
    font-size: 22px;
    font-weight: 700;
    color: var(--text-primary);
    line-height: 1.3;
    transition: color 0.3s ease;
  }

  /* 搜索卡片 */
  .search-card {
    margin-bottom: 16px;
    border-radius: 8px;
  }

  .search-card :deep(.ant-card-body) {
    padding: 16px 20px;
  }

  .search-actions {
    display: flex;
    justify-content: flex-end;
    margin-top: 12px;
    width: 100%;
  }

  /* 表格卡片 */
  .table-card {
    border-radius: 8px;
  }

  /* 时间列 */
  .time-cell {
    display: flex;
    flex-direction: column;
    font-size: 12px;
    line-height: 1.4;
  }

  .time-date {
    font-weight: 500;
    color: var(--text-primary);
  }

  .time-time {
    color: var(--text-secondary);
  }

  /* 操作描述 */
  .desc-cell {
    max-width: 240px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    color: var(--text-primary);
  }

  /* 操作人 */
  .operator-cell {
    display: flex;
    align-items: center;
    gap: 6px;
    color: var(--text-primary);
  }

  /* 执行时间 */
  .exec-time {
    color: var(--text-secondary);
    font-size: 13px;
  }

  .exec-time-na {
    color: var(--text-tertiary);
  }

  /* 操作类型 */
  .op-type {
    font-size: 13px;
    color: var(--text-primary);
  }

  /* 详情抽屉 */
  .detail-content {
    padding: 4px;
  }

  .error-msg {
    color: #ff4d4f;
    white-space: pre-wrap;
    word-break: break-all;
    font-size: 13px;
    line-height: 1.6;
  }

  /* 暗色模式适配：抽屉内描述列表 */
  [data-theme='dark'] .detail-content :deep(.ant-descriptions-title) {
    color: #e0e0e0;
  }
  [data-theme='dark'] .detail-content :deep(.ant-descriptions-item-label) {
    color: #a0a0a0;
  }
  [data-theme='dark'] .detail-content :deep(.ant-descriptions-item-content) {
    color: var(--text-primary);
  }

  /* 响应式 */
  @media (max-width: 768px) {
    .page-header {
      flex-direction: column;
      align-items: flex-start;
      gap: 12px;
    }
    .page-title {
      font-size: 20px;
    }
  }
</style>
