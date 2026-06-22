<template>
  <div class="customer-list-page">
    <div class="page-header">
      <h1 class="page-title">客户管理</h1>
      <div class="page-actions">
        <a-button type="primary" @click="showAddModal" class="add-btn">
          <template #icon>
            <plus-outlined />
          </template>
          新增客户
        </a-button>
        <a-button @click="showImportModal" :loading="importLoading">
          <template #icon>
            <import-outlined />
          </template>
          导入客户
        </a-button>
        <a-button @click="handleExport" :loading="exportLoading">
          <template #icon>
            <export-outlined />
          </template>
          导出
        </a-button>
        <a-tooltip title="刷新">
          <a-button @click="handleRefresh" :loading="isLoading || statsLoading">
            <template #icon>
              <reload-outlined />
            </template>
          </a-button>
        </a-tooltip>
        <a-button @click="handleBack" style="margin-right:8px">
          <template #icon>
            <home-outlined />
          </template>
          返回仪表盘
        </a-button>
      </div>
    </div>

    <a-card class="search-card">
      <a-form class="search-form" layout="inline" :model="searchForm" @finish="handleSearch">
        <a-row :gutter="[16, 16]" style="width: 100%">
          <a-col :xs="24" :sm="12" :md="8" :lg="6">
            <a-form-item label="关键词">
              <a-input
                v-model:value="searchForm.keyword"
                placeholder="姓名/手机号/邮箱"
                allow-clear
              />
            </a-form-item>
          </a-col>

          <a-col :xs="24" :sm="12" :md="8" :lg="5">
            <a-form-item label="礼品等级">
              <a-select
                v-model:value="searchForm.giftLevel"
                placeholder="请选择"
                allow-clear
                style="width: 100%"
              >
                <a-select-option :value="0">普通客户</a-select-option>
                <a-select-option :value="1">等级1</a-select-option>
                <a-select-option :value="2">等级2</a-select-option>
                <a-select-option :value="3">等级3</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>

          <a-col :xs="24" :sm="12" :md="8" :lg="5">
            <a-form-item label="状态">
              <a-select
                v-model:value="searchForm.status"
                placeholder="请选择"
                allow-clear
                style="width: 100%"
              >
                <a-select-option :value="1">正常</a-select-option>
                <a-select-option :value="0">停用</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>

          <a-col :xs="24" :sm="12" :md="10" :lg="7">
            <a-form-item label="创建时间">
              <a-range-picker
                v-model:value="searchForm.dateRange"
                style="width: 100%"
                :placeholder="['开始日期', '结束日期']"
              />
            </a-form-item>
          </a-col>
        </a-row>

        <a-row style="width: 100%; margin-top: 8px">
          <a-col :xs="24" style="display: flex; justify-content: flex-end">
            <a-space>
              <a-button type="primary" html-type="submit" :loading="isLoading">
                搜索
              </a-button>
              <a-button @click="handleReset">
                重置
              </a-button>
            </a-space>
          </a-col>
        </a-row>
      </a-form>
    </a-card>

    <a-row :gutter="[16, 16]" class="stats-row">
      <a-col v-for="card in customerStatCards" :key="card.key" :xs="24" :sm="12" :lg="6">
        <a-card class="stats-card" :loading="statsLoading">
          <div class="stats-primary-line">
            <span class="stats-title">{{ card.label }}</span>
            <span class="stats-value-inline">{{ card.value }}</span>
          </div>
          <div class="stats-secondary-line">{{ card.helper }}</div>
        </a-card>
      </a-col>
    </a-row>

    <a-card class="table-card">
      <a-table
        :columns="columns"
        :data-source="customers"
        :loading="isLoading"
        :pagination="pagination"
        :row-selection="rowSelection"
        row-key="id"
        @change="handleTableChange"
        size="middle"
        bordered
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'name'">
            <div class="customer-name">
              <a-avatar :size="32" :style="{ backgroundColor: getCustomerColor(record.name) }">
                {{ getFirstChar(record.name) }}
              </a-avatar>
              <div class="name-info">
                <span class="name">{{ record.name }}</span>
                <div class="customer-tags">
                  <a-tag
                    v-if="record.giftLevel > 0"
                    :color="giftLevelColors[record.giftLevel]"
                    size="small"
                  >
                    等级{{ record.giftLevel }}
                  </a-tag>
                  <a-tag
                    v-if="record.referrerId"
                    color="blue"
                    size="small"
                  >
                    转介绍
                  </a-tag>
                </div>
              </div>
            </div>
          </template>

          <template v-else-if="column.dataIndex === 'phone'">
            <div class="contact-info">
              <div>{{ record.phone }}</div>
              <div v-if="record.email" class="email">{{ record.email }}</div>
            </div>
          </template>

          <template v-else-if="column.dataIndex === 'birthday'">
            <div v-if="record.birthday">
              <div>{{ formatDate(record.birthday) }}</div>
              <div class="age-zodiac">
                <a-tag size="small" color="blue">{{ calculateAge(record.birthday) }}岁</a-tag>
              </div>
            </div>
            <span v-else>-</span>
          </template>

          <template v-else-if="column.dataIndex === 'gender'">
            <a-tag :color="record.gender === 1 ? 'blue' : 'pink'">
              {{ getGenderText(record.gender) }}
            </a-tag>
          </template>

          <template v-else-if="column.dataIndex === 'status'">
            <a-tag :color="record.status === 1 ? 'green' : 'red'">
              {{ record.status === 1 ? '正常' : '停用' }}
            </a-tag>
          </template>

          <template v-else-if="column.dataIndex === 'giftLevel'">
            <a-tag :color="giftLevelColors[record.giftLevel || 0]">
              {{ getGiftLevelText(record.giftLevel || 0) }}
            </a-tag>
          </template>

          <template v-else-if="column.dataIndex === 'registeredAt'">
            <div v-if="record.registeredAt">
              {{ formatDate(record.registeredAt) }}
            </div>
            <span v-else>-</span>
          </template>

          <template v-else-if="column.dataIndex === 'actions'">
            <a-space size="small">
              <a-button type="link" size="small" @click="handleView(record)">
                查看
              </a-button>
              <a-button type="link" size="small" @click="handleEdit(record)">
                编辑
              </a-button>
              <a-button
                v-if="record.status === 1"
                type="link"
                @click="handleDisable(record)"
                danger
              >
                <stop-outlined />
                停用
              </a-button>
              <a-button
                v-else
                type="link"
                @click="handleEnable(record)"
              >
                <check-outlined />
                启用
              </a-button>
              <a-button
                type="link"
                size="small"
                danger
                @click="handleDelete(record)"
              >
                删除
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>

      <div v-if="selectedRowKeys.length > 0" class="batch-actions">
        <span class="selected-count">
          已选择 {{ selectedRowKeys.length }} 项
        </span>
        <a-space>
          <a-button @click="handleBatchExport">
            导出选中
          </a-button>
          <a-button
            @click="handleBatchEnable"
            v-if="hasDisabledSelected"
          >
            批量启用
          </a-button>
          <a-button
            @click="handleBatchDisable"
            danger
            v-else
          >
            批量停用
          </a-button>
          <a-button @click="handleBatchDelete" danger>
            批量删除
          </a-button>
          <a-button @click="clearSelection">
            取消选择
          </a-button>
        </a-space>
      </div>
    </a-card>

    <a-modal
      v-model:open="importModalVisible"
      title="导入客户"
      width="760px"
      :footer="null"
      @cancel="closeImportModal"
    >
      <div class="import-section">
        <div class="import-header">
          <div>
            <div class="import-title">CSV 模板说明</div>
            <div class="import-subtitle">导入结果会返回失败明细，成功数据将自动刷新当前列表与统计。</div>
          </div>
          <a-space>
            <a-button size="small" :disabled="!customerImportMeta" @click="copyImportTemplateHeader">复制模板表头</a-button>
            <label class="import-picker">
              <input :key="importInputKey" type="file" accept=".csv,text/csv" @change="handleImportFileChange" />
              <span>选择文件</span>
            </label>
            <a-button type="primary" :loading="importLoading" @click="submitImport">
              开始导入
            </a-button>
          </a-space>
        </div>

        <a-alert
          v-if="importMetaError"
          type="warning"
          show-icon
          :message="importMetaError"
          class="import-alert"
        />
        <a-alert
          v-else-if="importMetaLoading"
          type="info"
          show-icon
          message="正在加载后端导入模板说明..."
          class="import-alert"
        />

        <a-alert
          v-if="selectedImportFile"
          type="info"
          show-icon
          :message="`已选择文件：${selectedImportFile.name}`"
          class="import-alert"
        />

        <a-card v-if="customerImportMeta" size="small" class="import-meta-card">
          <div class="meta-row">
            <span class="meta-label">模板字段</span>
            <a-space wrap>
              <a-tag v-for="field in customerImportMeta.templateFields" :key="field">{{ field }}</a-tag>
            </a-space>
          </div>
          <div class="meta-row">
            <span class="meta-label">必填字段</span>
            <a-space wrap>
              <a-tag v-for="field in customerImportMeta.requiredFields" :key="field" color="red">{{ field }}</a-tag>
            </a-space>
          </div>
          <div class="meta-row single-line">
            <span class="meta-label">判重策略</span>
            <span>{{ customerImportMeta.duplicateStrategy }}</span>
          </div>
          <div class="meta-row">
            <span class="meta-label">补充说明</span>
            <ul class="meta-list">
              <li v-for="note in customerImportMeta.notes" :key="note">{{ note }}</li>
            </ul>
          </div>
        </a-card>

        <a-card v-if="customerImportResult" size="small" class="import-result-card">
          <div class="result-summary">
            <a-statistic title="成功导入" :value="customerImportResult.successCount" />
            <a-statistic title="失败条数" :value="customerImportResult.failureCount" />
          </div>

          <a-table
            v-if="customerImportResult.failureDetails.length"
            :data-source="customerImportResult.failureDetails"
            :pagination="false"
            size="small"
            row-key="rowNumber"
          >
            <a-table-column title="行号" data-index="rowNumber" key="rowNumber" width="80" />
            <a-table-column title="标识" data-index="identifier" key="identifier" />
            <a-table-column title="失败原因" data-index="reason" key="reason" />
          </a-table>
          <a-empty v-else description="本次导入没有失败记录" />
        </a-card>
      </div>
    </a-modal>

    <a-modal
      v-model:open="modalVisible"
      :title="modalTitle"
      width="600px"
      :confirm-loading="modalLoading"
      @ok="handleModalOk"
      @cancel="handleModalCancel"
    >
      <a-form
        ref="formRef"
        :model="formState"
        :rules="rules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
        @finish="handleModalOk"
      >
        <a-form-item label="姓名" name="name">
          <a-input v-model:value="formState.name" placeholder="请输入客户姓名" />
        </a-form-item>

        <a-form-item label="手机号" name="phone">
          <a-input v-model:value="formState.phone" placeholder="请输入手机号" />
        </a-form-item>

        <a-form-item label="邮箱" name="email">
          <a-input v-model:value="formState.email" placeholder="请输入邮箱" />
        </a-form-item>

        <a-form-item label="性别" name="gender">
          <a-radio-group v-model:value="formState.gender">
            <a-radio :value="1">男</a-radio>
            <a-radio :value="0">女</a-radio>
          </a-radio-group>
        </a-form-item>

        <a-form-item label="生日" name="birthday">
          <a-date-picker
            v-model:value="formState.birthday"
            style="width: 100%"
            placeholder="请选择生日"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
          />
        </a-form-item>

        <a-form-item label="地址" name="address">
          <a-textarea
            v-model:value="formState.address"
            placeholder="请输入地址"
            :rows="2"
          />
        </a-form-item>

        <a-form-item label="礼品等级" name="giftLevel">
          <a-select
            v-model:value="formState.giftLevel"
            placeholder="请选择礼品等级"
          >
            <a-select-option :value="0">普通客户</a-select-option>
            <a-select-option :value="1">等级1</a-select-option>
            <a-select-option :value="2">等级2</a-select-option>
            <a-select-option :value="3">等级3</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="推荐人" name="referrerId">
          <a-select
            v-model:value="formState.referrerId"
            placeholder="请输入推荐人姓名或手机号搜索"
            show-search
            allow-clear
            :filter-option="false"
            :not-found-content="referrerLoading ? undefined : '暂无数据'"
            @search="handleReferrerSearch"
            @focus="handleReferrerFocus"
          >
            <a-select-option
              v-for="referrer in referrerList"
              :key="referrer.id"
              :value="referrer.id"
            >
              <div class="referrer-option">
                <span class="referrer-name">{{ referrer.name }}</span>
                <span class="referrer-phone">{{ referrer.phone }}</span>
              </div>
            </a-select-option>
            <template #notFoundContent>
              <a-spin v-if="referrerLoading" size="small" />
              <span v-else>暂无数据</span>
            </template>
          </a-select>
        </a-form-item>

        <a-form-item label="状态" name="status">
          <a-switch
            v-model:checked="formState.status"
            checked-children="启用"
            un-checked-children="停用"
            :checked-value="1"
            :un-checked-value="0"
          />
        </a-form-item>

        <a-form-item label="备注" name="remark">
          <a-textarea
            v-model:value="formState.remark"
            placeholder="请输入备注"
            :rows="3"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-drawer
      v-model:open="drawerVisible"
      title="客户详情"
      width="400"
      placement="right"
    >
      <template v-if="currentCustomer">
        <div class="customer-detail">
          <div class="detail-header">
            <a-avatar :size="64" :style="{ backgroundColor: getCustomerColor(currentCustomer.name) }">
              {{ getFirstChar(currentCustomer.name) }}
            </a-avatar>
            <div class="header-info">
              <h2>{{ currentCustomer.name }}</h2>
              <div class="customer-tags">
                <a-tag
                  v-if="currentCustomer.giftLevel > 0"
                  :color="giftLevelColors[currentCustomer.giftLevel]"
                >
                  等级{{ currentCustomer.giftLevel }}
                </a-tag>
                <a-tag :color="currentCustomer.status === 1 ? 'green' : 'red'">
                  {{ currentCustomer.status === 1 ? '正常' : '停用' }}
                </a-tag>
              </div>
            </div>
          </div>

          <a-divider />

          <div class="detail-info">
            <div class="info-item">
              <span class="label">手机号：</span>
              <span class="value">{{ currentCustomer.phone }}</span>
            </div>
            <div class="info-item" v-if="currentCustomer.email">
              <span class="label">邮箱：</span>
              <span class="value">{{ currentCustomer.email }}</span>
            </div>
            <div class="info-item" v-if="currentCustomer.gender !== undefined">
              <span class="label">性别：</span>
              <span class="value">{{ currentCustomer.gender === 1 ? '男' : '女' }}</span>
            </div>
            <div class="info-item" v-if="currentCustomer.birthday">
              <span class="label">生日：</span>
              <span class="value">{{ formatDate(currentCustomer.birthday) }}</span>
            </div>
            <div class="info-item" v-if="currentCustomer.address">
              <span class="label">地址：</span>
              <span class="value">{{ currentCustomer.address }}</span>
            </div>
            <div class="info-item">
              <span class="label">创建时间：</span>
              <span class="value">{{ formatDate(currentCustomer.registeredAt) }}</span>
            </div>
            <div class="info-item" v-if="currentCustomer.remark">
              <span class="label">备注：</span>
              <span class="value">{{ currentCustomer.remark }}</span>
            </div>
          </div>

          <a-divider />

          <div class="detail-actions">
            <a-button type="primary" @click="handleEdit(currentCustomer)" block>
              编辑
            </a-button>
          </div>
        </div>
      </template>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  message,
  Modal,
  type FormInstance,
  type TableProps,
  type TableColumnType,
} from 'ant-design-vue'
import {
  PlusOutlined,
  ExportOutlined,
  ImportOutlined,
  ReloadOutlined,
  StopOutlined,
  CheckOutlined,
  HomeOutlined,
} from '@ant-design/icons-vue'
import { useCustomerStore } from '@/stores/customer'
import { customerApi } from '@/api/customer'
import type { Customer, CustomerCreateDTO, PageParams } from '@/types'
import dayjs from 'dayjs'
import {
  buildCustomerImportMeta,
  type CustomerStatistics,
  type ImportResult,
} from '@/utils/featureEnhancements'

