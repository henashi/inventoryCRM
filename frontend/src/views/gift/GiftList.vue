<template>
  <div class="gift-page">
    <div class="page-header">
      <h1 class="page-title">礼品管理</h1>
      <div class="page-actions">
          <a-button @click="handleRefresh" :loading="isLoading">
            <reload-outlined />
            刷新
          </a-button>
          <a-button type="primary" @click="showAddModal" style="margin-left:8px">
            <plus-outlined />
            新增礼品
          </a-button>
          <a-button @click="handleViewDistributionLogs" :loading="isLoading" style="margin-left:8px">
            查看发放日志
          </a-button>
          <a-button @click="handleBack" style="margin-left:8px">
            <template #icon>
              <home-outlined />
            </template>
            返回
          </a-button>
      </div>
    </div>
    <a-card class="table-card">
      <a-table
        :columns="columns"
        :data-source="dataSource"
        :pagination="pagination"
        :loading="isLoading"
        :rules="rules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
        @change="loadGifts"
        rowKey="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'limitEnabled'">
            <a-tag :color="record.limitEnabled ? 'green' : 'red'">
              {{ record.limitEnabled ? '是' : '否' }}
            </a-tag>
          </template>
          <!-- <template v-else-if="column.dataIndex === 'startTime' || column.dataIndex === 'endTime'">
            {{ formatDateTime(record[column.dataIndex]) }}
          </template> -->
          <template v-else-if="column.dataIndex === 'status'">
            <a-tag :color="getStatusColor(record.status)">
              {{ getStatusText(record.status) }}
            </a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'product'">
            {{ record.productName ? record.productName : '无关联商品' }}
          </template>
          <template v-else-if="column.dataIndex === 'type'">
            <a-tag :color="getTypeColor(record.type)">
              {{ getTypeText(record.type) }}
            </a-tag>
          </template>
        </template>

        <template #action="{ record }">
          <a-button type="link" @click="router.push(`/gift/${record.id}`)">
            查看详情
          </a-button>
          <a-button
            type="link"
            size="small"
            @click="handleGiftEdit(record)"
          >
            编辑
          </a-button>
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
        <!-- 显示编码（只读） -->
        <a-form-item label="礼品编码">
          <a-input
            v-model:value="formState.code"
            placeholder="系统自动生成"
            readonly
            disabled
          >
          </a-input>
        </a-form-item>
        <!-- <a-form-item label="邀新等级" name="newGiftLevel">
          <a-select
            v-model:value="formState.newGiftLevel"
            placeholder="请选择邀新等级"
            allow-clear>
            <a-select-option value="0">首次邀约礼品</a-select-option>
            <a-select-option value="1">进阶邀约礼品</a-select-option>
            <a-select-option value="2">最终邀新礼品</a-select-option>
          </a-select>
        </a-form-item> -->

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
            allow-clear>
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
            allow-clear>
            <a-select-option value="ACTIVE">进行中</a-select-option>
            <a-select-option value="DEPLETED">已售罄</a-select-option>
            <a-select-option value="EXPIRED">已过期</a-select-option>
            <a-select-option value="PAUSED">已暂停</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="描述" name="description">
          <a-input v-model:value="formState.description" placeholder="请输入礼品描述" />
        </a-form-item>
        <!-- <a-form-item label="开始时间" name="startTime">
          <a-date-picker v-model:value="formState.startTime" show-time style="width: 100%" />
        </a-form-item>
        <a-form-item label="结束时间" name="endTime">
          <a-date-picker v-model:value="formState.endTime" show-time style="width: 100%" />
        </a-form-item> -->
        <a-form-item label="开启限制" name="limitEnabled">
          <a-switch v-model:checked="formState.limitEnabled" />
        </a-form-item>
        <a-form-item label="限制数量" name="limitPerPerson">
          <a-input-number v-model:value="formState.limitPerPerson" :disabled="!formState.limitEnabled" style="width: 100%" />
        </a-form-item>
        <a-form-item label="备注" name="remark">
          <a-textarea v-model:value="formState.remark" placeholder="请输入备注信息" :maxlength="200" show-count/>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>
<script lang="ts" setup>
import dayjs from 'dayjs'
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useGiftStore } from '@/stores/gift'
import { useProductStore } from '@/stores/product'
import type { Gift, GiftDTO, PageParams } from '@/types'
import { message } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import { ReloadOutlined, HomeOutlined, PlusOutlined } from '@ant-design/icons-vue'

const productStore = useProductStore()
const modalType = ref<'add' | 'edit'>('add')
const modalTitle = ref('')
const modalVisible = ref(false)
const formRef = ref<FormInstance>()
const router = useRouter()
const giftStore = useGiftStore()
const currentGift = ref<Gift | null>(null)
const isLoading = ref(false);
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
    key: 'product'
  },
  {
    title: '状态',
    dataIndex: 'status',
    key: 'status',
    render: (text: string) => {
      switch (text) {
        case 'ACTIVE':
          return '进行中'
        case 'DEPLETED':
          return '已售罄'
        // case 'DRAFT':
        //   return '草稿'
        case 'EXPIRED':
          return '已过期'
        case 'PAUSED':
          return '已暂停'
        default:
          return '未知'
      }
    }
  },
  // {
  //   title: '描述',
  //   dataIndex: 'description',
  //   key: 'description',
  // },
  // {
  //   title: '开始时间',
  //   dataIndex: 'startTime',
  //   key: 'startTime',
  // },
  // {
  //   title: '结束时间',
  //   dataIndex: 'endTime',
  //   key: 'endTime',
  // },
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
    width: 200,
    slots: { customRender: 'action' },
  },
]
// 表单数据
const formState = reactive<GiftDTO>({
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
})

