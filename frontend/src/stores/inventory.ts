import { defineStore } from 'pinia'
import { ref } from 'vue'
import { inventoryApi } from '@/api/inventory'
import type {
  Inventory,
  InventoryAdjustDTO,
  InventoryDetail,
  InventoryInDTO,
  InventoryOutDTO,
  InventoryQueryParams,
  PageResult,
} from '@/types'

const buildInventoryQueryParams = (params?: InventoryQueryParams) => {
  const queryParams = {
    page: params?.page,
    size: params?.size,
    keyword: params?.keyword,
    productId: params?.productId,
    minStock: params?.minStock,
    maxStock: params?.maxStock,
    lowStockOnly: params?.lowStockOnly,
    status: params?.status,
  }

  return Object.fromEntries(
    Object.entries(queryParams).filter(([, value]) => value !== undefined && value !== null && value !== ''),
  )
}

const downloadBlob = (response: Blob, filename: string) => {
  const blob = response instanceof Blob ? response : new Blob([response], { type: 'text/csv;charset=utf-8' })
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  window.URL.revokeObjectURL(url)
}

export const useInventoryStore = defineStore('inventory', () => {
  const inventories = ref<Inventory[]>([])
  const selectableInventories = ref<Inventory[]>([])
  const alerts = ref<Inventory[]>([])
  const currentInventory = ref<InventoryDetail | null>(null)
  const isLoading = ref(false)
  const actionLoading = ref(false)
  const selectableLoading = ref(false)
  const pagination = ref({
    page: 1,
    size: 5,
    total: 0,
  })
  const lastQuery = ref<InventoryQueryParams>({
    page: 0,
    size: 5,
  })

  const loadInventories = async (params?: InventoryQueryParams) => {
    try {
      isLoading.value = true
      const mergedParams: InventoryQueryParams = {
        ...lastQuery.value,
        ...params,
      }
      const response: PageResult<Inventory> = await inventoryApi.getInventories(buildInventoryQueryParams(mergedParams))

      inventories.value = response.content
      pagination.value = {
        page: response.number + 1,
        size: response.size,
        total: response.totalElements,
      }
      lastQuery.value = mergedParams
      return response
    } finally {
      isLoading.value = false
    }
  }

  const loadSelectableInventories = async () => {
    try {
      selectableLoading.value = true
      const response: PageResult<Inventory> = await inventoryApi.getInventories(buildInventoryQueryParams({
        page: 0,
        size: 1000,
      }))
      selectableInventories.value = response.content
      return response.content
    } finally {
      selectableLoading.value = false
    }
  }

  const loadInventory = async (id: number) => {
    try {
      isLoading.value = true
      const response = await inventoryApi.getInventory(id)
      currentInventory.value = response
      return response
    } finally {
      isLoading.value = false
    }
  }

  const loadAlerts = async (threshold?: number) => {
    const response = await inventoryApi.getStockAlerts(threshold)
    alerts.value = response
    return response
  }

  const stockIn = async (data: InventoryInDTO) => {
    try {
      actionLoading.value = true
      return await inventoryApi.stockIn(data)
    } finally {
      actionLoading.value = false
    }
  }

  const stockOut = async (data: InventoryOutDTO) => {
    try {
      actionLoading.value = true
      return await inventoryApi.stockOut(data)
    } finally {
      actionLoading.value = false
    }
  }

  const adjustStock = async (id: number, data: InventoryAdjustDTO) => {
    try {
      actionLoading.value = true
      return await inventoryApi.adjustStock(id, data)
    } finally {
      actionLoading.value = false
    }
  }

  const exportInventories = async (params?: InventoryQueryParams) => {
    const response = await inventoryApi.exportInventoryReport(buildInventoryQueryParams(params || lastQuery.value))
    downloadBlob(response as Blob, `库存快照_${new Date().toISOString().slice(0, 10)}.csv`)
  }

  const resetCurrentInventory = () => {
    currentInventory.value = null
  }

  return {
    inventories,
    selectableInventories,
    alerts,
    currentInventory,
    isLoading,
    actionLoading,
    selectableLoading,
    pagination,
    lastQuery,
    loadInventories,
    loadSelectableInventories,
    loadInventory,
    loadAlerts,
    stockIn,
    stockOut,
    adjustStock,
    exportInventories,
    resetCurrentInventory,
  }
})
