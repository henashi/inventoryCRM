// frontend/src/stores/customer.ts
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { customerApi } from '@/api/customer'
import type { Customer, CustomerCreateDTO, CustomerUpdateDTO, PageParams } from '@/types'

type DateLike = string | Date | { format: (fmt: string) => string } | null | undefined

const formatDateValue = (value: DateLike): string | undefined => {
  if (!value) return undefined
  if (typeof value === 'string') return value.slice(0, 10)
  if (typeof value === 'object' && 'format' in value) return (value as { format: (fmt: string) => string }).format('YYYY-MM-DD')
  if (value instanceof Date) return value.toISOString().split('T')[0]
  return undefined
}

const buildCustomerQueryParams = (params?: PageParams) => {
  const queryParams: Record<string, unknown> = {
    page: params?.page,
    size: params?.size,
    keyword: params?.keyword,
    giftLevel: params?.giftLevel,
    status: params?.status,
    gender: params?.gender,
    sort: params?.sort,
    direction: params?.direction,
    startDate: params?.startDate,
    endDate: params?.endDate,
  }

  if (params?.dateRange && Array.isArray(params.dateRange) && params.dateRange.length === 2) {
    queryParams.startDate = formatDateValue(params.dateRange[0])
    queryParams.endDate = formatDateValue(params.dateRange[1])
  }

  return Object.fromEntries(
    Object.entries(queryParams).filter(
      ([_, value]) => value !== undefined && value !== null && value !== '',
    ),
  )
}

export const useCustomerStore = defineStore('customer', () => {
  const customers = ref<Customer[]>([])
  const currentCustomer = ref<Customer | null>(null)
  const isLoading = ref(false)
  const total = ref(0)
  const pagination = ref({
    page: 1,
    size: 5,
    total: 0,
  })
  const searchKeyword = ref('')

  const totalCustomers = computed(() => total.value)
  const activeCustomers = computed(() => customers.value.filter((c) => c.status === 1).length)
  const filteredCustomers = computed(() => {
    if (!searchKeyword.value) return customers.value

    const keyword = searchKeyword.value.toLowerCase()
    return customers.value.filter(
      (customer) =>
        customer.name?.toLowerCase().includes(keyword) ||
        customer.phone?.includes(keyword) ||
        customer.email?.toLowerCase().includes(keyword),
    )
  })
  const customerOptions = computed(() =>
    customers.value.map((c) => ({
      label: `${c.name} (${c.phone})`,
      value: c.id,
    })),
  )

  const loadCustomers = async (params?: PageParams) => {
    try {
      isLoading.value = true
      const queryParams = buildCustomerQueryParams({
        page: params?.page ?? pagination.value.page - 1,
        size: params?.size ?? pagination.value.size,
        keyword: params?.keyword ?? (searchKeyword.value || undefined),
        giftLevel: params?.giftLevel,
        status: params?.status,
        gender: params?.gender,
        sort: params?.sort,
        direction: params?.direction,
        startDate: params?.startDate,
        endDate: params?.endDate,
        dateRange: params?.dateRange,
      })

      if (params?.keyword !== undefined) {
        searchKeyword.value = params.keyword
      }

      const response = await customerApi.getCustomers(queryParams)

      customers.value = response.content
      total.value = response.totalElements
      pagination.value.total = response.totalElements

      if (params?.page !== undefined) {
        pagination.value.page = params.page + 1
      }
      if (params?.size !== undefined) {
        pagination.value.size = params.size
      }
    } finally {
      isLoading.value = false
    }
  }

  const getCustomer = async (id: number) => {
    try {
      isLoading.value = true
      const customer = await customerApi.getCustomer(id)
      currentCustomer.value = customer
      return customer
    } finally {
      isLoading.value = false
    }
  }

  const findCustomerById = async (id: number) => {
    try {
      isLoading.value = true
      const customer = await customerApi.getCustomer(id)
      return customer
    } finally {
      isLoading.value = false
    }
  }

  const addCustomer = async (data: CustomerCreateDTO) => {
    try {
      isLoading.value = true
      const newCustomer = await customerApi.createCustomer(data)
      customers.value.unshift(newCustomer)
      total.value += 1
      return newCustomer
    } finally {
      isLoading.value = false
    }
  }

  const updateCustomer = async (id: number, data: Partial<CustomerUpdateDTO>) => {
    try {
      isLoading.value = true
      const updatedCustomer = await customerApi.updateCustomer(id, data)

      const index = customers.value.findIndex((c) => c.id === id)
      if (index !== -1) {
        customers.value[index] = { ...customers.value[index], ...updatedCustomer }
      }

      if (currentCustomer.value?.id === id) {
        currentCustomer.value = { ...currentCustomer.value, ...updatedCustomer }
      }

      return updatedCustomer
    } finally {
      isLoading.value = false
    }
  }

  const deleteCustomer = async (id: number) => {
    try {
      isLoading.value = true
      await customerApi.deleteCustomer(id)

      const index = customers.value.findIndex((c) => c.id === id)
      if (index !== -1) {
        customers.value.splice(index, 1)
        total.value -= 1
      }
    } finally {
      isLoading.value = false
    }
  }

  const exportCustomers = async (params?: PageParams) => {
    return customerApi.exportCustomers(buildCustomerQueryParams(params))
  }

  const batchUpdateStatus = async (ids: number[], status: 0 | 1) => {
    await customerApi.batchUpdateStatus(ids, status)
    await loadCustomers()
  }

  const setSearchKeyword = (keyword: string) => {
    searchKeyword.value = keyword
  }

  const clearCurrentCustomer = () => {
    currentCustomer.value = null
  }

  const reset = () => {
    customers.value = []
    currentCustomer.value = null
    searchKeyword.value = ''
    pagination.value = {
      page: 1,
      size: 5,
      total: 0,
    }
  }

  return {
    customers,
    currentCustomer,
    isLoading,
    total,
    pagination,
    searchKeyword,
    totalCustomers,
    activeCustomers,
    filteredCustomers,
    customerOptions,
    loadCustomers,
    getCustomer,
    findCustomerById,
    addCustomer,
    updateCustomer,
    deleteCustomer,
    exportCustomers,
    batchUpdateStatus,
    setSearchKeyword,
    clearCurrentCustomer,
    reset,
  }
})