const rules = {
  name: [{ required: true, message: '请输入礼品名称', trigger: 'blur' }],
  // startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  // endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
  limitEnabled: [{ required: true, message: '请选择是否开启限制', trigger: 'change' }],
  limitPerPerson: [{ required: true, message: '请输入每人限制数量', trigger: 'blur' }],
  productId: [
    { required: true, message: '请选择关联商品', trigger: 'change' },
    {
      validator: (rule: any, value: any) => {
        if (!value) {
          return Promise.reject('请选择关联商品')
        }
        if (isNaN(value)) {
          return Promise.reject('关联商品ID必须是数字')
        }
        if (value <= 0) {
          return Promise.reject('关联商品ID必须大于0')
        }
        return Promise.resolve()
      },
      trigger: 'change',
    }
  ],
  type:[
    { required: true, message: '请选择礼品类型', trigger: 'change' },
  ],
  status:[
    { required: true, message: '请选择状态类型', trigger: 'change' },
  ],
}
const dataSource = computed(() => giftStore.gifts)
const pagination = computed(() => ({
  ...giftStore.pagination,
  showTotal: (total: number) => `共 ${total} 条数据`,
  showSizeChanger: true,
  showQuickJumper: true,
}))

const filterProductOption = (input: string, option: any) => {
  return (
    option.children.toLowerCase().indexOf(input.toLowerCase()) >= 0
  )
}

const productOptions = computed(() => {
  // 从giftStore中获取所有商品数据，转换成Select组件需要的格式
  return productStore.products.map(product => ({
    id: product.id,
    name: product.name,
    code: product.code,
  }))
})

const formatDate = (dateStr: string, format = 'YYYY-MM-dd') => {
  if (!dateStr) return ''
  return dayjs(dateStr).format(format)
}

const formatDateTime = (dateStr: string) => {
  return formatDate(dateStr, 'YYYY-MM-DD HH:mm:ss')
}

const getStatusColor = (status: string) => {
  switch (status) {
    case 'ACTIVE':
      return 'green'
    case 'DEPLETED':
      return 'red'
    case 'DRAFT':
      return 'gray'
    case 'EXPIRED':
      return 'orange'
    case 'PAUSED':
      return 'blue'
    default:
      return 'default'
  }
}

const handleAddOrEdit = async () => {
  try {
    if (!formRef.value) {
      message.error('表单未初始化')
      return
    }
    await formRef.value.validate()
    if (modalType.value === 'add') {
      await giftStore.createGift(formState)
      message.success('新增礼品成功')
    } else {
      // 编辑逻辑（如果需要）
      await giftStore.updateGift(currentGift.value!.id, formState)
      message.success('更新礼品成功')
    }
    modalVisible.value = false
    loadGifts()
  } catch (error) {
    console.error('表单验证失败:', error)
    message.error('请检查表单输入是否正确')
  }
}

const handleModalCancel = () => {
  modalVisible.value = false
  // 重置表单
  formRef.value?.resetFields();
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
      return '邀新礼品'
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

// 新增商品
const showAddModal = () => {
  modalType.value = 'add'
  Object.assign(formState, {
    name: '',
    code: '',
    status: 'ACTIVE',
    limitEnabled: true,
    limitPerPerson: null,
    // startTime: null,
    // endTime: null,
    remark: '',
    productId: null,
    type: 'NEW',
    isDeleted: 0,
  })

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
    // startTime: dayjs(gift.startTime),
    // endTime: dayjs(gift.endTime),
    limitEnabled: gift.limitEnabled,
    limitPerPerson: gift.limitPerPerson,
    remark: gift.remark,
    productId: gift.productId,
    status: gift.status,
  })
  modalVisible.value = true
  modalTitle.value = '编辑礼品'
}

const handleRefresh = () => {
  loadGifts()
  message.success('刷新成功')
}

const handleBack = () => {
  router.push('/')
}

const loadGifts = async (params?: PageParams) => {
  console.log('加载礼品数据，参数:', params)
  try {
    isLoading.value = true

    const queryParams: PageParams = {
      page: params?.page || 0,
      size: params?.size || 10,
    }

    await giftStore.loadGifts(queryParams)
  } catch (error) {
    message.error('加载礼品数据失败')
  }
  finally {
    isLoading.value = false
  }
}

const handleViewDistributionLogs = () => {
  router.push('/gift-logs')
}

onMounted(() => {
  loadGifts()
  productStore.loadProducts({ page: 0, size: 100 }) // 加载前100个商品用于选择
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
  margin-bottom: 20px;
}

.table-card {
  border-radius: 8px;
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .stats-cards {
    margin-bottom: 12px;
  }
}
</style>
