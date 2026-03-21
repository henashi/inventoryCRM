// frontend/src/stores/customer.ts
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { customerApi } from '@/api/customer'
import type { Customer, CustomerCreateDTO, PageParams, PageResult } from '@/types'

export const useCustomerStore = defineStore('customer', () => {
  // 状态
  const customers = ref<Customer[]>([])
  const currentCustomer = ref<Customer | null>(null)
  const isLoading = ref(false)
  const total = ref(0)
  const pagination = ref({
    page: 1,
    size: 10,
    total: 0
  })
  const searchKeyword = ref('')

  // Getter
  const totalCustomers = computed(() => total.value)
  const activeCustomers = computed(() =>
    customers.value.filter(c => c.status === 1).length
  )
  const filteredCustomers = computed(() => {
    if (!searchKeyword.value) return customers.value

    const keyword = searchKeyword.value.toLowerCase()
    return customers.value.filter(customer =>
      customer.name?.toLowerCase().includes(keyword) ||
      customer.phone?.includes(keyword) ||
      customer.email?.toLowerCase().includes(keyword)
    )
  })
  const customerOptions = computed(() =>
    customers.value.map(c => ({
      label: `${c.name} (${c.phone})`,
      value: c.id
    }))
  )

  // Actions
  const loadCustomers = async (params?: PageParams) => {
    try {
      isLoading.value = true
      console.log('params', params)
      // Build query params: prefer explicit params passed in, fallback to store state
      const queryParams: any = {
        page: params?.page ?? (pagination.value.page - 1),
        size: params?.size ?? pagination.value.size,
        keyword: params?.keyword ?? (searchKeyword.value || undefined),
        giftLevel: params?.giftLevel,
        status: params?.status,
        gender: params?.gender,
        sort: params?.sort,
        direction: params?.direction
      }

      // Support passing date range as an array [start, end]
      if (params?.dateRange && Array.isArray(params.dateRange) && params.dateRange.length === 2) {
        queryParams.startDate = params.dateRange[0].toISOString().split('T')[0];
        queryParams.endDate = params.dateRange[1].toISOString().split('T')[0];
      }

      // If caller provided a keyword param, keep it in store so filteredCustomers and future calls can reuse
      if (params?.keyword !== undefined) {
        searchKeyword.value = params.keyword
      }
      console.log('queryParams', queryParams)

      const response: PageResult<Customer> = await customerApi.getCustomers(queryParams)

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

  const updateCustomer = async (id: number, data: Partial<CustomerCreateDTO>) => {
    try {
      isLoading.value = true
      const updatedCustomer = await customerApi.updateCustomer(id, data)

      const index = customers.value.findIndex(c => c.id === id)
      if (index !== -1) {
        customers.value[index] = { ...customers.value[index], ...updatedCustomer }
      }

      if (currentCustomer.value?.id === id) {
        currentCustomer.value = updatedCustomer
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

      const index = customers.value.findIndex(c => c.id === id)
      if (index !== -1) {
        customers.value.splice(index, 1)
        total.value -= 1
      }
    } finally {
      isLoading.value = false
    }
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
      size: 10,
      total: 0
    }
  }

  return {
    // State
    customers,
    currentCustomer,
    isLoading,
    total,
    pagination,
    searchKeyword,

    // Getter
    totalCustomers,
    activeCustomers,
    filteredCustomers,
    customerOptions,

    // Actions
    loadCustomers,
    getCustomer,
    findCustomerById,
    addCustomer,
    updateCustomer,
    deleteCustomer,
    setSearchKeyword,
    clearCurrentCustomer,
    reset
  }
})
