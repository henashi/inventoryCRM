<template>
  <div class="page-container">
    <div class="page-header">
      <h1 class="page-title">订单管理</h1>
      <div class="page-header-actions">
        <a-button type="primary" @click="showForm = true">新增订单</a-button>
      </div>
    </div>

    <a-card :bordered="false">
      <a-table
        row-key="id"
        :columns="columns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        size="small"
        :scroll="{ x: 700 }"
        @change="handleChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'customer'">
            <div>
              <div class="name-cell">{{ record.customerName }}</div>
              <div class="meta-cell">ID: {{ record.customerId }}</div>
            </div>
          </template>
          <template v-if="column.key === 'amount'">
            <div>
              <div class="amount-cell">¥{{ record.finalAmount.toFixed(2) }}</div>
              <div v-if="record.discount > 0" class="meta-cell">优惠 ¥{{ record.discount.toFixed(2) }}</div>
            </div>
          </template>
          <template v-if="column.key === 'time'">
            <span class="meta-cell">{{ record.orderTime?.slice(0, 16) }}</span>
          </template>
          <template v-if="column.key === 'items'">
            <span class="meta-cell">{{ record.items?.length || 0 }} 件商品</span>
          </template>
          <template v-if="column.key === 'action'">
            <a-popconfirm title="确定删除此订单？" @confirm="handleDelete(record.id)">
              <a-button type="link" danger size="small">删除</a-button>
            </a-popconfirm>
          </template>
        </template>

        <template #expandedRowRender="{ record }">
          <div v-if="record.items?.length" class="expanded-items">
            <div v-for="item in record.items" :key="item.id" class="item-row">
              <span class="item-name">{{ item.productName || '未命名' }}</span>
              <span class="meta-cell">×{{ item.quantity }}</span>
              <span class="amount-cell">¥{{ item.totalAmount.toFixed(2) }}</span>
            </div>
          </div>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="showForm" title="新增订单" @ok="handleSubmit" :confirm-loading="submitting" :width="600" :destroyOnClose="true">
      <a-form layout="vertical">
        <a-form-item label="客户" v-bind="validateInfos.customerId">
          <a-select v-model:value="form.customerId" show-search :filter-option="false" @search="searchCustomer" @focus="loadAllCustomers" placeholder="搜索客户姓名或手机号" not-found-content="未找到">
            <a-select-option v-for="c in customerOptions" :key="c.id" :value="c.id">{{ c.name }}（{{ c.phone }}）</a-select-option>
          </a-select>
        </a-form-item>

        <div class="divider-label">商品明细</div>

        <div v-for="(item, idx) in form.items" :key="idx" class="item-form-row">
          <a-row :gutter="8" type="flex" align="middle">
            <a-col :span="8">
              <a-form-item :label="idx === 0 ? '商品' : ''" style="margin-bottom: 0">
                <a-select v-model:value="item.productId" allow-clear placeholder="选填" style="width:100%" @change="(val) => onProductChange(idx, val)">
                  <a-select-option v-for="p in productOptions" :key="p.id" :value="p.id">{{ p.name }}</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :span="4">
              <a-form-item :label="idx === 0 ? '数量' : ''" style="margin-bottom: 0">
                <a-input-number v-model:value="item.quantity" :min="1" style="width:100%" @change="() => calcRow(idx)" />
              </a-form-item>
            </a-col>
            <a-col :span="5">
              <a-form-item :label="idx === 0 ? '单价' : ''" style="margin-bottom: 0">
                <a-input-number v-model:value="item.unitPrice" :min="0" :precision="2" style="width:100%" placeholder="0.00" @change="() => calcRow(idx)" />
              </a-form-item>
            </a-col>
            <a-col :span="5">
              <a-form-item :label="idx === 0 ? '金额' : ''" style="margin-bottom: 0">
                <a-input-number v-model:value="item.totalAmount" :min="0" :precision="2" prefix="¥" style="width:100%" @change="calcTotal" />
              </a-form-item>
            </a-col>
            <a-col :span="2" class="col-remove-btn" :class="{ 'pt-24': idx === 0 }">
              <a-button type="text" danger size="small" @click="removeItem(idx)" v-if="form.items.length > 1">✕</a-button>
            </a-col>
          </a-row>
        </div>

        <a-button type="dashed" block style="margin-top: 8px" @click="addItem">+ 添加商品</a-button>

        <div class="order-summary">
          <div class="summary-row">
            <span>小计</span>
            <span>¥{{ subtotal.toFixed(2) }}</span>
          </div>
          <div class="summary-row">
            <span>优惠</span>
            <a-input-number v-model:value="form.discount" :min="0" :precision="2" style="width:120px" placeholder="0.00" @change="calcTotal" />
          </div>
          <div class="summary-divider" />
          <div class="summary-row final">
            <span>合计</span>
            <span>¥{{ finalTotal.toFixed(2) }}</span>
          </div>
        </div>

        <a-form-item label="备注" style="margin-bottom: 0">
          <a-input v-model:value="form.remark" maxlength="200" show-count placeholder="选填" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { Form } from 'ant-design-vue'
const useForm = Form.useForm
import { orderApi } from '@/api/order'
import { customerApi } from '@/api/customer'
import { productApi } from '@/api/product'
import type { OrderDTO, OrderCreateDTO } from '@/types'

const list = ref<OrderDTO[]>([])
const loading = ref(false)
const showForm = ref(false)
const submitting = ref(false)
const subtotal = ref(0)
const finalTotal = ref(0)

