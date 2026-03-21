import {defineStore} from 'pinia'
import {ref} from 'vue'
import {giftLogApi} from '@/api/giftLog'
import type {GiftLogDTO, PageParams} from '@/types'

export const useGiftLogStore = defineStore('giftLog', () => {
  const giftLogList = ref<GiftLogDTO[]>([])
  const pagination = ref({
    page: 1,
    size: 10,
    total: 0
  })

  const loadGiftLogs = async (params: PageParams) => {
    const res = await giftLogApi.loadGiftLogs(params)
    giftLogList.value = res.content
    pagination.value = {
      page: res.number,
      size: res.size,
      total: res.totalElements
    }
  }

  const deleteGiftLog = async (id: number) => {
    await giftLogApi.deleteGiftLog(id)
    await loadGiftLogs({page: pagination.value.page - 1, size: pagination.value.size})
  }

  const createGiftLog = async (giftLog: GiftLogDTO) => {
    await giftLogApi.addGiftLog(giftLog)
    await loadGiftLogs({page: pagination.value.page - 1, size: pagination.value.size})
  }

  const updateGiftLog = async (id: number, giftLog: GiftLogDTO) => {
    await giftLogApi.updateGiftLog(id, giftLog)
    await loadGiftLogs({page: pagination.value.page - 1, size: pagination.value.size})
  }

  const getGiftLogDetail = async (id: number) => {
    return await giftLogApi.getGiftLogDetail(id)
  }
  return {
    giftLogList,
    pagination,
    loadGiftLogs,
    deleteGiftLog,
    createGiftLog,
    updateGiftLog,
    getGiftLogDetail
  }
})
