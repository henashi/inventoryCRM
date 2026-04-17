import {defineStore} from 'pinia'
import {ref} from 'vue'
import {dataDictApi} from '@/api/dataDict'
import type {DataDict, DataDictCreateDTO, DataDictUpdateDTO, PageParams} from '@/types'

export const useDataDictStore = defineStore('data-dict', () => {
  const dataDicts = ref<DataDict[]>([])

  const pagination = ref({
    page: 0,
    size: 10,
    total: 0
  })

  const loadDataDicts = async (params: PageParams) => {
    const res = await dataDictApi.loadDataDicts(params)
    dataDicts.value = res.content
    pagination.value = {
      page: res.number,
      size: res.size,
      total: res.totalElements
    }
  }

  const createDataDict = async (dataDict: DataDictCreateDTO) => {
    await dataDictApi.createDataDict(dataDict)
  }

  const updateDataDict = async (id: number, dataDict: DataDictUpdateDTO) => {
    await dataDictApi.updateDataDict(id, dataDict)
  }

  const updateDataDictStatus = async (id: number, enable: boolean) => {
    await dataDictApi.updateDataDictStatus(id, enable)
  }

  const deleteDataDict = async (id: number) => {
    await dataDictApi.deleteDataDict(id)
  }

  const getDataDict = async (id: number) => {
    return await dataDictApi.getDataDict(id)
  }

  return {
    dataDicts,
    pagination,
    loadDataDicts,
    createDataDict,
    updateDataDict,
    updateDataDictStatus,
    deleteDataDict,
    getDataDict
  }
})
