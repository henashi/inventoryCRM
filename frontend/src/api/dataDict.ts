import request from './request'
import { mapPageContent, normalizeDataDict, sanitizeDataDictPayload } from './contracts'
import type { DataDict, DataDictCreateDTO, DataDictUpdateDTO, PageResult } from '@/types'

export const dataDictApi = {
  loadDataDicts: async (params?: Record<string, unknown>) => {
    const page = await request.get<PageResult<DataDict>>('/data-dict', { params })
    return mapPageContent(page, normalizeDataDict)
  },

  getDataDict: async (id: number) => normalizeDataDict(
    await request.get<DataDict>(`/data-dict/${id}`),
  ),

  createDataDict: (data: DataDictCreateDTO) => request.post('/data-dict', sanitizeDataDictPayload(data)),

  updateDataDict: (id: number, data: DataDictUpdateDTO) => request.patch(`/data-dict/${id}`, sanitizeDataDictPayload(data)),

  updateDataDictStatus: (id: number, enable: boolean) => request.patch(`/data-dict/status/${id}/${enable}`),

  deleteDataDict: (id: number) => request.delete(`/data-dict/${id}`),
}
