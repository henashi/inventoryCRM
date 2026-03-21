import request from './request'
import type { InventoryLog,  PageParams, PageResult } from '@/types'

export const inventoryLogApi = {
  // 获取日志列表
  getLogs: (params?: any) =>
    request.get<PageResult<InventoryLog>>('/inventory-logs', { params }),

  // 获取统计信息
  getStats: (params?: any) =>
    request.get<any>('/inventory-logs/stats', { params }),

  // 导出日志
  exportLogs: (params?: any) =>
    request.get('/inventory-logs/export', {
      params,
      responseType: 'blob'
    }),

  // 获取日志详情
  getLogDetail: (id: number) =>
    request.get<InventoryLog>(`/inventory-logs/${id}`)
}
