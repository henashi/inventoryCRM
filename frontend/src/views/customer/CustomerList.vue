<!-- frontend/src/views/customer/CustomerList.vue -->
<template>
  <div class="customer-list-page">
    <!-- 页面标题和操作 -->
    <div class="page-header">
      <h1 class="page-title">客户管理</h1>
      <div class="page-actions">
        <a-button type="primary" @click="showAddModal" class="add-btn">
          <template #icon>
            <plus-outlined />
          </template>
          新增客户
        </a-button>
        <!-- <a-button @click="handleExport" :loading="exportLoading">
          <template #icon>
            <export-outlined />
          </template>
          导出
        </a-button> -->
        <!-- <a-button @click="showImportModal">
          <template #icon>
            <import-outlined />
          </template>
          导入
        </a-button> -->
        <a-tooltip title="刷新">
          <a-button @click="handleRefresh" :loading="isLoading">
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

    <!-- 搜索和筛选区域 -->
    <a-card class="search-card">
      <a-form layout="inline" :model="searchForm" @finish="handleSearch">
        <a-row :gutter="[16, 16]" style="width: 100%">
          <!-- 搜索框 -->
          <a-col :xs="24" :sm="12" :md="8" :lg="6">
            <a-form-item label="关键词">
              <a-input
                v-model:value="searchForm.keyword"
                placeholder="姓名/手机号/邮箱"
                allow-clear
              />
            </a-form-item>
          </a-col>

          <!-- 礼品等级筛选 -->
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

          <!-- 状态筛选 -->
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

          <!-- 时间筛选 -->
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

        <!-- 第二行：按钮行，靠右显示 -->
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

    <!-- 客户表格 -->
    <a-card class="table-card">
      <a-table
        :columns="columns"
        :data-source="customers"
        :loading="isLoading"
        :pagination="pagination"
        :row-selection="rowSelection"
        :row-key="record => record.id!"
        @change="handleTableChange"
        bordered
      >
        <!-- 姓名列 -->
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'name'">
            <div class="customer-name">
              <a-avatar :size="32" :src="record.avatar || getAvatarColor(record.name)">
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

          <!-- 联系方式 -->
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

          <!-- 状态 -->
          <template v-else-if="column.dataIndex === 'status'">
            <a-tag :color="record.status === 1 ? 'green' : 'red'">
              {{ record.status === 1 ? '正常' : '停用' }}
            </a-tag>
          </template>

          <!-- 礼品等级 -->
          <template v-else-if="column.dataIndex === 'giftLevel'">
            <a-tag :color="giftLevelColors[record.giftLevel || 0]">
              {{ getGiftLevelText(record.giftLevel || 0) }}
            </a-tag>
          </template>

          <!-- 创建时间 -->
          <template v-else-if="column.dataIndex === 'registeredAt'">
            <div v-if="record.registeredAt">
              {{ formatDate(record.registeredAt) }}
            </div>
            <span v-else>-</span>
          </template>

          <!-- 操作列 -->
          <template v-else-if="column.dataIndex === 'actions'">
            <a-space size="small">
              <a-button
                type="link"
                size="small"
                @click="handleView(record)"
              >
                查看
              </a-button>
              <a-button
                type="link"
                size="small"
                @click="handleEdit(record)"
              >
                编辑
              </a-button>
              <a-button
                type="link"
                v-if="record.status === 1"
                @click="handleDisable(record)"
                danger
              >
                <stop-outlined />
                停用
              </a-button>
              <a-button
                type="link"
                v-else
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
              <!-- <a-dropdown>
                <template #overlay>
                  <a-menu>
                    <a-menu-item @click="handleCall(record)">
                      <phone-outlined />
                      拨打电话
                    </a-menu-item>
                    <a-menu-item @click="handleSendEmail(record)">
                      <mail-outlined />
                      发送邮件
                    </a-menu-item>
                    <a-menu-divider />

                  </a-menu>
                </template>
                <a-button type="link" size="small">
                  更多
                  <down-outlined />
                </a-button>
              </a-dropdown> -->
            </a-space>
          </template>
        </template>
      </a-table>

      <!-- 批量操作 -->
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

    <!-- 新增/编辑客户模态框 -->
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
          <a-input
            v-model:value="formState.name"
            placeholder="请输入客户姓名"
          />
        </a-form-item>

        <a-form-item label="手机号" name="phone">
          <a-input
            v-model:value="formState.phone"
            placeholder="请输入手机号"
          />
        </a-form-item>

        <a-form-item label="邮箱" name="email">
          <a-input
            v-model:value="formState.email"
            placeholder="请输入邮箱"
          />
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

    <!-- 查看客户详情抽屉 -->
    <a-drawer
      v-model:open="drawerVisible"
      title="客户详情"
      width="400"
      placement="right"
    >
      <template v-if="currentCustomer">
        <div class="customer-detail">
          <div class="detail-header">
            <a-avatar :size="64" :src="currentCustomer.avatar || getAvatarColor(currentCustomer.name)">
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
            <!-- <a-button @click="handleCall(currentCustomer)" block style="margin-top: 8px">
              <phone-outlined />
              拨打电话
            </a-button> -->
          </div>
        </div>
      </template>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  message,
  Modal,
  type FormInstance,
  type TableProps,
  type TableColumnType
} from 'ant-design-vue'
import {
  PlusOutlined,
  ExportOutlined,
  ImportOutlined,
  ReloadOutlined,
  PhoneOutlined,
  MailOutlined,
  StopOutlined,
  CheckOutlined,
  DownOutlined
  ,
  HomeOutlined
} from '@ant-design/icons-vue'
import { useCustomerStore } from '@/stores/customer'
import { customerApi } from '@/api/customer'
import type { Customer, CustomerCreateDTO, PageParams } from '@/types'
import dayjs from 'dayjs'