const router = useRouter()
const route = useRoute()
const customerStore = useCustomerStore()
const formRef = ref<FormInstance>()
const referrerList = ref<Customer[]>([])
const referrerLoading = ref(false)
const referrerSearchKeyword = ref('')

const exportLoading = ref(false)
const statsLoading = ref(false)
const modalVisible = ref(false)
const modalLoading = ref(false)
const drawerVisible = ref(false)
const importModalVisible = ref(false)
const importLoading = ref(false)
const importMetaLoading = ref(false)
const importMetaError = ref('')
const importInputKey = ref(0)
const selectedImportFile = ref<File | null>(null)
const customerImportTemplate = ref<ImportResult | null>(null)
const customerImportResult = ref<ImportResult | null>(null)
const modalType = ref<'add' | 'edit'>('add')
const currentCustomer = ref<Customer | null>(null)
const selectedRowKeys = ref<number[]>([])
const customerStatistics = ref<CustomerStatistics>({
  totalCustomers: 0,
  normalCustomers: 0,
  disabledCustomers: 0,
  giftLevelDistribution: {},
})

const columns: TableColumnType[] = [
  {
    title: '客户信息',
    dataIndex: 'name',
    key: 'name',
    width: 200,
  },
  {
    title: '联系方式',
    dataIndex: 'phone',
    key: 'phone',
    width: 150,
  },
  {
    title: '性别',
    dataIndex: 'gender',
    key: 'gender',
    width: 80,
    filters: [
      { text: '男', value: 1 },
      { text: '女', value: 0 },
    ],
  },
  {
    title: '生日',
    dataIndex: 'birthday',
    key: 'birthday',
    width: 120,
  },
  {
    title: '礼品等级',
    dataIndex: 'giftLevel',
    key: 'giftLevel',
    width: 100,
  },
  {
    title: '状态',
    dataIndex: 'status',
    key: 'status',
    width: 100,
  },
  {
    title: '注册日期',
    dataIndex: 'registeredAt',
    key: 'registeredAt',
    width: 150,
    sorter: true,
  },
  {
    title: '操作',
    dataIndex: 'actions',
    key: 'actions',
    width: 200,
    fixed: 'right' as const,
  },
]

