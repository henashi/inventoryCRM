import request from './request'
import type { OrderDTO, OrderCreateDTO, PageResult } from '@/types'

export const orderApi = {
  list: (params?: { page?: number; size?: number }) =>
    request.get<PageResult<OrderDTO>>('/orders', { params }),

  getById: (id: number) => request.get<OrderDTO>(`/orders/${id}`),

  create: (data: OrderCreateDTO) => request.post<OrderDTO>('/orders', data),

  delete: (id: number) => request.delete(`/orders/${id}`),
}
