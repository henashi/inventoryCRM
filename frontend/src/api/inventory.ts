import request from './request'
import type {
  Inventory,
  InventoryAdjustDTO,
  InventoryChange,
  InventoryDetail,
  InventoryInDTO,
  InventoryOutDTO,
  InventoryQueryParams,
  PageResult,
} from '@/types'

export const inventoryApi = {
  getInventories: (params?: InventoryQueryParams) =>
    request.get<PageResult<Inventory>>('/inventories', { params }),

  getInventory: (id: number) =>
    request.get<InventoryDetail>(`/inventories/${id}`),

  stockIn: (data: InventoryInDTO) =>
    request.post<Inventory>('/inventories/in', data),

  stockOut: (data: InventoryOutDTO) =>
    request.post<Inventory>('/inventories/out', data),

  adjustStock: (id: number, data: InventoryAdjustDTO) =>
    request.patch<Inventory>(`/inventories/${id}/adjust`, data),

  getChangeHistory: (params?: InventoryQueryParams) =>
    request.get<PageResult<InventoryChange>>('/inventories/history', { params }),

  getStockAlerts: (threshold?: number) =>
    request.get<Inventory[]>('/inventories/alerts', { params: { threshold } }),

  exportInventoryReport: (params?: InventoryQueryParams) =>
    request.get('/inventories/export', {
      params,
      responseType: 'blob'
    })
}