const searchForm = reactive({
  keyword: '',
  giftLevel: undefined as number | undefined,
  status: undefined as 0 | 1 | undefined,
  dateRange: [] as any[],
})

const formState = reactive<CustomerCreateDTO & { status: 0 | 1 }>({
  name: '',
  phone: '',
  email: '',
  gender: 1,
  birthday: undefined as string | undefined,
  address: '',
  giftLevel: 0,
  status: 1,
  remark: '',
  referrerId: undefined,
})

const rules = {
  name: [
    { required: true, message: '请输入客户姓名', trigger: 'blur' },
    { min: 2, max: 20, message: '姓名长度2-20个字符', trigger: 'blur' },
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' },
  ],
  email: [
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' },
  ],
}

const customers = computed(() => customerStore.customers)
const isLoading = computed(() => customerStore.isLoading)
const pagination = computed(() => ({
  current: customerStore.pagination.page,
  pageSize: customerStore.pagination.size,
  total: customerStore.pagination.total,
  pageSizeOptions: ['5', '10', '20'],
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条`,
}))
const rowSelection = computed<TableProps['rowSelection']>(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (selectedKeys) => {
    selectedRowKeys.value = selectedKeys.map((key) => Number(key))
  },
}))
const modalTitle = computed(() => modalType.value === 'add' ? '新增客户' : '编辑客户')
const hasDisabledSelected = computed(() =>
  selectedRowKeys.value.some((key) => {
    const customer = customers.value.find((item) => item.id === key)
    return customer?.status === 0
  }),
)
const customerImportMeta = computed(() => buildCustomerImportMeta(customerImportResult.value || customerImportTemplate.value || undefined))
const customerStatCards = computed(() => {
  const distribution = customerStatistics.value.giftLevelDistribution || {}
  return [
    {
      key: 'totalCustomers',
      label: '总客户数',
      value: customerStatistics.value.totalCustomers,
      helper: `正常 ${customerStatistics.value.normalCustomers} / 停用 ${customerStatistics.value.disabledCustomers}`,
    },
    {
      key: 'normalCustomers',
      label: '正常客户',
      value: customerStatistics.value.normalCustomers,
      helper: `普通客户 ${distribution[0] || 0}`,
    },
    {
      key: 'giftLevel2',
      label: '等级 2 客户',
      value: distribution[2] || 0,
      helper: `等级 1 ${distribution[1] || 0} / 等级 3 ${distribution[3] || 0}`,
    },
    {
      key: 'disabledCustomers',
      label: '停用客户',
      value: customerStatistics.value.disabledCustomers,
      helper: '导入成功后自动刷新统计',
    },
  ]
})

const giftLevelColors: Record<number, string> = {
  0: 'default',
  1: 'blue',
  2: 'green',
  3: 'orange',
}

const getFirstChar = (name: string) => name.charAt(0).toUpperCase()
const getCustomerColor = (name: string) => {
  const colors = ['#1890ff', '#52c41a', '#faad14', '#f5222d']
  const index = name.charCodeAt(0) % colors.length
  return colors[index]
}
const getGiftLevelText = (level: number) => ['普通客户', '等级1', '等级2', '等级3'][level] || '普通客户'
const formatDate = (dateStr?: string, format?: string) => {
  if (!dateStr) return '-'
  return dayjs(dateStr).format(format || 'YYYY-MM-DD')
}
const getGenderText = (gender: 0 | 1 | undefined | null): string => {
  if (gender === 1) return '男'
  if (gender === 0) return '女'
  return '未知'
}
const calculateAge = (birthday: string): number => {
  const today = new Date()
  const birthDate = new Date(birthday)
  let age = today.getFullYear() - birthDate.getFullYear()
  const monthDiff = today.getMonth() - birthDate.getMonth()

  if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birthDate.getDate())) {
    age--
  }

  return age
}

const buildActiveSearchParams = (page?: number, size?: number): PageParams => ({
  page: page ?? customerStore.pagination.page - 1,
  size: size ?? customerStore.pagination.size,
  keyword: searchForm.keyword,
  giftLevel: searchForm.giftLevel,
  status: searchForm.status,
  dateRange: searchForm.dateRange,
})

const downloadCustomersAsCsv = (records: Customer[], fileName: string) => {
  const rows = [
    ['姓名', '手机号', '邮箱', '性别', '礼品等级', '状态', '生日', '地址', '推荐人', '备注'],
    ...records.map((record) => [
      record.name,
      record.phone,
      record.email || '',
      getGenderText(record.gender),
      getGiftLevelText(record.giftLevel),
      record.status === 1 ? '正常' : '停用',
      record.birthday || '',
      record.address || '',
      record.referrerName || '',
      record.remark || '',
    ]),
  ]
  const csv = `\uFEFF${rows.map((row) => row.map((cell) => `"${String(cell).replace(/"/g, '""')}"`).join(',')).join('\r\n')}`
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  link.click()
  window.URL.revokeObjectURL(url)
}

const loadCustomers = async (params?: PageParams) => {
  try {
    const filteredParams = Object.fromEntries(
      Object.entries(params || {}).filter(([_, value]) => value != null && value !== ''),
    )
    if (filteredParams.sort == null) {
      filteredParams.sort = 'registeredAt'
      filteredParams.direction = 'desc'
    }
    await customerStore.loadCustomers(filteredParams)
  } catch {
    message.error('加载客户列表失败')
  }
}

const loadCustomerStatistics = async () => {
  try {
    statsLoading.value = true
    const stats = await customerApi.getStatistics() as unknown as CustomerStatistics
    customerStatistics.value = {
      totalCustomers: stats.totalCustomers || 0,
      normalCustomers: stats.normalCustomers || 0,
      disabledCustomers: stats.disabledCustomers || 0,
      giftLevelDistribution: stats.giftLevelDistribution || {},
    }
  } catch {
    message.error('加载客户统计失败')
  } finally {
    statsLoading.value = false
  }
}

const refreshCustomerData = async () => {
  await Promise.all([loadCustomers(buildActiveSearchParams()), loadCustomerStatistics()])
}

const handleSearch = () => {
  selectedRowKeys.value = []
  loadCustomers(buildActiveSearchParams(0))
}

const handleReset = () => {
  searchForm.keyword = ''
  searchForm.giftLevel = undefined
  searchForm.status = undefined
  searchForm.dateRange = []
  handleSearch()
}

const handleTableChange: TableProps['onChange'] = (tablePagination, filters, sorter) => {
  const params: Record<string, unknown> = {
    ...buildActiveSearchParams((tablePagination.current || 1) - 1, tablePagination.pageSize),
  }

  if (filters.gender && filters.gender.length > 0 && filters.gender.length < 2) {
    params.gender = filters.gender[0]
  }

  if (filters.status && filters.status.length > 0) {
    params.status = filters.status[0]
  }

  const activeSorter = Array.isArray(sorter) ? sorter[0] : sorter
  if (activeSorter?.field) {
    params.sort = activeSorter.field
    params.direction = activeSorter.order === 'ascend' ? 'asc' : 'desc'
  }

  loadCustomers(params)
}

const handleRefresh = async () => {
  await refreshCustomerData()
  message.success('刷新成功')
}

const handleView = (record: Customer) => {
  if (!record.id) {
    message.warning('当前客户缺少详情标识，无法打开详情页')
    return
  }

  router.push(`/customers/${record.id}`)
}

const handleEdit = async (record: Customer) => {
  modalType.value = 'edit'
  currentCustomer.value = record
  Object.assign(formState, {
    name: record.name,
    phone: record.phone,
    email: record.email,
    referrerId: record.referrerId,
    referrerName: record.referrerName,
    gender: record.gender || 1,
    birthday: record.birthday,
    address: record.address || '',
    giftLevel: record.giftLevel || 0,
    status: record.status,
    remark: record.remark || '',
  })

  if (formState.referrerId && !referrerList.value.find((item) => item.id === formState.referrerId)) {
    const referrer = await customerStore.findCustomerById(formState.referrerId)
    if (referrer) {
      referrerList.value = [referrer, ...referrerList.value]
    }
  }

  modalVisible.value = true
}

const handleDelete = (record: Customer) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除客户 "${record.name}" 吗？`,
    okText: '确定',
    cancelText: '取消',
    okType: 'danger',
    onOk: async () => {
      try {
        await customerStore.deleteCustomer(record.id!)
        message.success('删除成功')
        await refreshCustomerData()
      } catch {
        message.error('删除失败')
      }
    },
  })
}

