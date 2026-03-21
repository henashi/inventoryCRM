import {defineStore} from 'pinia'
import {ref} from 'vue'
import {giftApi} from '@/api/gift'
import type {Gift, GiftCreateDTO, GiftUpdateDTO, PageParams} from '@/types'

export const useGiftStore = defineStore('gift', () => {
  const gifts = ref<Gift[]>([])

  const pagination = ref({
    page: 1,
    size: 10,
    total: 0
  })

  const loadGifts = async (params: PageParams) => {
    const res = await giftApi.loadGifts(params)
    gifts.value = res.content
    pagination.value = {
      page: res.number,
      size: res.size,
      total: res.totalElements
    }
  }

  const createGift = async (gift: GiftCreateDTO) => {
    await giftApi.createGift(gift)
    await loadGifts({page: pagination.value.page - 1, size: pagination.value.size})
  }

  const updateGift = async (id: number, gift: GiftUpdateDTO) => {
    await giftApi.updateGift(id, gift)
    await loadGifts({page: pagination.value.page - 1, size: pagination.value.size})
  }

  const deleteGift = async (id: number) => {
    await giftApi.deleteGift(id)
    await loadGifts({page: pagination.value.page - 1, size: pagination.value.size})
  }

  const getGift = async (id: number) => {
    return await giftApi.getGift(id)
  }

  return {
    gifts,
    pagination,
    loadGifts,
    createGift,
    updateGift,
    deleteGift,
    getGift
  }
})
