import { defineStore } from 'pinia'
import { ref } from 'vue'
import { giftLogApi } from '@/api/giftLog'
import type { GiftLogDTO, PageParams } from '@/types'
import { toUiPage } from '@/utils/pagination'

export const useGiftLogStore = defineStore('giftLog', () => {
  const giftLogList = ref<GiftLogDTO[]>([])
  const pagination = ref({
    page: 1,
    size: 5,
    total: 0,
  })

  const loadGiftLogs = async (params: PageParams & { customerId?: number }) => {
    const { customerId, ...pageParams } = params
    const res = customerId
      ? await giftLogApi.getLogsByCustomerId(customerId, pageParams)
      : await giftLogApi.loadGiftLogs(pageParams)

    giftLogList.value = res.content
    pagination.value = {
      page: toUiPage(res.number),
      size: res.size,
      total: res.totalElements,
    }
  }

  const deleteGiftLog = async (id: number) => {
    await giftLogApi.deleteGiftLog(id)
  }

  const createGiftLog = async (giftLog: GiftLogDTO) => {
    await giftLogApi.addGiftLog(giftLog)
  }

  const updateGiftLog = async (id: number, giftLog: GiftLogDTO) => {
    await giftLogApi.updateGiftLog(id, giftLog)
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
    getGiftLogDetail,
  }
})