const handleDisable = (record: Customer) => {
  Modal.confirm({
    title: '确认停用',
    content: `确定要停用客户 "${record.name}" 吗？`,
    okText: '确定',
    cancelText: '取消',
    okType: 'danger',
    onOk: async () => {
      try {
        await customerStore.updateCustomer(record.id!, { status: 0 })
        message.success('停用成功')
        await refreshCustomerData()
      } catch {
        message.error('停用失败')
      }
    },
  })
}

const handleEnable = (record: Customer) => {
  Modal.confirm({
    title: '确认启用',
    content: `确定要启用客户 "${record.name}" 吗？`,
    okText: '确定',
    cancelText: '取消',
    onOk: async () => {
      try {
        await customerStore.updateCustomer(record.id!, { status: 1 })
        message.success('启用成功')
        await refreshCustomerData()
      } catch {
        message.error('启用失败')
      }
    },
  })
}

const showAddModal = () => {
  modalType.value = 'add'
  formRef.value?.resetFields()
  Object.assign(formState, {
    name: '',
    phone: '',
    email: '',
    gender: 1,
    referrerId: undefined,
    birthday: undefined,
    address: '',
    giftLevel: 0,
    status: 1,
    remark: '',
  })
  modalVisible.value = true
}

const handleModalOk = async () => {
  try {
    await formRef.value?.validate()
    modalLoading.value = true

    if (modalType.value === 'add') {
      await customerStore.addCustomer(formState)
      message.success('添加成功')
    } else {
      await customerStore.updateCustomer(currentCustomer.value!.id!, formState)
      message.success('更新成功')
    }

    formRef.value?.resetFields()
    modalVisible.value = false
    await refreshCustomerData()
  } catch {
    message.error('保存失败')
  } finally {
    modalLoading.value = false
  }
}

