<template>
  <div class="product-list-page">
    <div class="page-header">
      <h1 class="page-title">商品管理</h1>
      <div class="page-actions">
        <a-button type="primary" @click="showAddModal">
          <plus-outlined />
          新增商品
        </a-button>
        <a-button @click="showImportModal">
          <import-outlined />
          导入商品
        </a-button>
        <a-button @click="handleExport" :loading="exportLoading">
          <export-outlined />
          导出商品
        </a-button>
        <a-button @click="handleRefresh" :loading="isLoading || statsLoading || categoryLoading">
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

          <a-col :xs="24" :sm="12" :md="8">
            <a-form-item label="分类">
              <a-select
                v-if="categoryLoading || categoryFilterState.showSelect"
                v-model:value="searchForm.category"
                placeholder="请选择分类"
                allow-clear
                style="width: 100%"
                :loading="categoryLoading"
              >
                <a-select-option v-for="category in categories" :key="category">
                  {{ category }}
                </a-select-option>
              </a-select>
              <a-alert
                v-else
                type="info"
                show-icon
                :message="categoryFilterState.emptyText"
              />
            </a-form-item>
          </a-col>

          <a-col :xs="24" :sm="24" :md="8">
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

    <a-row :gutter="[16, 16]" class="summary-row">
      <a-col v-for="card in productSummaryCards" :key="card.key" :xs="24" :sm="8">
        <a-card class="summary-card" :loading="statsLoading" @click="handleSummaryCardClick(card.key)">
          <div class="summary-title">{{ card.title }}</div>
          <div class="summary-value">{{ card.value }}</div>
          <div class="summary-helper">{{ card.helper }}</div>
        </a-card>
      </a-col>
    </a-row>

    <a-card class="table-card">
      <template #title>
        <div class="toolbar-row">
          <span>商品列表</span>
          <a-space>
            <a-button :type="quickFilter === 'all' ? 'primary' : 'default'" size="small" @click="changeQuickFilter('all')">
              全部商品
            </a-button>
            <a-button :type="quickFilter === 'lowStock' ? 'primary' : 'default'" size="small" @click="changeQuickFilter('lowStock')">
              只看低库存
            </a-button>
            <a-button :type="quickFilter === 'outOfStock' ? 'primary' : 'default'" size="small" @click="changeQuickFilter('outOfStock')">
              只看缺货
            </a-button>
          </a-space>
        </div>
      </template>
      <a-table
        :columns="columns"
        :data-source="tableProducts"
        :loading="isLoading"
        :pagination="pagination"
        row-key="id"
        @change="handleTableChange"
      >
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

          <template v-else-if="column.dataIndex === 'category'">
            <a-tag color="blue">{{ record.category || '未分类' }}</a-tag>
          </template>

          <template v-else-if="column.dataIndex === 'price'">
            <span class="price">¥{{ Number(record.price || 0).toFixed(2) }}</span>
          </template>

          <template v-else-if="column.dataIndex === 'currentStock'">
            <span :class="getStockClass(record)">
              {{ record.currentStock }} {{ record.unit }}
            </span>
          </template>

          <template v-else-if="column.dataIndex === 'safeStock'">
            <span :class="getStockClass(record)">
              {{ record.safeStock }} {{ record.unit }}
            </span>
          </template>

          <template v-else-if="column.dataIndex === 'status'">
            <a-tag :color="record.status === 1 ? 'green' : 'red'">
              {{ record.status === 1 ? '在售' : '停售' }}
            </a-tag>
          </template>

          <template v-else-if="column.dataIndex === 'actions'">
            <a-space size="small">
              <a-button type="link" size="small" @click="handleEdit(record)">
                编辑
              </a-button>
              <template v-if="canManageInventory">
                <a-button type="link" size="small" @click="goToInventoryDetail(record)">
                  库存详情
                </a-button>
                <a-button type="link" size="small" @click="goToInventoryAction(record, 'in')">
                  入库
                </a-button>
                <a-button type="link" size="small" @click="goToInventoryAction(record, 'out')">
                  出库
                </a-button>
              </template>
              <a-button
                v-if="record.status === 1"
                type="link"
                size="small"
                danger
                @click="handleDisable(record)"
              >
                <stop-outlined />
                停用
              </a-button>
              <a-button
                v-else
                type="link"
                size="small"
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

        <a-form-item label="分类" name="category">
          <a-input v-model:value="formState.category" placeholder="请输入商品分类" />
        </a-form-item>

        <a-form-item label="售价" name="price">
          <a-input-number
            v-model:value="formState.price"
            :min="0"
            :precision="2"
            style="width: 100%"
            placeholder="请输入售价"
          />
        </a-form-item>

        <a-form-item label="成本" name="cost">
          <a-input-number
            v-model:value="formState.cost"
            :min="0"
            :precision="2"
            style="width: 100%"
            placeholder="请输入成本"
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

    <a-modal
      v-model:open="importModalVisible"
      title="导入商品"
      width="760px"
      :footer="null"
      @cancel="closeImportModal"
    >
      <div class="import-section">
        <div class="import-header">
          <div>
            <div class="import-title">CSV 模板说明</div>
            <div class="import-subtitle">导入结果会返回失败明细，成功数据将自动刷新当前列表。</div>
          </div>
          <a-space>
            <a-button size="small" :disabled="!productImportMeta" @click="copyImportTemplateHeader">复制模板表头</a-button>
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

        <a-card v-if="productImportMeta" size="small" class="import-meta-card">
          <div class="meta-row">
            <span class="meta-label">模板字段</span>
            <a-space wrap>
              <a-tag v-for="field in productImportMeta.templateFields" :key="field">{{ field }}</a-tag>
            </a-space>
          </div>
          <div class="meta-row">
            <span class="meta-label">必填字段</span>
            <a-space wrap>
              <a-tag v-for="field in productImportMeta.requiredFields" :key="field" color="red">{{ field }}</a-tag>
            </a-space>
          </div>
          <div class="meta-row single-line">
            <span class="meta-label">判重策略</span>
            <span>{{ productImportMeta.duplicateStrategy }}</span>
          </div>
          <div class="meta-row">
            <span class="meta-label">补充说明</span>
            <ul class="meta-list">
              <li v-for="note in productImportMeta.notes" :key="note">{{ note }}</li>
            </ul>
          </div>
        </a-card>

        <a-card v-if="productImportResult" size="small" class="import-result-card">
          <div class="result-summary">
            <a-statistic title="成功导入" :value="productImportResult.successCount" />
            <a-statistic title="失败条数" :value="productImportResult.failureCount" />
          </div>

          <a-table
            v-if="productImportResult.failureDetails.length"
            :data-source="productImportResult.failureDetails"
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
  </div>
