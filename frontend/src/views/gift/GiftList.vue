<template>
  <div class="gift-page">
    <div class="page-header">
      <div>

        <p class="page-subtitle">支持查看礼品详情；删除仅对管理员开放。</p>
      </div>
      <div class="page-actions">

        <a-button v-if="canManageCatalog" type="primary" @click="showAddModal" style="margin-left: 8px">
          <plus-outlined />
          新增礼品
        </a-button>
        <a-button @click="handleViewDistributionLogs" :loading="isLoading" style="margin-left: 8px">
          查看发放日志
        </a-button>

      </div>
    </div>

    <a-alert
      v-if="!canManageCatalog"
      class="page-alert"
      type="info"
      show-icon
      message="当前账号为只读视角"
      description="你可以查看礼品详情与发放记录，新增、编辑由管理员或经理处理，删除仅管理员可见。"
    />

    <a-card class="table-card">
      <a-table
        :columns="columns"
        :data-source="dataSource"
        :pagination="pagination"
        :loading="isLoading"
        :rules="rules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
        @change="handleTableChange"
        rowKey="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'limitEnabled'">
            <a-tag :color="record.limitEnabled ? 'green' : 'red'">
              {{ record.limitEnabled ? '是' : '否' }}
            </a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'status'">
            <a-tag :color="getStatusColor(record.status)">
              {{ getStatusText(record.status) }}
            </a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'product'">
            {{ record.productName || '无关联商品' }}
          </template>
          <template v-else-if="column.dataIndex === 'type'">
            <a-tag :color="getTypeColor(record.type)">
              {{ getTypeText(record.type) }}
            </a-tag>
          </template>
        </template>

        <template #action="{ record }">
          <a-space :size="4">
            <a-button type="link" size="small" @click="handleGiftDetail(record)">
              详情
            </a-button>
            <a-button
              v-if="canManageCatalog"
              type="link"
              size="small"
              @click="handleGiftEdit(record)"
            >
              编辑
            </a-button>
            <a-button
              v-if="canDeleteCurrentGift"
              type="link"
              size="small"
              danger
              @click="handleDeleteGift(record)"
            >
              删除
            </a-button>
          </a-space>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:visible="modalVisible"
      :title="modalTitle"
      width="600px"
      @ok="handleAddOrEdit"
      @cancel="handleModalCancel"
    >
      <a-form
        ref="formRef"
        :model="formState"
        :rules="rules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="名称" name="name">
          <a-input v-model:value="formState.name" placeholder="请输入礼品名称" />
        </a-form-item>
        <a-form-item label="礼品编码">
          <a-input
            v-model:value="formState.code"
            placeholder="系统自动生成"
            readonly
            disabled
          />
        </a-form-item>
        <a-form-item label="关联商品" name="productId">
          <a-select
            v-model:value="formState.productId"
            placeholder="请选择商品"
            allow-clear
            show-search
            :filter-option="filterProductOption"
          >
            <a-select-option v-for="product in productOptions" :key="product.id" :value="product.id">
              {{ product.name }} ({{ product.code }})
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="礼品类型" name="type">
          <a-select
            v-model:value="formState.type"
            placeholder="请选择礼品类型"
            allow-clear
          >
            <a-select-option value="NEW">邀约礼品</a-select-option>
            <a-select-option value="PHYSICAL">实体礼品</a-select-option>
            <a-select-option value="VIRTUAL">虚拟礼品</a-select-option>
            <a-select-option value="COUPON">优惠券</a-select-option>
            <a-select-option value="POINTS">积分</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="状态类型" name="status">
          <a-select
            v-model:value="formState.status"
            placeholder="请选择状态类型"
            allow-clear
          >
            <a-select-option value="ACTIVE">进行中</a-select-option>
            <a-select-option value="DEPLETED">已售罄</a-select-option>
            <a-select-option value="EXPIRED">已过期</a-select-option>
            <a-select-option value="PAUSED">已暂停</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="描述" name="description">
          <a-input v-model:value="formState.description" placeholder="请输入礼品描述" />
        </a-form-item>
        <a-form-item label="开启限制" name="limitEnabled">
          <a-switch v-model:checked="formState.limitEnabled" />
        </a-form-item>
        <a-form-item label="限制数量" name="limitPerPerson">
          <a-input-number v-model:value="formState.limitPerPerson" :disabled="!formState.limitEnabled" style="width: 100%" />
        </a-form-item>
        <a-form-item label="备注" name="remark">
          <a-textarea v-model:value="formState.remark" placeholder="请输入备注信息" :maxlength="200" show-count />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-drawer v-model:open="detailVisible" title="礼品详情" width="520" :destroy-on-close="true">
      <a-spin :spinning="detailLoading">
        <a-empty v-if="!detailGift" description="暂无礼品详情" />
        <a-descriptions v-else :column="1" bordered size="small">
          <a-descriptions-item label="礼品名称">{{ detailGift.name }}</a-descriptions-item>
          <a-descriptions-item label="礼品编码">{{ detailGift.code }}</a-descriptions-item>
          <a-descriptions-item label="礼品类型">{{ getTypeText(detailGift.type) }}</a-descriptions-item>
          <a-descriptions-item label="礼品状态">{{ getStatusText(detailGift.status) }}</a-descriptions-item>
          <a-descriptions-item label="关联商品">{{ detailGift.productName || '无关联商品' }}</a-descriptions-item>
          <a-descriptions-item label="限额开关">{{ detailGift.limitEnabled ? '开启' : '关闭' }}</a-descriptions-item>
          <a-descriptions-item label="每人限额">{{ detailGift.limitPerPerson ?? '--' }}</a-descriptions-item>
          <a-descriptions-item label="礼品描述">{{ detailGift.description || '--' }}</a-descriptions-item>
          <a-descriptions-item label="备注">{{ detailGift.remark || '--' }}</a-descriptions-item>
          <a-descriptions-item label="创建时间">{{ formatDateTime(detailGift.createdTime) }}</a-descriptions-item>
          <a-descriptions-item label="更新时间">{{ formatDateTime(detailGift.updatedTime) }}</a-descriptions-item>
        </a-descriptions>
      </a-spin>
    </a-drawer>
  </div>