const router = useRouter()
const customerStore = useCustomerStore()
const formRef = ref<FormInstance>()
const referrerList = ref<Customer[]>([])
const referrerLoading = ref(false)
const referrerSearchKeyword = ref('')

// 状态
const isLoading = ref(false)
const exportLoading = ref(false)
const modalVisible = ref(false)
const modalLoading = ref(false)
const drawerVisible = ref(false)
const modalType = ref<'add' | 'edit'>('add')
const currentCustomer = ref<Customer | null>(null)
const selectedRowKeys = ref<number[]>([])

// 表格列定义
const columns: TableColumnType[] = [
  {
    title: '客户信息',
    dataIndex: 'name',
    key: 'name',
    width: 200
  },
  {
    title: '联系方式',
    dataIndex: 'phone',
    key: 'phone',
    width: 150
  },
  {
    title: '性别',
    dataIndex: 'gender',
    key: 'gender',
    width: 80,
    filters: [
      { text: '男', value: 1 },
      { text: '女', value: 0 }
    ]
  },
  {
    title: '生日',
    dataIndex: 'birthday',
    key: 'birthday',
    width: 120
  },
  {
    title: '礼品等级',
    dataIndex: 'giftLevel',
    key: 'giftLevel',
    width: 100
  },
  {
    title: '状态',
    dataIndex: 'status',
    key: 'status',
    width: 100
  },
  {
    title: '注册日期',
    dataIndex: 'registeredAt',
    key: 'registeredAt',
    width: 150,
    sorter: true
  },
  {
    title: '操作',
    dataIndex: 'actions',
    key: 'actions',
    width: 200,
    fixed: 'right' as const
  }
]

// 搜索表单
const searchForm = reactive({
  keyword: '',
  giftLevel: undefined as number | undefined,
  status: undefined as 0 | 1 | undefined,
  dateRange: [] as any[]
})

// 新增/编辑表单
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
  referrerId: undefined
})

