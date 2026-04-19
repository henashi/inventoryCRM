<!-- frontend/src/views/product/ProductList.vue -->
<template>
  <div class="product-list-page">
    <!-- 页面标题和操作 -->
    <div class="page-header">
      <h1 class="page-title">商品管理</h1>
      <div class="page-actions">
        <a-button type="primary" @click="showAddModal">
          <plus-outlined />
          新增商品
        </a-button>
        <a-button @click="handleRefresh" :loading="isLoading">
          <reload-outlined />
          刷新
        </a-button>
        <a-button @click="handleBack" style="margin-right:8px">
          <template #icon>
            <home-outlined />
          </template>
          返回仪表盘
        </a-button>
      </div>
    </div>

    <!-- 搜索区域 -->
    <a-card class="search-card">
      <a-form layout="inline" :model="searchForm" @finish="handleSearch">
        <a-row :gutter="[16, 16]" style="width: 100%">
          <a-col :xs="24" :sm="12" :md="8">
            <a-form-item label="关键词">
              <a-input
                v-model:value="searchForm.keyword"
                placeholder="商品名称/编码"
                allow-clear
              />
            </a-form-item>
          </a-col>

          <!-- <a-col :xs="24" :sm="12" :md="8">
            <a-form-item label="分类">
              <a-select
                v-model:value="searchForm.category"
                placeholder="请选择分类"
                allow-clear
                style="width: 100%"
              >
                <a-select-option v-for="category in categories" :key="category">
                  {{ category }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col> -->

          <a-col :xs="24" :sm="12" :md="8">
            <a-form-item>
              <a-space>
                <a-button type="primary" html-type="submit" :loading="isLoading">
                  搜索
                </a-button>
                <a-button @click="handleReset">
                  重置
                </a-button>
              </a-space>
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-card>

    <!-- 商品表格 -->
    <a-card class="table-card">
      <a-table
        :columns="columns"
        :data-source="products"
        :loading="isLoading"
        :pagination="pagination"
        :row-key="record => record.id!"
        @change="handleTableChange"
      >
        <!-- 商品信息列 -->
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'name'">
            <div class="product-info">
              <a-avatar :size="32" :style="{ backgroundColor: getProductColor(record.name) }">
                {{ getFirstChar(record.name) }}
              </a-avatar>
              <div class="product-details">
                <div class="product-name">{{ record.name }}</div>
                <div class="product-code">{{ record.code }}</div>
              </div>
            </div>
          </template>

          <!-- 价格 -->
          <template v-else-if="column.dataIndex === 'price'">
            <span class="price">¥{{ record.price }}</span>
          </template>

          <!-- 库存 -->
          <template v-else-if="column.dataIndex === 'currentStock'">
            <span :class="getStockClass(record)">
              {{ record.currentStock }} {{ record.unit }}
            </span>
          </template>

          <!-- 安全库存 -->
          <template v-else-if="column.dataIndex === 'safeStock'">
            <span :class="getStockClass(record)">
              {{ record.safeStock }} {{ record.unit }}
            </span>
          </template>

          <!-- 状态 -->
          <template v-else-if="column.dataIndex === 'status'">
            <a-tag :color="record.status === 1 ? 'green' : 'red'">
              {{ record.status === 1 ? '在售' : '停售' }}
            </a-tag>
          </template>

          <!-- 操作 -->
          <template v-else-if="column.dataIndex === 'actions'">
            <a-space size="small">
              <a-button type="link" size="small" @click="handleEdit(record)">
                编辑
              </a-button>
              <a-button type="link" size="small" @click="showInStockModal(record)">
                入库
              </a-button>
              <a-button type="link" size="small" @click="showOutStockModal(record)">
                出库
              </a-button>
              <a-button type="link" size="small"
                v-if="record.status === 1"
                @click="handleDisable(record)"
                danger
              >
                <stop-outlined />
                停用
              </a-button>
              <a-button type="link" size="small"
                v-else
                @click="handleEnable(record)"
              >
                <check-outlined />
                启用
              </a-button>
              <a-button type="link" size="small" danger @click="handleDelete(record)">
                删除
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>
    <a-modal
      v-model:open="inStockModalVisible"
      title="快速入库"
      width="400px"
      :confirm-loading="inStockModalLoading"
      @ok="handelInStockOk"
      @cancel="handleInStockCancel"
    >
      <a-form :model="inStockForm" layout="vertical">
        <a-form-item label="商品">
          <div class="product-info-area">
            <!-- 第一行：商品名称和编码 -->
            <div class="product-header">
              <span class="product-name">{{ inStockForm.name }}</span>
              <span class="product-code">{{ inStockForm.code }}</span>
            </div>

            <!-- 第二行：库存信息 -->
            <div class="stock-info">
              <div class="stock-item">
                <span class="stock-label">当前库存</span>
                <span class="stock-value">{{ inStockForm.currentStock }}</span>
                <span class="stock-unit">{{ inStockForm.unit }}</span>
              </div>
            </div>
          </div>
        </a-form-item>

        <!-- 其他字段保持不变，只添加单位 -->
        <a-form-item label="入库数量" required>
          <a-input-number
            v-model:value="inStockForm.quantity"
            :min="1"
            style="width: 100%"
            placeholder="请输入数量"
            :addon-after="inStockForm.unit"
          />
        </a-form-item>

        <a-form-item label="原因">
          <a-textarea
            v-model:value="inStockForm.reason"
            placeholder="简要说明入库原因"
            :rows="3"
          />
        </a-form-item>
      </a-form>
    </a-modal>
    <a-modal
      v-model:open="outStockModalVisible"
      title="快速出库"
      width="400px"
      :confirm-loading="outStockModalLoading"
      @ok="handleOutStockOk"
      @cancel="handleOutStockCancel"
    >
      <a-form :model="outStockForm" layout="vertical">
        <a-form-item label="商品">
          <div class="product-info-area">
            <!-- 第一行：商品名称和编码 -->
            <div class="product-header">
              <span class="product-name">{{ outStockForm.name }}</span>
              <span class="product-code">{{ outStockForm.code }}</span>
            </div>

            <!-- 第二行：库存信息 -->
            <div class="stock-info">
              <div class="stock-item">
                <span class="stock-label">当前库存</span>
                <span class="stock-value">{{ outStockForm.currentStock }}</span>
                <span class="stock-unit">{{ outStockForm.unit }}</span>
              </div>
            </div>
          </div>
        </a-form-item>
        <a-form-item label="出库数量" required>
          <a-input-number
            v-model:value="outStockForm.quantity"
            :min="1"
            style="width: 100%"
            placeholder="请输入数量"
          />
        </a-form-item>
        <a-form-item label="原因">
          <a-textarea
            v-model:value="outStockForm.reason"
            placeholder="简要说明出库原因"
            :rows="2"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 新增/编辑模态框 -->
    <a-modal
      v-model:open="modalVisible"
      :title="modalTitle"
      width="600px"
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
        <a-form-item label="商品名称" name="name">
          <a-input v-model:value="formState.name" placeholder="请输入商品名称" />
        </a-form-item>


        <!-- 显示编码（只读） -->
        <a-form-item label="商品编码">
          <a-input
            v-model:value="formState.code"
            placeholder="系统自动生成"
            readonly
            disabled
          >
            <template #suffix>
              <reload-outlined @click="handleRegenerateCode" title="重新生成" />
            </template>
          </a-input>
        </a-form-item>

        <!-- <a-form-item label="分类" name="category">
          <a-select v-model:value="formState.category" placeholder="请选择分类">
            <a-select-option v-for="category in categories" :key="category">
              {{ category }}
            </a-select-option>
          </a-select>
        </a-form-item> -->

        <a-form-item label="售价" name="price">
          <a-input-number
            v-model:value="formState.price"
            :min="0.01"
            :precision="2"
            style="width: 100%"
            placeholder="请输入售价"
          />
        </a-form-item>

        <a-form-item label="库存" name="currentStock">
          <a-input-number
            v-model:value="formState.currentStock"
            :min="0"
            :disabled="modalTitle === '编辑商品'"
            style="width: 100%"
            placeholder="当前库存"
          />
        </a-form-item>

        <a-form-item label="安全库存" name="safeStock">
          <a-input-number
            v-model:value="formState.safeStock"
            :min="0"
            style="width: 100%"
            placeholder="安全库存"
          />
        </a-form-item>

        <a-form-item label="单位" name="unit">
          <a-input v-model:value="formState.unit" placeholder="如：件、箱、个" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute,useRouter } from 'vue-router'