</template>

<script lang="ts" setup>
import dayjs from 'dayjs'
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Modal, message } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useGiftStore } from '@/stores/gift'
import { useProductStore } from '@/stores/product'
import { canDeleteGift, canManageGiftCatalog } from '@/router/accessControl'
import type { Gift, GiftCreateDTO, GiftUpdateDTO, PageParams } from '@/types'
import { buildServerPageParams, toServerPage } from '@/utils/pagination'

type GiftFormState = Omit<GiftCreateDTO, 'type' | 'limitPerPerson' | 'startTime' | 'endTime' | 'productId'> & {
  type: GiftUpdateDTO['type'] | 'NEW'
  limitPerPerson: number | null
  startTime: string | null
  endTime: string | null
  productId: number | null
  description?: string
}

const router = useRouter()
const authStore = useAuthStore()
const giftStore = useGiftStore()
const productStore = useProductStore()
const formRef = ref<FormInstance>()
const modalType = ref<'add' | 'edit'>('add')
const modalTitle = ref('')
const modalVisible = ref(false)
const currentGift = ref<Gift | null>(null)
const isLoading = ref(false)
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailGift = ref<Gift | null>(null)

const columns = [
  {
    title: '名称',
    dataIndex: 'name',
    key: 'name',
  },
  {
    title: '编码',
    dataIndex: 'code',
    key: 'code',
  },
  {
    title: '类型',
    dataIndex: 'type',
    key: 'type',
  },
  {
    title: '商品',
    dataIndex: 'product',
    key: 'product',
  },
  {
    title: '状态',
    dataIndex: 'status',
    key: 'status',
  },
  {
    title: '开启限制',
    dataIndex: 'limitEnabled',
    key: 'limitEnabled',
  },
  {
    title: '限制数量',
    dataIndex: 'limitPerPerson',
    key: 'limitPerPerson',
  },
  {
    title: '操作',
    key: 'action',
    fixed: 'right',
    width: 240,
    slots: { customRender: 'action' },
  },
]

const formState = reactive<GiftFormState>({
  name: '',
  code: '',
  status: 'ACTIVE',
  limitEnabled: true,
  limitPerPerson: null,
  startTime: null,
  endTime: null,
  remark: '',
  productId: null,
  type: 'NEW',
  isDeleted: 0,
  description: '',
})