const handleModalCancel = () => {
  modalVisible.value = false
  formRef.value?.resetFields()
}

const handleExport = async () => {
  try {
    exportLoading.value = true
    const response = await customerStore.exportCustomers(searchForm)
    const blob = response instanceof Blob ? response : new Blob([response], { type: 'text/csv;charset=utf-8' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `客户列表_${dayjs().format('YYYYMMDDHHmmss')}.csv`
    link.click()
    window.URL.revokeObjectURL(url)
  } catch {
    message.error('导出失败')
  } finally {
    exportLoading.value = false
  }
}

const handleBatchExport = () => {
  const records = customers.value.filter((customer) => customer.id && selectedRowKeys.value.includes(customer.id))
  if (!records.length) {
    message.warning('请先选择客户')
    return
  }

  downloadCustomersAsCsv(records, `客户列表_选中_${dayjs().format('YYYYMMDDHHmmss')}.csv`)
  message.success('已导出选中客户')
}

const handleBatchEnable = async () => {
  try {
    await customerStore.batchUpdateStatus(selectedRowKeys.value, 1)
    message.success('批量启用成功')
    clearSelection()
    await refreshCustomerData()
  } catch {
    message.error('批量启用失败')
  }
}

const handleBatchDisable = async () => {
  Modal.confirm({
    title: '确认批量停用',
    content: `确定要停用选中的 ${selectedRowKeys.value.length} 个客户吗？`,
    okText: '确定',
    cancelText: '取消',
    okType: 'danger',
    onOk: async () => {
      try {
        await customerStore.batchUpdateStatus(selectedRowKeys.value, 0)
        message.success('批量停用成功')
        clearSelection()
        await refreshCustomerData()
      } catch {
        message.error('批量停用失败')
      }
    },
  })
}

const handleBatchDelete = () => {
  Modal.confirm({
    title: '确认批量删除',
    content: `确定要删除选中的 ${selectedRowKeys.value.length} 个客户吗？`,
    okText: '确定',
    cancelText: '取消',
    okType: 'danger',
    onOk: async () => {
      try {
        for (const id of selectedRowKeys.value) {
          await customerStore.deleteCustomer(id)
        }
        message.success('批量删除成功')
        clearSelection()
        await refreshCustomerData()
      } catch {
        message.error('批量删除失败')
      }
    },
  })
}

const handleBack = () => {
  router.push('/')
}

const clearSelection = () => {
  selectedRowKeys.value = []
}

const resetImportState = () => {
  selectedImportFile.value = null
  customerImportTemplate.value = null
  customerImportResult.value = null
  importMetaError.value = ''
  importInputKey.value += 1
}

const loadImportTemplateMeta = async () => {
  try {
    importMetaLoading.value = true
    importMetaError.value = ''
    const result = await customerApi.getImportTemplate() as unknown as ImportResult
    customerImportTemplate.value = result
  } catch {
    customerImportTemplate.value = null
    importMetaError.value = '导入模板说明加载失败，请稍后重试'
  } finally {
    importMetaLoading.value = false
  }
}

const showImportModal = async () => {
  importModalVisible.value = true
  resetImportState()
  await loadImportTemplateMeta()
}

const closeImportModal = () => {
  importModalVisible.value = false
  resetImportState()
}

const handleImportFileChange = (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) {
    return
  }

  if (!file.name.toLowerCase().endsWith('.csv')) {
    message.warning('请选择 CSV 文件')
    target.value = ''
    return
  }

  if (file.size > 5 * 1024 * 1024) {
    message.warning('导入文件不能超过 5MB')
    target.value = ''
    return
  }

  selectedImportFile.value = file
  customerImportResult.value = null
}

const submitImport = async () => {
  if (!customerImportMeta.value && !importMetaLoading.value) {
    await loadImportTemplateMeta()
  }

  if (!selectedImportFile.value) {
    message.warning('请先选择待导入的 CSV 文件')
    return
  }

  try {
    importLoading.value = true
    const result = await customerApi.importCustomers(selectedImportFile.value) as unknown as ImportResult
    customerImportResult.value = result
    message.success(`导入完成：成功 ${result.successCount} 条，失败 ${result.failureCount} 条`)
    await refreshCustomerData()
  } catch {
    message.error('导入失败')
  } finally {
    importLoading.value = false
  }
}

const copyImportTemplateHeader = async () => {
  if (!customerImportMeta.value) {
    message.warning('导入模板说明尚未就绪')
    return
  }

  try {
    await navigator.clipboard.writeText(customerImportMeta.value.templateFields.join(','))
    message.success('模板表头已复制')
  } catch {
    message.warning('复制失败，请手动复制模板字段')
  }
}

const handleReferrerSearch = async (keyword: string) => {
  referrerSearchKeyword.value = keyword

  try {
    referrerLoading.value = true
    if (!keyword.trim()) {
      const response = await customerApi.getCustomers({
        page: 0,
        size: 5,
        direction: 'desc',
        sort: 'registeredAt',
      }) as unknown as { content: Customer[] }
      referrerList.value = response.content || []
      return
    }

    const response = await customerApi.getCustomers({
      keyword,
      page: 0,
      size: 5,
    }) as unknown as { content: Customer[] }
    referrerList.value = response.content || []
  } catch {
    referrerList.value = []
  } finally {
    referrerLoading.value = false
  }
}

const handleReferrerFocus = async () => {
  if (referrerList.value.length <= 1 && !referrerSearchKeyword.value) {
    await handleReferrerSearch('')
  }
}

onMounted(async () => {
  await Promise.all([loadCustomers(), loadCustomerStatistics()])

  if (route.query.openCreate === '1') {
    showAddModal()
    const nextQuery = { ...route.query }
    delete nextQuery.openCreate
    await router.replace({ query: nextQuery })
  }
})
</script>

<style scoped>
.customer-list-page {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  gap: 12px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  line-height: 1.2;
  margin: 0;
}

.page-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
}