interface FormItem {
  productId?: number
  productName?: string
  quantity: number
  unitPrice?: number
  totalAmount: number
}

const form = ref<{
  customerId: any
  items: FormItem[]
  discount: number
  remark: string
}>({
  customerId: undefined,
  items: [{ quantity: 1, totalAmount: 0 }],
  discount: 0,
  remark: '',
})

const rules = {
  customerId: [{ required: true, message: '请选择客户', trigger: 'change' }],
}
const { validate, validateInfos } = useForm(form, rules)

const customerOptions = ref<any[]>([])
const productOptions = ref<any[]>([])

const pagination = ref({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: false,
  showTotal: (t: number) => `共 ${t} 条`,
})

const columns = [
  { title: '客户', key: 'customer', width: 150 },
  { title: '商品数', key: 'items', width: 80 },
  { title: '实付金额', key: 'amount', width: 130 },
  { title: '下单时间', key: 'time', width: 140 },
  { title: '备注', dataIndex: 'remark', ellipsis: true },
  { title: '操作', key: 'action', width: 60, fixed: 'right' },
]

function loadData(page = 0) {
  loading.value = true
  orderApi.list({ page, size: pagination.value.pageSize }).then(res => {
    list.value = res.content
    pagination.value.total = res.totalElements
    pagination.value.current = page + 1
  }).catch(() => message.error('加载失败')).finally(() => loading.value = false)
}

function handleChange(pag: any) {
  loadData((pag.current - 1) || 0)
}

function loadAllCustomers() {
  if (customerOptions.value.length > 0) return
  customerApi.getCustomers({ page: 0, size: 200 }).then(res => {
    customerOptions.value = res.content
  }).catch(() => {})
}

function searchCustomer(keyword: string) {
  if (!keyword) { loadAllCustomers(); return }
  customerApi.searchCustomers(keyword).then(res => {
    customerOptions.value = res
  }).catch(() => {})
}

function addItem() {
  form.value.items.push({ quantity: 1, totalAmount: 0 })
}

function removeItem(idx: number) {
  form.value.items.splice(idx, 1)
  calcTotal()
}

function onProductChange(idx: number, productId: number | undefined) {
  const p = productOptions.value.find((x: any) => x.id === productId)
  if (p && form.value.items[idx]) {
    form.value.items[idx].productName = p.name
    if (p.price) form.value.items[idx].unitPrice = p.price
    calcRow(idx)
  }
}

function calcRow(idx: number) {
  const item = form.value.items[idx]
  if (item.quantity && item.unitPrice) {
    item.totalAmount = item.quantity * item.unitPrice
  }
  calcTotal()
}

function calcTotal() {
  const sum = form.value.items.reduce((s, i) => s + (i.totalAmount || 0), 0)
  subtotal.value = sum
  finalTotal.value = Math.max(0, sum - (form.value.discount || 0))
}

async function handleSubmit() {
  try { await validate() } catch { return }
  submitting.value = true
  try {
    const payload: OrderCreateDTO = {
      customerId: form.value.customerId,
      discount: form.value.discount || 0,
      remark: form.value.remark,
      items: form.value.items.map(i => ({
        productId: i.productId,
        productName: i.productName,
        quantity: i.quantity,
        unitPrice: i.unitPrice,
        totalAmount: i.totalAmount,
      })),
    }
    await orderApi.create(payload)
    message.success('订单已创建')
    showForm.value = false
    form.value = { customerId: undefined, items: [{ quantity: 1, totalAmount: 0 }], discount: 0, remark: '' }
    subtotal.value = 0; finalTotal.value = 0
    loadData()
  } catch { message.error('创建失败') }
  finally { submitting.value = false }
}

async function handleDelete(id: number) {
  try {
    await orderApi.delete(id)
    message.success('已删除')
    loadData(pagination.value.current - 1)
  } catch { message.error('删除失败') }
}

onMounted(() => {
  loadData()
  productApi.getProducts({ page: 0, size: 999 }).then(res => { productOptions.value = res.content || [] }).catch(() => {})
})
</script>

<style scoped>
.page-container { padding: 20px; background: #f5f7fa; min-height: 100vh; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-title { font-size: 24px; font-weight: 700; margin: 0; color: #111827; }
.name-cell { font-weight: 500; }
.meta-cell { color: #9ca3af; font-size: 12px; }
.amount-cell { font-weight: 600; color: #f5222d; }

.divider-label { font-size: 13px; color: #6b7280; margin: 8px 0; padding-bottom: 4px; border-bottom: 1px solid #f0f0f0; }
.item-form-row { background: #fafbfc; border-radius: 6px; padding: 4px 8px; margin-bottom: 4px; }
.col-remove-btn { display: flex; align-items: center; }
.pt-24 { padding-top: 24px; }

.order-summary { background: #f9fafb; border-radius: 8px; padding: 12px 16px; margin: 12px 0; }
.summary-row { display: flex; justify-content: space-between; align-items: center; padding: 4px 0; font-size: 14px; }
.summary-divider { height: 1px; background: #e5e7eb; margin: 4px 0; }
.summary-row.final { font-size: 18px; font-weight: 700; color: #f5222d; }

.expanded-items { padding: 4px 24px; }
.item-row { display: flex; gap: 16px; align-items: center; padding: 2px 0; }
.item-name { flex: 1; font-size: 13px; }
</style>