const rules = {
  name: [{ required: true, message: '请输入礼品名称', trigger: 'blur' }],
  limitEnabled: [{ required: true, message: '请选择是否开启限制', trigger: 'change' }],
  limitPerPerson: [{ required: true, message: '请输入每人限制数量', trigger: 'blur' }],
  productId: [
    { required: true, message: '请选择关联商品', trigger: 'change' },
    {
      validator: (_rule: any, value: any) => {
        if (!value) {
          return Promise.reject('请选择关联商品')
        }
        if (Number.isNaN(value)) {
          return Promise.reject('关联商品ID必须是数字')
        }
        if (value <= 0) {
          return Promise.reject('关联商品ID必须大于0')
        }
        return Promise.resolve()
      },
      trigger: 'change',
    },
  ],
  type: [{ required: true, message: '请选择礼品类型', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态类型', trigger: 'change' }],
}

const dataSource = computed(() => giftStore.gifts)
const pagination = computed(() => ({
  current: giftStore.pagination.page,
  pageSize: giftStore.pagination.size,
  total: giftStore.pagination.total,
  showTotal: (total: number) => `共 ${total} 条数据`,
  showSizeChanger: true,
  showQuickJumper: true,
}))
const canManageCatalog = computed(() => canManageGiftCatalog(authStore.userRole))
const canDeleteCurrentGift = computed(() => canDeleteGift(authStore.userRole))

const filterProductOption = (input: string, option: any) => {
  const label = typeof option?.children === 'string' ? option.children : ''
  return label.toLowerCase().includes(input.toLowerCase())
}

const productOptions = computed(() => {
  return productStore.products.map(product => ({
    id: product.id,
    name: product.name,
    code: product.code,
  }))
})

const formatDateTime = (dateStr?: string | null) => {
  if (!dateStr) return '--'
  return dayjs(dateStr).format('YYYY-MM-DD HH:mm:ss')
}

const getStatusColor = (status: string) => {
  switch (status) {
    case 'ACTIVE':
      return 'green'
    case 'DEPLETED':
      return 'red'
    case 'DRAFT':
      return 'default'
    case 'EXPIRED':
      return 'orange'
    case 'PAUSED':
      return 'blue'
    default:
      return 'default'
  }
}

const getStatusText = (status: string) => {
  switch (status) {
    case 'ACTIVE':
      return '进行中'
    case 'DEPLETED':
      return '已售罄'
    case 'DRAFT':
      return '草稿'
    case 'EXPIRED':
      return '已过期'
    case 'PAUSED':
      return '已暂停'
    default:
      return '未知'
  }
}

const getTypeText = (type: string) => {
  switch (type) {
    case 'NEW':
      return '邀约礼品'
    case 'PHYSICAL':
      return '实体礼品'
    case 'VIRTUAL':
      return '虚拟礼品'
    case 'COUPON':
      return '优惠券'
    case 'POINTS':
      return '积分'
    default:
      return '未知'
  }
}

const getTypeColor = (type: string) => {
  switch (type) {
    case 'NEW':
      return 'blue'
    case 'PHYSICAL':
      return 'green'
    case 'VIRTUAL':
      return 'orange'
    case 'COUPON':
      return 'purple'
    case 'POINTS':
      return 'cyan'
    default:
      return 'default'
  }
}

const resetFormState = () => {
  Object.assign(formState, {
    name: '',
    code: '',
    status: 'ACTIVE',
    limitEnabled: true,
    limitPerPerson: null,
    startTime: null,
    endTime: null,
    remark: '',
    productId: null,
    type: 'NEW',
    isDeleted: 0,
    description: '',
  })
}

const handleAddOrEdit = async () => {
  try {
    if (!formRef.value) {
      message.error('表单未初始化')
      return
    }

    await formRef.value.validate()

    if (modalType.value === 'add') {
      await giftStore.createGift(formState as GiftCreateDTO)
      message.success('新增礼品成功')
    } else if (currentGift.value) {
      await giftStore.updateGift(currentGift.value.id, formState as unknown as GiftUpdateDTO)
      message.success('更新礼品成功')
    }

    modalVisible.value = false
  } catch {
    message.error('请检查表单输入是否正确')
  }
}

const handleModalCancel = () => {
  modalVisible.value = false
  formRef.value?.resetFields()
  resetFormState()
}

const showAddModal = () => {
  modalType.value = 'add'
  currentGift.value = null
  resetFormState()
  modalVisible.value = true
  modalTitle.value = '新增礼品'
}

const handleGiftEdit = (gift: Gift) => {
  currentGift.value = gift
  modalType.value = 'edit'
  Object.assign(formState, {
    name: gift.name,
    code: gift.code,
    description: gift.description,
    limitEnabled: gift.limitEnabled,
    limitPerPerson: gift.limitPerPerson,
    remark: gift.remark,
    productId: gift.productId,
    status: gift.status,
    type: gift.type,
    isDeleted: gift.isDeleted,
  })
  modalVisible.value = true
  modalTitle.value = '编辑礼品'
}

const handleGiftDetail = async (gift: Gift) => {
  detailVisible.value = true
  detailLoading.value = true
  detailGift.value = null

  try {
    detailGift.value = await giftStore.getGift(gift.id)
  } catch {
    message.error('加载礼品详情失败')
  } finally {
    detailLoading.value = false
  }
}

const handleDeleteGift = (gift: Gift) => {
  Modal.confirm({
    title: '删除礼品',
    content: `确定要删除礼品“${gift.name}”吗？若礼品已有关联发放记录，请以后端校验结果为准。`,
    okText: '确认删除',
    okButtonProps: { danger: true },
    cancelText: '取消',
    onOk: async () => {
      try {
        await giftStore.deleteGift(gift.id)
        message.success('礼品已删除')
        if (detailGift.value?.id === gift.id) {
          detailVisible.value = false
          detailGift.value = null
        }
      } catch (error: any) {
        message.error(error?.response?.data?.message || '删除礼品失败')
      }
    },
  })
}





const loadGifts = async (params?: PageParams) => {
  try {
    isLoading.value = true
    await giftStore.loadGifts({
      page: params?.page ?? 0,
      size: params?.size ?? 10,
    })
  } catch {
    message.error('加载礼品数据失败')
  } finally {
    isLoading.value = false
  }
}

const handleTableChange = (tablePagination: { current?: number; pageSize?: number }) => {
  void loadGifts(buildServerPageParams(tablePagination, giftStore.pagination.size))
}

const handleViewDistributionLogs = () => {
  router.push('/gift-logs')
}

onMounted(() => {
  void loadGifts()
  void productStore.loadProducts({ page: 0, size: 100 })
})
</script>

<style scoped>
.gift-page {
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

.page-alert {
  margin-bottom: 16px;
}

.table-card {
  border-radius: 8px;
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
