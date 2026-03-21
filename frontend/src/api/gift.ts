import request from './request'
import type { Gift, GiftCreateDTO, GiftUpdateDTO, PageResult } from '@/types'

export const giftApi = {

  // 获取礼品列表
  loadGifts: (params?: any) => {
    console.log('API加载礼品数据，参数:', params)
    return request.get<PageResult<Gift>>('/gifts', { params })
  },

  // 获取礼品详情
  getGift: (id: number) => request.get<Gift>(`/gifts/${id}`),

  // 新增礼品
  createGift: (data: GiftCreateDTO) => request.post('/gifts', data),

  // 更新礼品
  updateGift: (id: number, data: GiftUpdateDTO) => request.put(`/gifts/${id}`, data),

  // 删除礼品
  deleteGift: (id: number) => request.delete(`/gifts/${id}`),

}
