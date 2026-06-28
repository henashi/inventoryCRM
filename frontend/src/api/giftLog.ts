import request from './request'
import { mapPageContent, normalizeGiftLog, sanitizeGiftLogPayload } from './contracts'
import type { GiftLogDTO, PageResult } from '@/types'

export const giftLogApi = {
  loadGiftLogs: async (params?: Record<string, unknown>) => {
    const page = await request.get<PageResult<GiftLogDTO>>('/gift-logs', { params })
    return mapPageContent(page, normalizeGiftLog)
  },

  getLogsByCustomerId: async (customerId: number, params?: Record<string, unknown>) => {
    const page = await request.get<PageResult<GiftLogDTO>>(`/gift-logs/customer/${customerId}`, {
      params,
    })
    return mapPageContent(page, normalizeGiftLog)
  },

  getGiftLogDetail: async (id: number) =>
    normalizeGiftLog(await request.get<GiftLogDTO>(`/gift-logs/${id}`)),

  deleteGiftLog: (id: number) => request.delete(`/gift-logs/${id}`),

  addGiftLog: async (data: GiftLogDTO & { limitEnabled?: boolean }) =>
    normalizeGiftLog(await request.post<GiftLogDTO>('/gift-logs', sanitizeGiftLogPayload(data))),

  updateGiftLog: async (id: number, data: GiftLogDTO & { limitEnabled?: boolean }) =>
    normalizeGiftLog(
      await request.patch<GiftLogDTO>(`/gift-logs/${id}`, sanitizeGiftLogPayload(data)),
    ),
}
