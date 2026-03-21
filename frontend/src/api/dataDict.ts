import request from './request'
import type { DataDict, DataDictCreateDTO, DataDictUpdateDTO, PageResult } from '@/types'

export const dataDictApi = {

  // 获取配置列表
  loadDataDicts: (params?: any) => {
    console.log('API加载配置数据，参数:', params)
    return request.get<PageResult<DataDict>>('/data-dict', { params })
  },

  // 获取配置详情
  getDataDict: (id: number) => request.get<DataDict>(`/data-dict/${id}`),

  // 新增配置
  createDataDict: (data: DataDictCreateDTO) => request.post('/data-dict', data),

  // 更新配置
  updateDataDict: (id: number, data: DataDictUpdateDTO) => request.put(`/data-dict/${id}`, data),

  // 删除配置
  deleteDataDict: (id: number) => request.delete(`/data-dict/${id}`),

}
