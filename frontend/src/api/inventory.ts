// frontend/src/api/inventory.ts
import request from './request'
import type { Inventory, InventoryChange, PageParams, PageResult } from '@/types'

export const inventoryApi = {
  // 获取库存列表
  getInventories: (params?: PageParams) => 
    request.get<PageResult<Inventory>>('/inventories', { params }),
  
  // 获取库存详情
  getInventory: (id: number) => 
    request.get<Inventory>(`/inventories/${id}`),
  
  // 商品入库
  stockIn: (data: {
    productId: number
    quantity: number
    warehouseId?: number
    remark?: string
  }) => request.post<Inventory>('/inventories/in', data),
  
  // 商品出库
  stockOut: (data: {
    productId: number
    quantity: number
    warehouseId?: number
    remark?: string
  }) => request.post<Inventory>('/inventories/out', data),
  
  // 库存调整
  adjustStock: (id: number, data: {
    actualQuantity: number
    reason: string
    remark?: string
  }) => request.patch<Inventory>(`/inventories/${id}/adjust`, data),
  
  // 获取库存变更记录
  getChangeHistory: (params?: PageParams) => 
    request.get<PageResult<InventoryChange>>('/inventories/history', { params }),
  
  // 获取库存预警
  getStockAlerts: (threshold?: number) => 
    request.get<Inventory[]>('/inventories/alerts', { params: { threshold } }),
  
  // 导出库存报表
  exportInventoryReport: (params?: any) => 
    request.get('/inventories/export', { 
      params,
      responseType: 'blob'
    })
}