</template>

<script setup lang="ts">
import dayjs from 'dayjs'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Modal, type FormInstance } from 'ant-design-vue'
import {
  PlusOutlined,
  ReloadOutlined,
  HomeOutlined,
  StopOutlined,
  CheckOutlined,
  ImportOutlined,
  ExportOutlined,
} from '@ant-design/icons-vue'
import { canAccessFeature } from '@/router/accessControl'
import { useAuthStore } from '@/stores/auth'
import { useProductStore } from '@/stores/product'
import { productApi } from '@/api/product'
import type { Product, ProductCreateDTO, PageParams } from '@/types'
import {
  buildCategoryFilterState,
  buildProductImportMeta,
  buildProductListSummary,
  resolveProductListParams,
  type ImportResult,
  type ProductQuickFilter,
  type ProductStockStatistics,
  type ProductSummaryCard,
} from '@/utils/featureEnhancements'

type ProductFormState = ProductCreateDTO & {
  category: string
  remark?: string
  status: 0 | 1
}

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const productStore = useProductStore()
const formRef = ref<FormInstance>()

const isLoading = ref(false)
const statsLoading = ref(false)
const categoryLoading = ref(false)
const exportLoading = ref(false)
const modalVisible = ref(false)
const modalType = ref<'add' | 'edit'>('add')
const currentProduct = ref<Product | null>(null)
const modalTitle = ref('')
const quickFilter = ref<ProductQuickFilter>('all')
const categories = ref<string[]>([])
const productSummaryCards = ref<ProductSummaryCard[]>([])
const lowStockProducts = ref<Product[]>([])
const importModalVisible = ref(false)
const importLoading = ref(false)
const importMetaLoading = ref(false)
const importMetaError = ref('')
const selectedImportFile = ref<File | null>(null)
const importInputKey = ref(0)
const productImportTemplate = ref<ImportResult | null>(null)
const productImportResult = ref<ImportResult | null>(null)

