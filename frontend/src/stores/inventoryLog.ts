// frontend/src/stores/inventoryLog.ts
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { inventoryLogApi } from '@/api/inventoryLog'
import type { InventoryLog, PageParams, PageResult } from '@/types'

const formatDateValue = (value: any) => {
  if (!value) return undefined
  if (typeof value === 'string') return value.slice(0, 10)
  if (typeof value?.format === 'function') return value.format('YYYY-MM-DD')
  if (value instanceof Date) return value.toISOString().split('T')[0]
  return undefined
}

const buildInventoryLogQueryParams = (params?: any) => {
  const queryParams: any = {
    page: params?.page,
    size: params?.size,
    productId: params?.productId,
    type: params?.type,
    operator: params?.operator,
    startTime: params?.startTime,
    endTime: params?.endTime
  }

  if (params?.dateRange && Array.isArray(params.dateRange) && params.dateRange.length === 2) {
    queryParams.startTime = formatDateValue(params.dateRange[0])
    queryParams.endTime = formatDateValue(params.dateRange[1])
  }

  return Object.fromEntries(
    Object.entries(queryParams).filter(([_, value]) => value !== undefined && value !== null && value !== '')
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

export const useInventoryLogStore = defineStore('inventoryLog', () => {
  const logs = ref<InventoryLog[]>([])
  const isLoading = ref(false)
  const pagination = ref({
    page: 1,
    size: 5,
    total: 0
  })
  const stats = ref({
    inCount: 0,
    outCount: 0,
    inQuantity: 0,
    outQuantity: 0
  })

  const loadLogs = async (params?: PageParams) => {
    try {
      isLoading.value = true
      const response: PageResult<InventoryLog> = await inventoryLogApi.getLogs(buildInventoryLogQueryParams(params))

      logs.value = response.content
      pagination.value.total = response.totalElements

      if (params?.page !== undefined) {
        pagination.value.page = params.page + 1
      }
      if (params?.size !== undefined) {
        pagination.value.size = params.size
      }
      await loadStats()
    } finally {
      isLoading.value = false
    }
  }

  const loadStats = async () => {
    const response = await inventoryLogApi.getStats()
    stats.value = response
  }

  const exportLogs = async (params?: any) => {
    const response = await inventoryLogApi.exportLogs(buildInventoryLogQueryParams(params))
    downloadBlob(response as Blob, `库存日志_${new Date().toISOString().slice(0, 10)}.csv`)
  }

  return {
    logs,
    isLoading,
    pagination,
    stats,
    loadLogs,
    exportLogs
  }
})
