import request from './request'
import type { CustomerScore, GiftRecommendation, StockPrediction, StockAlert, PredictionSummary, AiRunResult } from '@/types'

export const aiApi = {
  /** 查询所有商品预测（分页） */
  getPredictions: (params?: { page?: number; size?: number; keyword?: string }) =>
    request.get<{ content: StockPrediction[]; totalElements: number; totalPages: number; number: number; size: number }>('/ai/predictions', { params }),

  /** 查询单个商品预测详情 */
  getPredictionByProduct: (productId: number) =>
    request.get<StockPrediction>(`/ai/predictions/${productId}`),

  /** 查询预测概览统计 */
  getPredictionSummary: () =>
    request.get<PredictionSummary>('/ai/predictions/summary'),

  /** 查询预警列表 */
  getAlerts: (level?: 'DANGER' | 'WARNING' | 'ALL') =>
    request.get<StockAlert[]>('/ai/alerts', { params: { level: level || 'ALL' } }),

  /** 手动触发全量预测 */
  runPrediction: () =>
    request.post<AiRunResult>('/ai/predictions/run'),

  // ===== 客户评分 & 礼品推荐 =====

  /** 查询客户评分列表（分页） */
  getCustomerScores: (params?: { page?: number; size?: number }) =>
    request.get<{ content: CustomerScore[]; totalElements: number; totalPages: number }>('/ai/customers/scores', { params }),

  /** 查询单个客户评分详情 */
  getCustomerScore: (customerId: number) =>
    request.get<CustomerScore>(`/ai/customers/${customerId}/score`),

  /** 查询客户礼品推荐 */
  getRecommendations: (customerId: number) =>
    request.get<GiftRecommendation[]>(`/ai/customers/${customerId}/recommendations`),

  /** 查询未来7天生日的客户 */
  getUpcomingBirthdayCustomers: () =>
    request.get<any[]>('/ai/customers/birthday-upcoming'),

  /** 手动触发全量客户评分 */
  runCustomerScoring: () =>
    request.post<{ success: boolean; executionTimeMs: number }>('/ai/customers/run-scoring'),

  // ===== AI 聊天助手 =====

  /** 发送消息给 AI 助手 */
  chat: (message: string) =>
    request.post<{ reply: string; fallback: boolean }>('/ai/chat', { message }),
}
