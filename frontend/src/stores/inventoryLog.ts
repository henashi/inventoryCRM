// frontend/src/stores/inventoryLog.ts
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { inventoryLogApi } from '@/api/inventoryLog'
import type { InventoryLog, PageParams, PageResult } from '@/types'

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

  // 加载日志列表
  const loadLogs = async (params?: PageParams) => {
    try {
      isLoading.value = true
      const response: PageResult<InventoryLog> = await inventoryLogApi.getLogs(params)

      logs.value = response.content
      pagination.value.total = response.totalElements

      if (params?.page !== undefined) {
        pagination.value.page = params.page + 1
      }
      if (params?.size !== undefined) {
        pagination.value.size = params.size
      }
      // 加载统计信息
      await loadStats()

    } finally {
      isLoading.value = false
    }
  }

  // 加载统计信息
  const loadStats = async () => {
    const response = await inventoryLogApi.getStats()
    stats.value = response
  }

  // 导出日志
  const exportLogs = async (params?: any) => {
    const response = await inventoryLogApi.exportLogs(params)
    const blob = new Blob([response], { type: 'application/vnd.ms-excel' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `库存日志_${new Date().toISOString().slice(0, 10)}.xlsx`
    link.click()
    window.URL.revokeObjectURL(url)
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
