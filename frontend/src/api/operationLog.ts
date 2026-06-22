import request from './request'

type OperationLogRecord = {
  id: number
  module: string
  operationType: string
  description: string
  requestUrl?: string
  requestMethod?: string
  operator?: string
  ipAddress?: string
  status: number
  errorMessage?: string
  executionTime?: number
  operationTime?: string
}

type OperationLogPage = {
  content: OperationLogRecord[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  first: boolean
  last: boolean
  empty: boolean
}

export const operationLogApi = {
  searchLogs: (params?: Record<string, unknown>) => request.get<OperationLogPage>('/operation-logs/search', { params }),
}

export type { OperationLogRecord, OperationLogPage }