.add-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
}

.search-card,
.stats-row,
.table-card {
  margin-bottom: 16px;
}

.search-card :deep(.ant-card-body) {
  padding: 16px 20px;
}

.search-form :deep(.ant-form-item) {
  width: 100%;
  margin-bottom: 0;
}

.search-form :deep(.ant-form-item-control) {
  flex: 1;
  min-width: 0;
}

.search-card :deep(.ant-form-item-label > label) {
  color: #6b7280;
  font-size: 13px;
}

.search-card :deep(.ant-input),
.search-card :deep(.ant-picker),
.search-card :deep(.ant-select-selector),
.search-card :deep(.ant-btn),
.page-actions :deep(.ant-btn) {
  min-height: 34px;
  font-size: 13px;
  border-radius: 8px;
}

.table-card :deep(.ant-card-body) {
  padding: 12px 16px 16px;
}

.table-card :deep(.ant-table-thead > tr > th),
.table-card :deep(.ant-table-tbody > tr > td) {
  padding-top: 12px;
  padding-bottom: 12px;
}

.table-card :deep(.ant-table-pagination.ant-pagination) {
  margin-bottom: 0;
}

.table-card :deep(.ant-table) {
  min-width: 1080px;
}

.stats-card {
  position: relative;
  min-height: 84px;
  overflow: hidden;
  border: 1px solid #dbeafe;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
  box-shadow: 0 10px 24px -22px rgba(37, 99, 235, 0.55);
}