const searchForm = reactive({
  keyword: '',
  category: undefined as string | undefined,
})

const formState = reactive<ProductFormState>({
  name: '',
  code: '',
  category: '',
  price: 0,
  cost: 0,
  currentStock: 0,
  safeStock: 0,
  unit: '个',
  status: 1,
  remark: '',
})

const rules = {
  name: [{ required: true, message: '请输入商品名称' }],
  code: [{ required: true, message: '请输入商品编码' }],
  category: [{ required: true, message: '请输入商品分类' }],
  price: [{ required: true, message: '请输入售价' }],
  cost: [{ required: true, message: '请输入成本' }],
  unit: [{ required: true, message: '请输入单位' }],
}

const columns = [
  {
    title: '商品信息',
    dataIndex: 'name',
    key: 'name',
    width: 200,
  },
  {
    title: '分类',
    dataIndex: 'category',
    key: 'category',
    width: 120,
  },
  {
    title: '售价',
    dataIndex: 'price',
    key: 'price',
    width: 100,
  },
  {
    title: '库存',
    dataIndex: 'currentStock',
    key: 'currentStock',
    width: 100,
  },
  {
    title: '安全库存',
    dataIndex: 'safeStock',
    key: 'safeStock',
    width: 100,
  },
  {
    title: '状态',
    dataIndex: 'status',
    key: 'status',
    width: 80,
  },
  {
    title: '操作',
    dataIndex: 'actions',
    key: 'actions',
    width: 240,
  },
]

const canManageInventory = computed(() => canAccessFeature(authStore.userRole, 'inventory'))
const categoryFilterState = computed(() => buildCategoryFilterState(categories.value))
const tableProducts = computed(() => quickFilter.value === 'all' ? productStore.products : lowStockProducts.value)
const pagination = computed(() => {
  if (quickFilter.value !== 'all') {
    return false
  }

  return {
    current: productStore.pagination.page,
    pageSize: productStore.pagination.size,
    total: productStore.pagination.total,
    pageSizeOptions: ['5', '10', '20'],
    showSizeChanger: true,
    showQuickJumper: true,
  }
})
const productImportMeta = computed(() => buildProductImportMeta(productImportResult.value || productImportTemplate.value || undefined))

const getFirstChar = (name: string) => name.charAt(0).toUpperCase()
const getProductColor = (name: string) => {
  const colors = ['#1890ff', '#52c41a', '#faad14', '#f5222d']
  const index = name.charCodeAt(0) % colors.length
  return colors[index]
}

const generateProductCode = () => `SP${dayjs().format('YYYYMMDDHHmmssSSS')}`

const handleRegenerateCode = () => {
  formState.code = generateProductCode()
}

const getStockClass = (product: Product) => {
  if (product.currentStock <= 0) return 'out-of-stock'
  if (product.currentStock < product.safeStock) return 'low-stock'
  return 'normal-stock'
}

const buildListParams = (params?: PageParams) => ({
  page: params?.page ?? (productStore.pagination.page - 1),
  size: params?.size ?? productStore.pagination.size,
  ...resolveProductListParams(searchForm, 'all'),
})

const matchesQuickFilter = (product: Product) => {
  if (quickFilter.value === 'outOfStock') {
    return product.currentStock <= 0
  }
  return product.currentStock < product.safeStock
}

const matchesSearchFilter = (product: Product) => {
  const keyword = searchForm.keyword.trim().toLowerCase()
  const category = searchForm.category

  if (keyword) {
    const matched = [product.name, product.code, product.category]
      .filter(Boolean)
      .some((value) => value!.toLowerCase().includes(keyword))

    if (!matched) {
      return false
    }
  }

  if (category && product.category !== category) {
    return false
  }

  return true
}

const loadLowStockProducts = async () => {
  const response = await productApi.getLowStockProducts() as unknown as Product[]
  lowStockProducts.value = response
    .filter(matchesQuickFilter)
    .filter(matchesSearchFilter)
}

const loadProducts = async (params?: PageParams) => {
  try {
    isLoading.value = true
    if (quickFilter.value === 'all') {
      await productStore.loadProducts(buildListParams(params))
      return
    }

    await loadLowStockProducts()
  } catch {
    message.error('加载商品列表失败')
  } finally {
    isLoading.value = false
  }
}

