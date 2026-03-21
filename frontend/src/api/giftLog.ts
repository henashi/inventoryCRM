import request from './request'
import type { GiftLogDTO, PageResult } from '@/types'

export const giftLogApi = {

  // 获取礼品日志列表
  loadGiftLogs: (params: any) => {
    return request.get<PageResult<GiftLogDTO>>(`/gift-logs`, { params })
  },

  // 获取礼品日志详情
  getGiftLogDetail: (id: number) => {
    return request.get<GiftLogDTO>(`/gift-logs/${id}`)
  },

  // 删除礼品日志
  deleteGiftLog: (id: number) => {
    return request.delete(`/gift-logs/${id}`)
  },

  // 新增礼品日志
  addGiftLog: (data: GiftLogDTO) => {
    return request.post(`/gift-logs`, data)
  },

  // 更新礼品日志
  updateGiftLog: (id: number, data: GiftLogDTO) => {
    return request.patch(`/gift-logs/${id}`, data)
  },
}
