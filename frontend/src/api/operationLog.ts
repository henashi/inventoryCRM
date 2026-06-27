import request from './request'

export interface OperationLogRecord {
  id: number
  module: string
  operationType: string
  description: string
  requestUrl?: string
  requestMethod?: string
  operator?: string
  ipAddress?: string
  status?: number
  errorMessage?: string
  executionTime?: number
  operationTime: string
}

export interface OperationLogPage {
  content: OperationLogRecord[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  first: boolean
  last: boolean
  empty: boolean
}

export interface OperationLogQuery {
  keyword?: string
  module?: string
  operator?: string
  status?: number
  page?: number
  size?: number
  startTime?: string
  endTime?: string
}

export const operationLogApi = {
  /** 分页搜索系统日志 */
  search: (params?: OperationLogQuery) =>
    request.get<OperationLogPage>('/operation-logs/search', { params }),

  /** 分页搜索（别名，兼容旧引用） */
  searchLogs: (params?: OperationLogQuery) =>
    request.get<OperationLogPage>('/operation-logs/search', { params }),

  /** 根据ID查询详情 */
  getById: (id: number) =>
    request.get<OperationLogRecord>(`/operation-logs/${id}`),

  /** 按模块查询日志 */
  getByModule: (module: string, params?: { page?: number; size?: number }) =>
    request.get<OperationLogPage>(`/operation-logs/module/${module}`, { params }),

  /** 按操作人查询日志 */
  getByOperator: (operator: string, params?: { page?: number; size?: number }) =>
    request.get<OperationLogPage>(`/operation-logs/operator/${operator}`, { params }),
}