const loadProductSummary = async () => {
  try {
    statsLoading.value = true
    const stats = await productApi.getStockStatistics() as unknown as ProductStockStatistics
    productSummaryCards.value = buildProductListSummary(stats)
  } catch {
    message.error('加载商品统计失败')
  } finally {
    statsLoading.value = false
  }
}

const loadCategories = async () => {
  try {
    categoryLoading.value = true
    const list = await productStore.loadCategories() as unknown as string[]
    categories.value = Array.from(new Set((list || []).filter(Boolean)))
    if (!categories.value.includes(searchForm.category || '')) {
      searchForm.category = undefined
    }
  } catch {
    categories.value = []
    searchForm.category = undefined
    message.warning('商品分类加载失败')
  } finally {
    categoryLoading.value = false
  }
}

const handleSearch = () => {
  loadProducts({ page: 0, size: productStore.pagination.size })
}

const handleReset = () => {
  searchForm.keyword = ''
  searchForm.category = undefined
  quickFilter.value = 'all'
  handleSearch()
}

const handleTableChange = (pag: { current?: number; pageSize?: number }) => {
  if (quickFilter.value !== 'all') {
    return
  }

  loadProducts({
    page: (pag.current || 1) - 1,
    size: pag.pageSize,
  })
}

const handleRefresh = async () => {
  await Promise.all([loadProducts(), loadProductSummary(), loadCategories()])
  message.success('刷新成功')
}

const handleBack = () => {
  router.push('/')
}

const resetFormState = () => {
  Object.assign(formState, {
    name: '',
    code: generateProductCode(),
    category: '',
    price: 0,
    cost: 0,
    currentStock: 0,
    safeStock: 0,
    unit: '件',
    status: 1,
    remark: '',
  })
}

const showAddModal = () => {
  modalType.value = 'add'
  resetFormState()
  modalVisible.value = true
  modalTitle.value = '新增商品'
}

const handleEdit = (record: Product) => {
  modalType.value = 'edit'
  currentProduct.value = record
  Object.assign(formState, {
    name: record.name,
    code: record.code,
    category: record.category || '',
    price: record.price,
    cost: record.cost || 0,
    currentStock: record.currentStock,
    safeStock: record.safeStock,
    unit: record.unit,
    status: record.status,
    remark: record.description || '',
  })
  modalVisible.value = true
  modalTitle.value = '编辑商品'
}

const goToInventoryDetail = (record: Product) => {
  router.push(`/inventory/${record.id}`)
}

const goToInventoryAction = (record: Product, action: 'in' | 'out') => {
  router.push({
    path: '/inventory',
    query: { action, productId: record.id },
  })
}

const handleModalOk = async () => {
  try {
    await formRef.value?.validate()
    const payload = {
      ...formState,
      category: formState.category.trim(),
      remark: formState.remark?.trim() || undefined,
    }

    if (modalType.value === 'add') {
      await productStore.addProduct(payload)
      message.success('添加成功')
    } else {
      await productStore.updateProduct(currentProduct.value!.id!, payload)
      message.success('更新成功')
    }

    modalVisible.value = false
    await Promise.all([loadProducts(), loadProductSummary(), loadCategories()])
  } catch {
    message.error('操作失败')
  }
}

const handleModalCancel = () => {
  modalVisible.value = false
}

const handleDelete = (record: Product) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除商品"${record.name}"吗？`,
    okType: 'danger',
    onOk: async () => {
      try {
        await productStore.deleteProduct(record.id!)
        message.success('删除成功')
        await Promise.all([loadProducts(), loadProductSummary(), loadCategories()])
      } catch {
        message.error('删除失败')
      }
    },
  })
}

const handleDisable = (record: Product) => {
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
        await Promise.all([loadProducts(), loadProductSummary()])
      } catch {
        message.error('停用失败')
      }
    },
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
        await Promise.all([loadProducts(), loadProductSummary()])
      } catch {
        message.error('启用失败')
      }
    },
  })
}

const downloadBlob = (blob: Blob, fileName: string) => {
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  link.click()
  window.URL.revokeObjectURL(url)
}

const escapeCsvValue = (value: unknown) => {
  const text = String(value ?? '')
  if (text.includes(',') || text.includes('"') || text.includes('\n')) {
    return `"${text.replace(/"/g, '""')}"`
  }
  return text
}

