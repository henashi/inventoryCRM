<template>
  <div class="gift-log-page">
    <div class="page-header">
      <div>
        <h1 class="page-title">礼品发放</h1>
        <p class="page-subtitle">支持查看发放详情；删除仅对管理员开放。</p>
      </div>
      <div class="page-actions">
        <a-button @click="handleRefresh" :loading="isLoading">
          <reload-outlined />
          刷新
        </a-button>
        <a-button @click="handleAdd" type="primary" style="margin-left: 8px">
          <gift-outlined />
          发放
        </a-button>
        <a-button @click="handleBack" style="margin-left: 8px">
          <left-outlined />
          返回
        </a-button>
      </div>
    </div>

    <a-card class="filter-card">
      <a-form layout="inline">
        <a-form-item label="客户筛选">
          <a-select
            v-model:value="giftLogFilter.customerId"
            placeholder="全部客户"
            allow-clear
            show-search
            style="width: 280px"
            :filter-option="filterCustomerOption"
            @change="handleCustomerFilterChange"
          >
            <a-select-option v-for="customer in customerOptions" :key="customer.id" :value="customer.id">
              {{ customer.name }} {{ customer.phone }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" @click="handleApplyFilters">应用筛选</a-button>
            <a-button @click="handleResetFilters">清空筛选</a-button>
            <a-tag v-if="routeCustomerContext.hasCustomerContext" color="blue">
              来自客户上下文
            </a-tag>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>

    <a-alert
      v-if="activeCustomerFilterLabel"
      class="page-alert"
      type="info"
      show-icon
      :message="`当前仅展示${activeCustomerFilterLabel}的礼品记录`"
      description="筛选已收口到页面状态，后续可在此继续扩展更多显式筛选条件。"
    />

    <a-alert
      v-if="!canDeleteCurrentGiftLog"
      class="page-alert"
      type="info"
      show-icon
      message="删除权限已收口"
      description="当前账号可以查看礼品日志详情并继续发放待处理记录，删除入口仅对管理员显示。"
    />

    <a-card class="table-card">
      <a-table
        :columns="columns"
        ref="formRef"
        :data-source="dataSource"
        :pagination="pagination"
        :loading="isLoading"
        :rules="rules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
        @change="handleTableChange"
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'createdTime'">
            {{ formatDateTime(record.createdTime) }}
          </template>
          <template v-else-if="column.dataIndex === 'issueTime'">
            {{ formatDateTime(record.issueTime) }}
          </template>
          <template v-else-if="column.dataIndex === 'status'">
            <a-tag :color="record.status === 'PENDING' ? 'orange' : record.status === 'CANCELLED' ? 'red' : 'green'">
              {{ getStatusText(record.status) }}
            </a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'issueNotes'">
            <a-tooltip :title="record.issueNotes">
              <span>{{ record.issueNotes || '--' }}</span>
            </a-tooltip>
          </template>
        </template>

        <template #action="{ record }">
          <a-space :size="4">
            <a-button type="link" size="small" @click="handleGiftLogDetail(record)">
              详情
            </a-button>
            <a-button
              v-if="record.status === 'PENDING'"
              type="link"
              size="small"
              @click="handleIssuePendingLog(record)"
            >
              发放
            </a-button>
            <a-button
              v-if="canDeleteCurrentGiftLog"
              type="link"
              size="small"
              danger
              @click="handleDeleteGiftLog(record)"
            >
              删除
            </a-button>
          </a-space>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:visible="modalVisible"
      title="发放礼品"
      @ok="handleModalOk"
      @cancel="handleModalCancel"
    >
      <a-form
        ref="formRef"
        :model="formState"
        :rules="rules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="关联礼品" name="giftId">
          <a-select
            v-model:value="formState.giftId"
            placeholder="请选择礼品"
            allow-clear
            show-search
            :filter-option="filterGiftOption"
            :disabled="modelType === 'edit'"
            @change="handleGiftChange"
          >
            <a-select-option v-for="gift in giftOptions" :key="gift.id" :value="gift.id">
              {{ gift.name }} {{ gift.code }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="领取人" name="customerId">
          <a-select
            v-model:value="formState.customerId"
            placeholder="请选择领取人"
            allow-clear
            show-search
            :filter-option="filterCustomerOption"
            :disabled="modelType === 'edit' || !!activeCustomerId"
          >
            <a-select-option v-for="customer in customerOptions" :key="customer.id" :value="customer.id">
              {{ customer.name }} {{ customer.phone }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="发放数量" name="quantity">
          <a-input-number v-model:value="formState.quantity" style="width: 100%" :disabled="formState.limitEnabled" />
        </a-form-item>
        <a-form-item label="发放说明" name="issueNotes">
          <a-textarea v-model:value="formState.issueNotes" placeholder="请输入发放说明" :maxlength="50" show-count />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-drawer v-model:open="detailVisible" title="礼品日志详情" width="560" :destroy-on-close="true">
      <a-spin :spinning="detailLoading">
        <a-empty v-if="!detailGiftLog" description="暂无日志详情" />
        <a-descriptions v-else :column="1" bordered size="small">
          <a-descriptions-item label="礼品名称">{{ detailGiftLog.giftName }}</a-descriptions-item>
          <a-descriptions-item label="领取人">{{ detailGiftLog.customerName }}</a-descriptions-item>
          <a-descriptions-item label="发放状态">{{ getStatusText(detailGiftLog.status) }}</a-descriptions-item>
          <a-descriptions-item label="发放数量">{{ detailGiftLog.quantity }}</a-descriptions-item>
          <a-descriptions-item label="处理说明">{{ detailGiftLog.issueNotes || '--' }}</a-descriptions-item>
          <a-descriptions-item label="操作人">{{ detailGiftLog.operator || '--' }}</a-descriptions-item>
          <a-descriptions-item label="备注">{{ detailGiftLog.remark || '--' }}</a-descriptions-item>
          <a-descriptions-item label="发放时间">{{ formatDateTime(detailGiftLog.issuedAt) }}</a-descriptions-item>
          <a-descriptions-item label="创建时间">{{ formatDateTime(detailGiftLog.createdTime) }}</a-descriptions-item>
          <a-descriptions-item label="更新时间">{{ formatDateTime(detailGiftLog.updatedTime) }}</a-descriptions-item>
        </a-descriptions>
      </a-spin>
    </a-drawer>
  </div>
</template>

<script lang="ts" setup>
import dayjs from 'dayjs'
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Modal, message } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import { GiftOutlined, LeftOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { customerApi } from '@/api/customer'
import { useGiftLogStore } from '@/stores/giftLog'
import { useGiftStore } from '@/stores/gift'
import { canDeleteGiftLog } from '@/router/accessControl'
import type { Customer, GiftLogDTO, PageParams } from '@/types'
import { buildServerPageParams, toServerPage } from '@/utils/pagination'
import { resolveGiftLogFilterState } from '@/utils/featureEnhancements'

type GiftLogFormState = GiftLogDTO & {
  limitEnabled: boolean
}

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const giftStore = useGiftStore()
const giftLogStore = useGiftLogStore()
const isLoading = ref(false)
const customers = ref<Customer[]>([])
const modalVisible = ref(false)
const formRef = ref<FormInstance>()
const modelType = ref<'add' | 'edit'>('add')
const currentGiftLog = ref<GiftLogDTO | null>(null)
const dataSource = computed(() => giftLogStore.giftLogList)
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailGiftLog = ref<GiftLogDTO | null>(null)
const routeCustomerContext = resolveGiftLogFilterState(route.query as Record<string, unknown>)

const giftLogFilter = reactive({
  customerId: routeCustomerContext.customerId,
  customerName: routeCustomerContext.customerName,
})

const pagination = computed(() => ({
  current: giftLogStore.pagination.page,
  pageSize: giftLogStore.pagination.size,
  total: giftLogStore.pagination.total,
  showTotal: (total: number) => `共 ${total} 条`,
  showSizeChanger: true,
  showQuickJumper: true,
}))

const columns = [
  { title: '礼品名称', dataIndex: 'giftName', key: 'giftName' },
  { title: '领取人', dataIndex: 'customerName', key: 'customerName' },
  { title: '发放时间', dataIndex: 'createdTime', key: 'createdTime' },
  { title: '发放数量', dataIndex: 'quantity', key: 'quantity' },
  { title: '处理说明', dataIndex: 'issueNotes', key: 'issueNotes' },
  { title: '发放状态', dataIndex: 'status', key: 'status' },
  {
    title: '操作',
    key: 'action',
    fixed: 'right',
    width: 240,
    slots: { customRender: 'action' },
  },
]

const formState = reactive<GiftLogFormState>({
  id: null,
  giftId: null,
  giftName: '',
  customerId: null,
  customerName: '',
  quantity: 1,
  issueNotes: '',
  remark: '',
  issuedAt: '',
  operator: '',
  status: 'PENDING',
  createdTime: '',
  updatedTime: '',
  limitEnabled: false,
})

const rules = {
  giftId: [{ required: true, message: '请选择关联礼品', trigger: 'change' }],
  customerId: [{ required: true, message: '请选择领取人', trigger: 'change' }],
  quantity: [{ required: true, message: '请输入发放数量', trigger: 'change' }],
  issueNotes: [{ max: 50, message: '处理说明不能超过50字', trigger: 'blur' }],
}

const activeCustomerId = computed(() => giftLogFilter.customerId)
const activeCustomerFilterLabel = computed(() => giftLogFilter.customerName ? `客户“${giftLogFilter.customerName}”` : '')
const canDeleteCurrentGiftLog = computed(() => canDeleteGiftLog(authStore.userRole))

const getStatusText = (status: GiftLogDTO['status']) => {
  switch (status) {
    case 'PENDING':
      return '待发放'
    case 'CANCELLED':
      return '已取消'
    default:
      return '已发放'
  }
}

const resetFormState = () => {
  Object.assign(formState, {
    id: null,
    giftId: null,
    giftName: '',
    customerId: null,
    customerName: '',
    quantity: 1,
    issueNotes: '',
    remark: '',
    issuedAt: '',
    operator: '',
    status: 'PENDING',
    createdTime: '',
    updatedTime: '',
    limitEnabled: false,
  })
}

const handleRefresh = async () => {
  await loadGiftLogs({
    page: toServerPage(giftLogStore.pagination.page),
    size: giftLogStore.pagination.size,
  })
  message.success('刷新成功')
}

const applyCustomerPreset = () => {
  if (!activeCustomerId.value) {
    return
  }

  Object.assign(formState, {
    customerId: activeCustomerId.value,
    customerName: giftLogFilter.customerName,
  })
}

const handleAdd = () => {
  modelType.value = 'add'
  resetFormState()
  applyCustomerPreset()
  modalVisible.value = true
}

const handleIssuePendingLog = (record: GiftLogDTO) => {
  currentGiftLog.value = record
  modelType.value = 'edit'
  modalVisible.value = true
  const result = findGiftLimitData(record.giftId!)
  Object.assign(formState, {
    giftId: currentGiftLog.value?.giftId || null,
    customerId: currentGiftLog.value?.customerId || null,
    quantity: currentGiftLog.value?.quantity || 1,
    issueNotes: currentGiftLog.value?.issueNotes || '',
    remark: currentGiftLog.value?.remark || '',
    status: 'ISSUED',
    limitEnabled: result.limitEnabled,
  })
}

const handleGiftLogDetail = async (record: GiftLogDTO) => {
  detailVisible.value = true
  detailLoading.value = true
  detailGiftLog.value = null

  try {
    detailGiftLog.value = await giftLogStore.getGiftLogDetail(record.id!)
  } catch {
    message.error('加载礼品日志详情失败')
  } finally {
    detailLoading.value = false
  }
}

const handleDeleteGiftLog = (record: GiftLogDTO) => {
  Modal.confirm({
    title: '删除礼品日志',
    content: `确定要删除礼品日志“${record.giftName} / ${record.customerName}”吗？删除仅影响管理视角展示，请以后端校验语义为准。`,
    okText: '确认删除',
    okButtonProps: { danger: true },
    cancelText: '取消',
    onOk: async () => {
      try {
        await giftLogStore.deleteGiftLog(record.id!)
        await loadGiftLogs({
          page: toServerPage(giftLogStore.pagination.page),
          size: giftLogStore.pagination.size,
        })
        message.success('礼品日志已删除')
        if (detailGiftLog.value?.id === record.id) {
          detailVisible.value = false
          detailGiftLog.value = null
        }
      } catch (error: any) {
        message.error(error?.response?.data?.message || '删除礼品日志失败')
      }
    },
  })
}

const handleBack = () => {
  if (routeCustomerContext.source === 'customer-detail' && routeCustomerContext.customerId) {
    router.push(`/customers/${routeCustomerContext.customerId}`)
    return
  }

  if (routeCustomerContext.source === 'dashboard') {
    router.push('/dashboard')
    return
  }

  if (routeCustomerContext.source === 'customers') {
    router.push('/customers')
    return
  }

  router.push('/')
}

const handleGiftChange = (value: number | null) => {
  if (value == null) {
    formState.limitEnabled = false
    formState.quantity = 1
    return
  }

  const result = findGiftLimitData(value)
  formState.limitEnabled = result.limitEnabled
  formState.quantity = result.limitPerPerson
}

const handleModalOk = async () => {
  try {
    if (!formRef.value) {
      message.error('表单未加载')
      return
    }

    await formRef.value.validate()
    if (modelType.value === 'add') {
      await giftLogStore.createGiftLog(formState)
      message.success('礼品发放成功')
    } else if (currentGiftLog.value?.id) {
      await giftLogStore.updateGiftLog(currentGiftLog.value.id, formState)
      message.success('礼品发放成功')
    }
  } catch {
    message.error('请检查表单输入')
    return
  }

  modalVisible.value = false
  formRef.value?.resetFields()
  resetFormState()
  await loadGiftLogs({
    page: toServerPage(giftLogStore.pagination.page),
    size: giftLogStore.pagination.size,
  })
}

const handleModalCancel = () => {
  modalVisible.value = false
  formRef.value?.resetFields()
  resetFormState()
  applyCustomerPreset()
}

const filterGiftOption = (input: string, option: any) => {
  const label = typeof option?.children === 'string' ? option.children : ''
  return label.toLowerCase().includes(input.toLowerCase())
}

const findGiftLimitData = (id: number) => {
  const response = giftStore.gifts.find(gift => gift.id === id)
  return {
    limitEnabled: response?.limitEnabled ?? false,
    limitPerPerson: response?.limitPerPerson ?? 1,
  }
}

const giftOptions = computed(() => {
  return giftStore.gifts.map(gift => ({
    id: gift.id,
    name: gift.name,
    code: gift.code,
  }))
})

const filterCustomerOption = (input: string, option: any) => {
  const label = typeof option?.children === 'string' ? option.children : ''
  return label.toLowerCase().includes(input.toLowerCase())
}

const formatDateTime = (dateStr?: string) => {
  if (!dateStr) return '--'
  return dayjs(dateStr).format('YYYY-MM-DD HH:mm:ss')
}

const customerOptions = computed(() => {
  return customers.value.map(customer => ({
    id: customer.id,
    name: customer.name,
    phone: customer.phone,
  }))
})

const syncFilterCustomerName = () => {
  const matchedCustomer = customers.value.find((customer) => customer.id === giftLogFilter.customerId)
  giftLogFilter.customerName = matchedCustomer?.name || ''
}

const handleCustomerFilterChange = () => {
  syncFilterCustomerName()
}

const handleApplyFilters = async () => {
  await loadGiftLogs({ page: 0, size: giftLogStore.pagination.size })
}

const handleResetFilters = async () => {
  giftLogFilter.customerId = undefined
  giftLogFilter.customerName = ''
  await loadGiftLogs({ page: 0, size: giftLogStore.pagination.size })
}

const loadGiftLogs = async (params?: PageParams) => {
  try {
    isLoading.value = true
    await giftLogStore.loadGiftLogs({
      page: params?.page ?? 0,
      size: params?.size ?? 10,
      customerId: activeCustomerId.value,
    })
  } catch {
    message.error('加载礼品日志失败')
  } finally {
    isLoading.value = false
  }
}

const loadCustomersForOptions = async () => {
  try {
    const response = await customerApi.getCustomers({ page: 0, size: 100 }) as unknown as { content: Customer[] }
    customers.value = response.content || []
  } catch {
    customers.value = []
  }
}

const handleTableChange = (tablePagination: { current?: number; pageSize?: number }) => {
  void loadGiftLogs(buildServerPageParams(tablePagination, giftLogStore.pagination.size))
}

onMounted(async () => {
  await Promise.allSettled([
    loadGiftLogs(),
    giftStore.loadGifts({ page: 0, size: 100 }),
    loadCustomersForOptions(),
  ])

  syncFilterCustomerName()
  applyCustomerPreset()

  if (route.query.openCreate === '1') {
    handleAdd()
    const nextQuery = { ...route.query }
    delete nextQuery.openCreate
    await router.replace({ query: nextQuery })
  }
})
</script>

<style scoped>
.gift-log-page {
  padding: 20px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.page-title {
  margin: 0;
}

.page-subtitle {
  margin: 8px 0 0;
  color: #8c8c8c;
}

.filter-card,
.page-alert {
  margin-bottom: 16px;
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
