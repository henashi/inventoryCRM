// frontend/src/api/customer.ts
import request from './request'
import type {
  Customer,
  CustomerCreateDTO,
  CustomerUpdateDTO,
  PageParams,
  PageResult
} from '@/types'

export const customerApi = {
  // 获取客户列表
  getCustomers: (params?: PageParams) =>
    request.get<PageResult<Customer>>('/customers', { params }),

  // 获取客户详情
  getCustomer: (id: number) =>
    request.get<Customer>(`/customers/${id}`),

  // 创建客户
  createCustomer: (data: CustomerCreateDTO) =>
    request.post<Customer>('/customers', data),

  // 更新客户
  updateCustomer: (id: number, data: Partial<CustomerUpdateDTO>) =>
    request.patch<Customer>(`/customers/${id}`, data),

  // 删除客户
  deleteCustomer: (id: number) =>
    request.delete(`/customers/${id}`),

  // 搜索客户
  searchCustomers: (keyword: string) =>
    request.get<Customer[]>('/customers/search', {
      params: { keyword }
    }),

  // 导出客户
  exportCustomers: (params?: any) =>
    request.get('/customers/export', {
      params,
      responseType: 'blob'
    }),

  // 导入客户
  importCustomers: (file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return request.post('/customers/import', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  },

  // 获取客户统计数据
  getStatistics: () =>
    request.get('/customers/statistics'),

  // 批量更新客户状态
  batchUpdateStatus: (ids: number[], status: 0 | 1) =>
    request.put('/customers/batch/status', { ids, status })
}
