<template>
  <a-modal
    :open="open"
    :title="modalTitle"
    :confirm-loading="loading"
    destroy-on-close
    @cancel="handleCancel"
    @ok="handleOk"
  >
    <a-form layout="vertical">
      <a-form-item v-if="!fixedInventory" label="选择商品" required>
        <a-select
          v-model:value="selectedInventoryId"
          show-search
          placeholder="请选择商品"
          :filter-option="filterOption"
        >
          <a-select-option
            v-for="item in inventoryOptions"
            :key="getInventoryIdentifier(item)"
            :value="getInventoryIdentifier(item)"
          >
            {{ item.productName }}（{{ item.productCode }}）
          </a-select-option>
        </a-select>
      </a-form-item>

      <a-card v-if="activeInventory" size="small" class="inventory-summary-card">
        <a-descriptions :column="1" size="small">
          <a-descriptions-item label="商品名称">
            {{ activeInventory.productName }}
          </a-descriptions-item>
          <a-descriptions-item label="商品编码">
            {{ activeInventory.productCode }}
          </a-descriptions-item>
          <a-descriptions-item label="当前库存">
            {{ activeInventory.currentStock }} {{ activeInventory.unit }}
          </a-descriptions-item>
          <a-descriptions-item label="安全库存">
            {{ activeInventory.safeStock }} {{ activeInventory.unit }}
          </a-descriptions-item>
        </a-descriptions>
      </a-card>

      <a-form-item v-if="mode === 'in' || mode === 'out'" :label="quantityLabel" required>
        <a-input-number
          v-model:value="quantity"
          :min="1"
          style="width: 100%"
          :addon-after="activeInventory?.unit || '件'"
        />
      </a-form-item>

      <a-form-item v-if="mode === 'adjust'" label="盘点后库存" required>
        <a-input-number
          v-model:value="actualQuantity"
          :min="0"
          style="width: 100%"
          :addon-after="activeInventory?.unit || '件'"
        />
      </a-form-item>

      <a-form-item v-if="mode" :label="reasonLabel" required>
        <a-textarea v-model:value="reason" :rows="3" :maxlength="200" />
      </a-form-item>

      <a-form-item label="备注">
        <a-textarea v-model:value="remark" :rows="3" :maxlength="500" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import type { Inventory, InventoryDetail, InventoryAdjustDTO, InventoryInDTO, InventoryOutDTO } from '@/types'

const props = defineProps<{
  open: boolean
  mode: 'in' | 'out' | 'adjust' | null
  inventory?: Inventory | InventoryDetail | null
  inventoryOptions: Inventory[]
  loading?: boolean
}>()

const emit = defineEmits<{
  cancel: []
  submit: [payload: { mode: 'in' | 'out' | 'adjust'; productId: number; data: InventoryInDTO | InventoryOutDTO | InventoryAdjustDTO }]
}>()

const selectedInventoryId = ref<number>()
const quantity = ref<number>(1)
const actualQuantity = ref<number>(0)
const reason = ref('')
const remark = ref('')

const modalTitle = computed(() => {
  if (props.mode === 'in') return '库存入库'
  if (props.mode === 'out') return '库存出库'
  if (props.mode === 'adjust') return '库存调整'
  return '库存操作'
})

const quantityLabel = computed(() => props.mode === 'in' ? '入库数量' : '出库数量')
const reasonLabel = computed(() => {
  if (props.mode === 'in') return '入库原因'
  if (props.mode === 'out') return '出库原因'
  if (props.mode === 'adjust') return '调整原因'
  return '原因'
})

const fixedInventory = computed(() => props.inventory || null)

const activeInventory = computed(() => {
  if (fixedInventory.value) {
    return fixedInventory.value
  }

  return props.inventoryOptions.find((item) => getInventoryIdentifier(item) === selectedInventoryId.value) || null
})

const getInventoryIdentifier = (inventory: Inventory | InventoryDetail) => inventory.id || inventory.productId

const resetForm = () => {
  const inventoryId = props.inventory ? getInventoryIdentifier(props.inventory) : undefined
  selectedInventoryId.value = inventoryId
  quantity.value = 1
  actualQuantity.value = props.inventory?.currentStock || 0
  reason.value = ''
  remark.value = ''
}

const filterOption = (input: string, option: { children?: string }) => {
  return String(option.children || '').toLowerCase().includes(input.toLowerCase())
}

const handleCancel = () => {
  emit('cancel')
}

const handleOk = () => {
  if (!props.mode) {
    return
  }

  const inventory = activeInventory.value
  if (!inventory) {
    message.warning('请先选择商品')
    return
  }

  const trimmedReason = reason.value.trim()
  const productId = getInventoryIdentifier(inventory)

  if (props.mode === 'in') {
    if (!quantity.value || quantity.value <= 0) {
      message.warning('请输入正确的入库数量')
      return
    }
    if (!trimmedReason) {
      message.warning('请输入入库原因')
      return
    }

    emit('submit', {
      mode: 'in',
      productId,
      data: {
        productId,
        quantity: quantity.value,
        reason: trimmedReason,
        remark: remark.value || undefined,
      },
    })
    return
  }

  if (props.mode === 'out') {
    if (!quantity.value || quantity.value <= 0) {
      message.warning('请输入正确的出库数量')
      return
    }
    if (quantity.value > inventory.currentStock) {
      message.warning('出库数量不能大于当前库存')
      return
    }
    if (!trimmedReason) {
      message.warning('请输入出库原因')
      return
    }

    emit('submit', {
      mode: 'out',
      productId,
      data: {
        productId,
        quantity: quantity.value,
        reason: trimmedReason,
        remark: remark.value || undefined,
      },
    })
    return
  }

  if (actualQuantity.value < 0) {
    message.warning('盘点后库存不能小于 0')
    return
  }
  if (!trimmedReason) {
    message.warning('请输入调整原因')
    return
  }

  emit('submit', {
    mode: 'adjust',
    productId,
    data: {
      actualQuantity: actualQuantity.value,
      reason: trimmedReason,
      remark: remark.value || undefined,
    },
  })
}

watch(
  () => [props.open, props.mode, props.inventory],
  () => {
    if (props.open) {
      resetForm()
    }
  },
  { immediate: true },
)
</script>

<style scoped>
.inventory-summary-card {
  margin-bottom: 16px;
}
</style>
