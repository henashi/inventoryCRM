import request from './request'
import { mapPageContent, normalizeGift, sanitizeGiftPayload } from './contracts'
import type { Gift, GiftCreateDTO, GiftUpdateDTO, PageResult } from '@/types'

export const giftApi = {
  loadGifts: async (params?: Record<string, unknown>) => {
    const page = await request.get<PageResult<Gift>>('/gifts', { params })
    return mapPageContent(page, normalizeGift)
  },

  getGift: async (id: number) => normalizeGift(await request.get<Gift>(`/gifts/${id}`)),

  createGift: async (data: GiftCreateDTO) =>
    normalizeGift(await request.post<Gift>('/gifts', sanitizeGiftPayload(data))),

  updateGift: async (id: number, data: GiftUpdateDTO) =>
    normalizeGift(await request.put<Gift>(`/gifts/${id}`, sanitizeGiftPayload(data))),

  deleteGift: (id: number) => request.delete(`/gifts/${id}`),
}