import { message, Modal, type FormInstance } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import { useProductStore } from '@/stores/product'
import type { Product, ProductCreateDTO, PageParams, Props } from '@/types'

const router = useRouter()
const route = useRoute()
const productStore = useProductStore()
const formRef = ref<FormInstance>()

const props = defineProps<Props>();

// 状态
const isLoading = ref(false)
const modalVisible = ref(false)
const inStockModalVisible = ref(false)
const outStockModalVisible = ref(false)
const inStockModalLoading = ref(false)
const outStockModalLoading = ref(false)
const modalType = ref<'add' | 'edit'>('add')
const currentProduct = ref<Product | null>(null)
const modalTitle = ref('')
const inStockForm = reactive({
  id: 0,
  code: '',
  name: '',
  currentStock: 0,
  unit: '',
  quantity: 0,
  reason: ''
})
const outStockForm = reactive({
  id: 0,
  code: '',
  name: '',
  currentStock: 0,
  unit: '',
  quantity: 0,
  reason: ''
})
// 搜索表单
const searchForm = reactive({
  keyword: '',
  category: undefined as string | undefined
})

// 表单数据
const formState = reactive<ProductCreateDTO & { status: 0 | 1 }>({
  name: '',
  code: '',
  price: 0,
  currentStock: 0,
  safeStock: 0,
  unit: '个',
  status: 1
})

