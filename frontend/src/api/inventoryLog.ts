import request from './request'
import { mapPageContent, normalizeInventoryLog } from './contracts'
import type { InventoryLog, InventoryLogStats, PageResult } from '@/types'

export const inventoryLogApi = {
  getLogs: async (params?: Record<string, unknown>) => {
    const page = await request.get<PageResult<InventoryLog>>('/inventory-logs', { params })
    return mapPageContent(page, normalizeInventoryLog)
  },

  getStats: (params?: Record<string, unknown>) =>
    request.get<InventoryLogStats>('/inventory-logs/stats', { params }),

  exportLogs: (params?: Record<string, unknown>) =>
    request.get('/inventory-logs/export', {
      params,
      responseType: 'blob',
    }),

  getLogDetail: async (id: number) => normalizeInventoryLog(
    await request.get<InventoryLog>(`/inventory-logs/${id}`),
  ),
}