const exportCurrentQuickFilter = () => {
  const headers = ['name', 'code', 'category', 'price', 'currentStock', 'safeStock', 'unit', 'status']
  const rows = lowStockProducts.value.map((product) => ([
    product.name,
    product.code,
    product.category || '',
    product.price,
    product.currentStock,
    product.safeStock,
    product.unit,
    product.status === 1 ? '在售' : '停售',
  ].map(escapeCsvValue).join(',')))
  const csv = [headers.join(','), ...rows].join('\n')
  downloadBlob(new Blob([`\uFEFF${csv}`], { type: 'text/csv;charset=utf-8' }), `商品列表_${dayjs().format('YYYYMMDDHHmmss')}.csv`)
}

const handleExport = async () => {
  try {
    exportLoading.value = true
    if (quickFilter.value !== 'all') {
      exportCurrentQuickFilter()
      return
    }

    const response = await productApi.exportProducts(resolveProductListParams(searchForm, 'all')) as unknown as Blob
    const blob = response instanceof Blob ? response : new Blob([response], { type: 'text/csv;charset=utf-8' })
    downloadBlob(blob, `商品列表_${dayjs().format('YYYYMMDDHHmmss')}.csv`)
  } catch {
    message.error('导出失败')
  } finally {
    exportLoading.value = false
  }
}

const resetImportState = () => {
  selectedImportFile.value = null
  productImportTemplate.value = null
  productImportResult.value = null
  importMetaError.value = ''
  importInputKey.value += 1
}

const loadImportTemplateMeta = async () => {
  try {
    importMetaLoading.value = true
    importMetaError.value = ''
    const result = await productApi.getImportTemplate() as unknown as ImportResult
    productImportTemplate.value = result
  } catch {
    productImportTemplate.value = null
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
  productImportResult.value = null
}

const submitImport = async () => {
  if (!productImportMeta.value && !importMetaLoading.value) {
    await loadImportTemplateMeta()
  }

  if (!selectedImportFile.value) {
    message.warning('请先选择待导入的 CSV 文件')
    return
  }

  try {
    importLoading.value = true
    const result = await productApi.importProducts(selectedImportFile.value) as unknown as ImportResult
    productImportResult.value = result
    message.success(`导入完成：成功 ${result.successCount} 条，失败 ${result.failureCount} 条`)
    await Promise.all([loadProducts(), loadProductSummary(), loadCategories()])
  } catch {
    message.error('导入失败')
  } finally {
    importLoading.value = false
  }
}

const copyImportTemplateHeader = async () => {
  if (!productImportMeta.value) {
    message.warning('导入模板说明尚未就绪')
    return
  }

  try {
    await navigator.clipboard.writeText(productImportMeta.value.templateFields.join(','))
    message.success('模板表头已复制')
  } catch {
    message.warning('复制失败，请手动复制模板字段')
  }
}

const changeQuickFilter = async (nextFilter: ProductQuickFilter) => {
  quickFilter.value = nextFilter
  await loadProducts({ page: 0, size: productStore.pagination.size })
}

const handleSummaryCardClick = (key: ProductSummaryCard['key']) => {
  if (key === 'lowStockProducts') {
    changeQuickFilter('lowStock')
    return
  }

  changeQuickFilter('all')
}

watch(
  () => route.params.code,
  (newCode, oldCode) => {
    if (newCode) {
      searchForm.keyword = Array.isArray(newCode) ? newCode[0] || '' : newCode || ''
      if (oldCode !== newCode) {
        handleSearch()
      }
      return
    }

    searchForm.keyword = ''
  },
  { immediate: true },
)

onMounted(async () => {
  if (!route.params.code) {
    await loadProducts({ page: 0, size: productStore.pagination.size })
  }
  await Promise.all([loadProductSummary(), loadCategories()])
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

.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.search-card,
.summary-row,
.table-card {
  margin-bottom: 16px;
}

.page-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.toolbar-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.summary-card {
  cursor: pointer;
  min-height: 120px;
}

.summary-title {
  color: #666;
  font-size: 13px;
}

.summary-value {
  font-size: 28px;
  font-weight: 700;
  margin-top: 8px;
}

.summary-helper {
  margin-top: 8px;
  color: #999;
  font-size: 12px;
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
  .toolbar-row,
  .import-header {
    flex-direction: column;
    align-items: stretch;
  }

  .page-actions {
    width: 100%;
  }
}
</style>