// 表单验证
const rules = {
  name: [{ required: true, message: '请输入商品名称' }],
  code: [{ required: true, message: '请输入商品编码' }],
  category: [{ required: true, message: '请选择分类' }],
  price: [{ required: true, message: '请输入售价' }],
  unit: [{ required: true, message: '请输入单位' }]
}

// 表格列
const columns = [
  {
    title: '商品信息',
    dataIndex: 'name',
    key: 'name',
    width: 200
  },
  // {
  //   title: '分类',
  //   dataIndex: 'category',
  //   key: 'category',
  //   width: 100
  // },
  {
    title: '售价',
    dataIndex: 'price',
    key: 'price',
    width: 100
  },
  {
    title: '库存',
    dataIndex: 'currentStock',
    key: 'currentStock',
    width: 100
  },
  {
    title: '安全库存',
    dataIndex: 'safeStock',
    key: 'safeStock',
    width: 100
  },
  {
    title: '状态',
    dataIndex: 'status',
    key: 'status',
    width: 80
  },
  {
    title: '操作',
    dataIndex: 'actions',
    key: 'actions',
    width: 150
  }
]

// 计算属性
const products = computed(() => productStore.products)
// const categories = computed(() => productStore.categories)
const pagination = computed(() => ({
  current: productStore.pagination.page,
  pageSize: productStore.pagination.size,
  total: productStore.pagination.total,
  pageSizeOptions: ['5', '10', '20'], // 可选的每页条数
  showSizeChanger: true,
  showQuickJumper: true
}))

// 工具方法
const getFirstChar = (name: string) => name.charAt(0).toUpperCase()
const getProductColor = (name: string) => {
  const colors = ['#1890ff', '#52c41a', '#faad14', '#f5222d']
  const index = name.charCodeAt(0) % colors.length
  return colors[index]
}

const getStockClass = (product: Product) => {
  if (product.currentStock <= 0) return 'out-of-stock'
  if (product.currentStock < product.safeStock) return 'low-stock' // 简单判断低库存
  return 'normal-stock'
}

// 数据加载
const loadProducts = async (params?: PageParams) => {
  console.log('loadProducts', params)
  try {
    isLoading.value = true
    await productStore.loadProducts(params)
  } catch (error) {
    message.error('加载商品列表失败')
  } finally {
    isLoading.value = false
  }
}

// 搜索
const handleSearch = () => {
  console.log('Search with:', searchForm)
  loadProducts({ page: 0, ...searchForm })
}

const handleReset = () => {
  searchForm.keyword = ''
  // searchForm.category = undefined
  handleSearch()
}

// 表格分页
const handleTableChange = (pag: any) => {
  console.log('Page:', pag)
  loadProducts({
    page: pag.current! - 1,
    size: pag.pageSize
  })
}

// 刷新
const handleRefresh = () => {
  loadProducts()
  message.success('刷新成功')
}

const handleBack = () => {
  router.push('/')
}

// 新增商品
const showAddModal = () => {
  modalType.value = 'add'
  Object.assign(formState, {
    name: '',
    code: '',
    category: '',
    price: 0,
    currentStock: 0,
    safeStock: 0,
    unit: '件',
    status: 1
  })

  modalVisible.value = true
  modalTitle.value = '新增商品'
}

// 编辑商品
const handleEdit = (record: Product) => {
  modalType.value = 'edit'
  currentProduct.value = record
  Object.assign(formState, {
    name: record.name,
    code: record.code,
    // category: record.category,
    price: record.price,
    currentStock: record.currentStock,
    safeStock: record.safeStock,
    unit: record.unit,
    status: record.status
  })
  modalVisible.value = true
  modalTitle.value = '编辑商品'
}

// 入库商品
const showInStockModal = (record: Product) => {
  Object.assign(inStockForm, {
    id: record.id,
    code: record.code,
    name: record.name,
    currentStock: record.currentStock,
    unit: record.unit,
    quantity: 1,
    reason: ''
  })

  inStockModalVisible.value = true
  currentProduct.value = record
}

// 出库商品
const showOutStockModal = (record: Product) => {
  Object.assign(outStockForm, {
    id: record.id,
    code: record.code,
    name: record.name,
    currentStock: record.currentStock,
    unit: record.unit,
    quantity: 1,
    reason: ''
  })

  outStockModalVisible.value = true
  currentProduct.value = record
}