.stats-card::before {
  content: '';
  position: absolute;
  inset: 0 0 auto 0;
  height: 3px;
  background: linear-gradient(90deg, #60a5fa 0%, #818cf8 100%);
}

.stats-card::after {
  content: '';
  position: absolute;
  top: -28px;
  right: -18px;
  width: 86px;
  height: 86px;
  border-radius: 999px;
  background: radial-gradient(circle, rgba(96, 165, 250, 0.18) 0%, rgba(96, 165, 250, 0) 72%);
  pointer-events: none;
}

.stats-row :deep(.ant-col:nth-child(2)) .stats-card {
  border-color: #dcfce7;
  background: linear-gradient(180deg, #ffffff 0%, #f7fff9 100%);
  box-shadow: 0 10px 24px -22px rgba(34, 197, 94, 0.5);
}

.stats-row :deep(.ant-col:nth-child(2)) .stats-card::before {
  background: linear-gradient(90deg, #4ade80 0%, #22c55e 100%);
}

.stats-row :deep(.ant-col:nth-child(2)) .stats-card::after {
  background: radial-gradient(circle, rgba(74, 222, 128, 0.18) 0%, rgba(74, 222, 128, 0) 72%);
}

.stats-row :deep(.ant-col:nth-child(3)) .stats-card {
  border-color: #fde68a;
  background: linear-gradient(180deg, #ffffff 0%, #fffaf2 100%);
  box-shadow: 0 10px 24px -22px rgba(245, 158, 11, 0.45);
}

.stats-row :deep(.ant-col:nth-child(3)) .stats-card::before {
  background: linear-gradient(90deg, #fbbf24 0%, #f59e0b 100%);
}

.stats-row :deep(.ant-col:nth-child(3)) .stats-card::after {
  background: radial-gradient(circle, rgba(251, 191, 36, 0.18) 0%, rgba(251, 191, 36, 0) 72%);
}

.stats-row :deep(.ant-col:nth-child(4)) .stats-card {
  border-color: #e9d5ff;
  background: linear-gradient(180deg, #ffffff 0%, #fbf7ff 100%);
  box-shadow: 0 10px 24px -22px rgba(168, 85, 247, 0.45);
}

.stats-row :deep(.ant-col:nth-child(4)) .stats-card::before {
  background: linear-gradient(90deg, #c084fc 0%, #8b5cf6 100%);
}

.stats-row :deep(.ant-col:nth-child(4)) .stats-card::after {
  background: radial-gradient(circle, rgba(192, 132, 252, 0.2) 0%, rgba(192, 132, 252, 0) 72%);
}

.stats-card :deep(.ant-card-body) {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  padding: 12px 16px 13px;
}

.stats-primary-line {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
}

.stats-title {
  color: #64748b;
  font-size: 12px;
  font-weight: 500;
  line-height: 1.3;
  white-space: nowrap;
}

.stats-value-inline {
  font-size: 18px;
  line-height: 1;
  font-weight: 700;
  color: #111827;
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
}

.stats-secondary-line {
  margin-top: 6px;
  color: #475569;
  font-size: 12px;
  line-height: 1.35;
}

.customer-name {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.name-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.name {
  font-weight: 500;
  color: #333;
}

.customer-tags {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.contact-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.email {
  font-size: 12px;
  color: #666;
}

.referrer-option {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 4px 0;
}

.referrer-name {
  font-weight: 500;
  color: #333;
  min-width: 80px;
}

.referrer-phone {
  color: #666;
  font-size: 13px;
}

.batch-actions {
  margin-top: 16px;
  padding: 12px 16px;
  background: #fafafa;
  border-radius: 6px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.selected-count {
  color: #666;
  font-size: 14px;
}

.customer-detail {
  padding: 8px;
}

.detail-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.header-info h2 {
  margin: 0 0 8px 0;
  color: #333;
}

.detail-info {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}

.info-item:last-child {
  border-bottom: none;
}

.label {
  color: #666;
  min-width: 80px;
}

.value {
  color: #333;
  text-align: right;
  flex: 1;
  word-break: break-all;
}

.detail-actions {
  margin-top: 16px;
}

.import-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.import-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.import-title {
  font-size: 16px;
  font-weight: 600;
}

.import-subtitle {
  color: #666;
  margin-top: 4px;
}

.import-picker {
  position: relative;
  overflow: hidden;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 32px;
  padding: 0 15px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  background: #fff;
  cursor: pointer;
}

.import-picker input {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
}

.import-alert,
.import-meta-card,
.import-result-card {
  width: 100%;
}

.meta-row {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 12px;
}

.meta-row:last-child {
  margin-bottom: 0;
}

.meta-row.single-line {
  align-items: center;
}

.meta-label {
  min-width: 72px;
  color: #666;
}

.meta-list {
  margin: 0;
  padding-left: 16px;
  color: #666;
}

.result-summary {
  display: flex;
  gap: 32px;
  margin-bottom: 16px;
}

@media (max-width: 768px) {
  .page-header,
  .batch-actions,
  .import-header {
    flex-direction: column;
    align-items: stretch;
  }

  .page-actions {
    width: 100%;
  }
}
</style>