// 表单验证规则
const rules = {
  name: [
    { required: true, message: '请输入客户姓名', trigger: 'blur' },
    { min: 2, max: 20, message: '姓名长度2-20个字符', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  birthday: [
    { required: false, message: '请选择生日', trigger: 'change' }
  ]
}

// 计算属性
const customers = computed(() => customerStore.customers)
const pagination = computed(() => ({
  current: customerStore.pagination.page,
  pageSize: customerStore.pagination.size,
  total: customerStore.pagination.total,
  pageSizeOptions: ['5', '10', '20'], // 可选的每页条数
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条`
}))

const rowSelection = computed<TableProps['rowSelection']>(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (selectedKeys: any[]) => {
    selectedRowKeys.value = selectedKeys
  },
  getCheckboxProps: (record: Customer) => ({
    disabled: record.status === 0
  })
}))

const modalTitle = computed(() => modalType.value === 'add' ? '新增客户' : '编辑客户')
const hasDisabledSelected = computed(() =>
  selectedRowKeys.value.some(key => {
    const customer = customers.value.find(c => c.id === key)
    return customer?.status === 0
  })
)

// 礼品等级颜色映射
const giftLevelColors: Record<number, string> = {
  0: 'default',
  1: 'blue',
  2: 'green',
  3: 'orange'
}

// 方法
const getFirstChar = (name: string) => name.charAt(0).toUpperCase()
const getAvatarColor = (name: string) => {
  const colors = ['#1890ff', '#52c41a', '#faad14', '#f5222d', '#722ed1', '#13c2c2']
  const index = name.charCodeAt(0) % colors.length
  return colors[index]
}

const getGiftLevelText = (level: number) => {
  const texts = ['普通客户', '等级1', '等级2', '等级3']
  return texts[level] || '普通客户'
}

const formatDate = (dateStr?: string, format?: string) => {
  if (!dateStr) return '-'
  // 如果没有传入format参数，使用默认格式
  const formatStr = format || 'YYYY-MM-DD'
  return dayjs(dateStr).format(formatStr)
}

// 加载数据
const loadCustomers = async (params?: PageParams) => {
  try {
    const filteredParams = Object.fromEntries(
      Object.entries(params || {}).filter(([_, v]) => v != null && v !== '')
    )
    if (filteredParams.sort == null) {
      filteredParams.sort = 'registeredAt'
      filteredParams.direction = 'desc'
    }
    await customerStore.loadCustomers(filteredParams)
  } catch (error) {
    message.error('加载客户列表失败')
  }
}

// 搜索
const handleSearch = () => {
  console.log(searchForm)
  selectedRowKeys.value = []
  loadCustomers({ page: 0, ...searchForm })
}

// 重置搜索
const handleReset = () => {
  searchForm.keyword = ''
  searchForm.giftLevel = undefined
  searchForm.status = undefined
  searchForm.dateRange = []
  handleSearch()
}

// 表格变化
const handleTableChange: TableProps['onChange'] = (pagination, filters, sorter) => {
  console.log(pagination, filters, sorter)
  const params: any = {
    page: pagination.current! - 1,
    size: pagination.pageSize
  }

  if (filters.gender && filters.gender.length > 0 && filters.gender.length < 2) {
    params.gender = filters.gender[0]
  }

  if (filters.status) {
    params.status = filters.status[0]
  }

  if (sorter && sorter.field) {
    params.sort = sorter.field
    params.direction = sorter.order === 'ascend' ? 'asc' : 'desc'
  }
  console.log("Table changed with params:", params)
  loadCustomers(params)
}

// 刷新
const handleRefresh = () => {
  loadCustomers()
  message.success('刷新成功')
}

// 查看详情
const handleView = (record: Customer) => {
  currentCustomer.value = record
  drawerVisible.value = true
}

// 编辑
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
    remark: record.remark || ''
  })
  // 如果当前推荐人ID不在列表中，尝试查询并添加
  if (formState.referrerId && !referrerList.value.find(r => r.id === formState.referrerId)) {
    console.log('Current referrer ID not in list, fetching...');
    const referrer = await customerStore.findCustomerById(formState.referrerId);
    if (referrer) {
      referrerList.value = [referrer, ...referrerList.value];
    }
  }
  modalVisible.value = true
}

// 删除
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
        loadCustomers()
      } catch (error) {
        message.error('删除失败')
      }
    }
  })
}

// 拨打电话
const handleCall = (record: Customer) => {
  window.location.href = `tel:${record.phone}`
}

// 发送邮件
const handleSendEmail = (record: Customer) => {
  if (record.email) {
    window.location.href = `mailto:${record.email}`
  } else {
    message.warning('该客户没有邮箱')
  }
}

// 启用/停用
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
        loadCustomers()
      } catch (error) {
        message.error('停用失败')
      }
    }
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
        loadCustomers()
      } catch (error) {
        message.error('启用失败')
      }
    }
  })
}

// 显示新增模态框
const showAddModal = () => {
  modalType.value = 'add'
  formRef.value?.resetFields()
  Object.assign(formState, {
    name: '',
    phone: '',
    email: '',
    gender: 1,
    referrerId: null,
    birthday: undefined as string | undefined,
    address: '',
    giftLevel: 0,
    status: 1,
    remark: ''
  })
  modalVisible.value = true
}

// 模态框确定
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
    loadCustomers()
    modalVisible.value = false
  } catch (error) {
    console.error('保存失败:', error)
  } finally {
    modalLoading.value = false
  }
}

// 模态框取消
const handleModalCancel = () => {
  modalVisible.value = false
  formRef.value?.resetFields()
}

// 导出
const handleExport = async () => {
  try {
    exportLoading.value = true
    const response = await customerStore.exportCustomers(searchForm)
    const blob = new Blob([response], { type: 'application/vnd.ms-excel' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `客户列表_${dayjs().format('YYYYMMDDHHmmss')}.xlsx`
    link.click()
    window.URL.revokeObjectURL(url)
  } catch (error) {
    message.error('导出失败')
  } finally {
    exportLoading.value = false
  }
}

// 批量操作
const handleBatchExport = () => {
  message.info('批量导出功能开发中')
}

const handleBatchEnable = async () => {
  try {
    await customerStore.batchUpdateStatus(selectedRowKeys.value, 1)
    message.success('批量启用成功')
    clearSelection()
    loadCustomers()
  } catch (error) {
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
        loadCustomers()
      } catch (error) {
        message.error('批量停用失败')
      }
    }
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
        loadCustomers()
      } catch (error) {
        message.error('批量删除失败')
      }
    }
  })
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

const handleBack = () => {
  router.push('/')
}

const clearSelection = () => {
  selectedRowKeys.value = []
}

// 导入模态框
const showImportModal = () => {
  message.info('导入功能开发中')
}

// 初始化
onMounted(() => {
  loadCustomers()
})

// 搜索推荐人
const handleReferrerSearch = async (keyword: string) => {
  console.log('Searching referrers with keyword:', keyword)
  referrerSearchKeyword.value = keyword

  try {
    referrerLoading.value = true
    // 调用搜索接口，这里假设你的后端支持按关键词搜索客户
    if (!keyword.trim()) {
      // 空关键词时显示最近使用的推荐人或热门推荐
      const response = await customerApi.getCustomers({
        page: 0,
        size: 5,
        direction: 'desc',
        sort: 'registeredAt'  // 按创建时间倒序，显示最新客户
      })
      referrerList.value = response.content || []
    } else {
      // 有关键词时正常搜索
      const response = await customerApi.getCustomers({
        keyword: keyword,
        page: 0,
        size: 5
      })
      referrerList.value = response.content || []
    }
  } catch (error) {
    console.error('搜索推荐人失败:', error)
    referrerList.value = []
  } finally {
    referrerLoading.value = false
  }
}

// 获取焦点时加载默认列表
const handleReferrerFocus = async () => {
  if (referrerList.value.length <= 1 && !referrerSearchKeyword.value) {
    console.log('Loading default referrer list on focus')
    await handleReferrerSearch('')
  }
}
</script>

<style scoped>
.customer-list-page {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.page-actions {
  display: flex;
  gap: 8px;
}

.add-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
}

.search-card {
  margin-bottom: 16px;
  border-radius: 8px;
}

.table-card {
  border-radius: 8px;
}

.customer-name {
  display: flex;
  align-items: center;
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

/* 响应式设计 */
@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .page-actions {
    width: 100%;
    flex-wrap: wrap;
  }

  .batch-actions {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
}
</style>