// 保存商品
const handleModalOk = async () => {
  try {
    if (modalType.value === 'add') {
      await productStore.addProduct(formState)
      message.success('添加成功')
    } else {
      await productStore.updateProduct(currentProduct.value!.id!, formState)
      message.success('更新成功')
    }
    modalVisible.value = false
    loadProducts()
  } catch (error) {
    message.error('操作失败')
  }
}

const handleModalCancel = () => {
  modalVisible.value = false
}

const handelInStockOk = async () => {
  try {
    console.log('InStockForm:', inStockForm)
    await productStore.updateStock(currentProduct.value!.id!, inStockForm.quantity, 'IN')
    message.success('入库成功')
    inStockModalVisible.value = false
    loadProducts()
  } catch (error) {
    message.error('入库失败')
  }
}

const handleOutStockOk = async () => {
  try {
    console.log('InStockForm:', inStockForm)
    await productStore.updateStock(currentProduct.value!.id!, outStockForm.quantity, 'OUT')
    message.success('出库成功')
    outStockModalVisible.value = false
    loadProducts()
  } catch (error) {
    message.error('出库失败')
  }
}

const handleInStockCancel = () => {
  inStockModalVisible.value = false
}

const handleOutStockCancel = () => {
  outStockModalVisible.value = false
}

// 删除商品
const handleDelete = (record: Product) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除商品"${record.name}"吗？`,
    okType: 'danger',
    onOk: async () => {
      try {
        await productStore.deleteProduct(record.id)
        message.success('删除成功')
        loadProducts()
      } catch (error) {
        message.error('删除失败')
      }
    }
  })
}

// 启用/停用
const handleDisable = (record: Product) => {
  console.log('停用商品:', record)
  Modal.confirm({
    title: '确认停用',
    content: `确定要停用商品 "${record.name}" 吗？`,
    okText: '确定',
    cancelText: '取消',
    okType: 'danger',
    onOk: async () => {
      try {
        await productStore.updateProduct(record.id!, { status: 0 })
        message.success('停用成功')
        loadProducts()
      } catch (error) {
        message.error('停用失败')
      }
    }
  })
}

const handleEnable = (record: Product) => {
  Modal.confirm({
    title: '确认启用',
    content: `确定要启用商品 "${record.name}" 吗？`,
    okText: '确定',
    cancelText: '取消',
    onOk: async () => {
      try {
        await productStore.updateProduct(record.id!, { status: 1 })
        message.success('启用成功')
        loadProducts()
      } catch (error) {
        message.error('启用失败')
      }
    }
  })
}

watch(
  () => route.params.code,  // 监听特定参数
  (newCode, oldCode) => {
    console.log(`code 变化: ${oldCode} -> ${newCode}`)
    if (newCode) {
      // 如果 URL 中有 code 参数，直接搜索该商品
      searchForm.keyword = Array.isArray(newCode) ? newCode[0] || '' : newCode || ''
      if (oldCode && oldCode !== newCode) {
        // 只有当 code 发生变化时才触发搜索，避免重复请求
        handleSearch()
      }
    } else {
      // 如果没有 code 参数，清空搜索框
      searchForm.keyword = ''
    }
  },
  { immediate: true }
)

// 初始化
onMounted(() => {
  handleSearch()
})
</script>

<style scoped>
.product-list-page {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.search-card {
  margin-bottom: 16px;
}

.page-actions {
  display: flex;
  gap: 8px;
}

.product-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.product-details {
  display: flex;
  flex-direction: column;
}

.product-name {
  font-weight: 500;
}

.product-code {
  font-size: 12px;
  color: #666;
}

.price {
  color: #f56c6c;
  font-weight: 500;
}

.out-of-stock {
  color: #ff4d4f;
  font-weight: 500;
}

.low-stock {
  color: #faad14;
  font-weight: 500;
}

.normal-stock {
  color: #52c41a;
}

/* 商品信息区域样式 */
.product-info-area {
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  padding: 16px;
  background-color: #fafafa;
  margin-bottom: 8px;
}

/* 商品标题行 */
.product-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px dashed #e8e8e8;
}

.product-header .product-name {
  font-size: 16px;
  font-weight: 600;
  color: #1890ff;
  display: flex;
  align-items: center;
  gap: 8px;
}

.product-header .product-name::before {
  content: "";
  display: inline-block;
  width: 4px;
  height: 16px;
  background-color: #1890ff;
  border-radius: 2px;
}

.product-code {
  font-size: 12px;
  color: #666;
  background-color: #f5f5f5;
  padding: 2px 8px;
  border-radius: 10px;
  border: 1px solid #e8e8e8;
}

</style>
