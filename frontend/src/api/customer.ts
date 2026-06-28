import request from './request'
import { mapPageContent, normalizeCustomer } from './contracts'
import type {
  Customer,
  CustomerCreateDTO,
  CustomerUpdateDTO,
  PageParams,
  PageResult,
} from '@/types'

export const customerApi = {
  getCustomers: async (params?: PageParams) => {
    const page = await request.get<PageResult<Customer>>('/customers', { params })
    return mapPageContent(page, normalizeCustomer)
  },

  getCustomer: async (id: number) =>
    normalizeCustomer(await request.get<Customer>(`/customers/${id}`)),

  createCustomer: async (data: CustomerCreateDTO) =>
    normalizeCustomer(await request.post<Customer>('/customers', data)),

  updateCustomer: async (id: number, data: Partial<CustomerUpdateDTO>) =>
    normalizeCustomer(await request.patch<Customer>(`/customers/${id}`, data)),

  deleteCustomer: (id: number) => request.delete(`/customers/${id}`),

  searchCustomers: (keyword: string) =>
    request.get<Customer[]>('/customers/search', {
      params: { keyword },
    }),

  exportCustomers: (params?: Record<string, unknown>) =>
    request.get('/customers/export', {
      params,
      responseType: 'blob',
    }),

  getImportTemplate: () => request.get('/customers/import/template'),

  importCustomers: (file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return request.post('/customers/import', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    })
  },

  getStatistics: () => request.get('/customers/statistics'),

  batchUpdateStatus: (ids: number[], status: 0 | 1) =>
    request.put('/customers/batch/status', { ids, status }),
}
