// frontend/src/stores/product.ts
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { productApi } from '@/api/product'
import type { Product, ProductCreateDTO, PageParams, PageResult } from '@/types'

export const useProductStore = defineStore('product', () => {
  // 状态
  const products = ref<Product[]>([])
  const currentProduct = ref<Product | null>(null)
  const isLoading = ref(false)
  const total = ref(0)
  const pagination = ref({
    page: 1,
    size: 10,
    total: 0
  })
  const searchKeyword = ref('')
  const selectedCategory = ref<string>('')

  // Getter
  const totalProducts = computed(() => total.value)
  const lowStockProducts = computed(() =>
    products.value.filter(p => p.currentStock < p.safeStock)
  )
  const outOfStockProducts = computed(() =>
    products.value.filter(p => p.currentStock <= 0)
  )
  const filteredProducts = computed(() => {
    let filtered = products.value

    if (searchKeyword.value) {
      const keyword = searchKeyword.value.toLowerCase()
      filtered = filtered.filter(product =>
        product.name?.toLowerCase().includes(keyword) ||
        product.code?.toLowerCase().includes(keyword) ||
        product.category?.toLowerCase().includes(keyword)
      )
    }

    if (selectedCategory.value) {
      filtered = filtered.filter(p => p.category === selectedCategory.value)
    }

    return filtered
  })
  const categories = computed(() => {
    const cats = new Set(products.value.map(p => p.category).filter(Boolean))
    return Array.from(cats)
  })
  const stockValue = computed(() =>
    products.value.reduce((total, p) => total + (p.currentStock * p.price), 0)
  )

  // Actions
  const loadProducts = async (params?: PageParams) => {
    try {
      isLoading.value = true
      const queryParams = {
        page: params?.page || pagination.value.page - 1,
        size: params?.size || pagination.value.size,
        keyword: params?.keyword ?? (searchKeyword.value || undefined),
        // category: selectedCategory.value || undefined
      }
      const response: PageResult<Product> = await productApi.getProducts(queryParams)

      products.value = response.content
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

  const loadCategories = async () => {
    const categories = await productApi.getCategories()
    return categories
  }

  const getProduct = async (id: number) => {
    try {
      isLoading.value = true
      const product = await productApi.getProduct(id)
      currentProduct.value = product
      return product
    } finally {
      isLoading.value = false
    }
  }

  const addProduct = async (data: ProductCreateDTO) => {
    try {
      isLoading.value = true
      const newProduct = await productApi.createProduct(data)
      products.value.unshift(newProduct)
      total.value += 1
      return newProduct
    } finally {
      isLoading.value = false
    }
  }

  const updateProduct = async (id: number, data: Partial<ProductCreateDTO>) => {
    try {
      isLoading.value = true
      const updatedProduct = await productApi.updateProduct(id, data)

      const index = products.value.findIndex(p => p.id === id)
      if (index !== -1) {
        products.value[index] = { ...products.value[index], ...updatedProduct }
      }

      if (currentProduct.value?.id === id) {
        currentProduct.value = updatedProduct
      }

      return updatedProduct
    } finally {
      isLoading.value = false
    }
  }

  const deleteProduct = async (id: number) => {
    try {
      isLoading.value = true
      await productApi.deleteProduct(id)

      const index = products.value.findIndex(p => p.id === id)
      if (index !== -1) {
        products.value.splice(index, 1)
        total.value -= 1
      }
    } finally {
      isLoading.value = false
    }
  }

  const updateStock = async (id: number, quantity: number, type: 'in' | 'out') => {
    console.log('updateStock', id, quantity, type)
    try {
      isLoading.value = true
      const updatedProduct = await productApi.updateStock(id, quantity, type)

      const index = products.value.findIndex(p => p.id === id)
      if (index !== -1) {
        products.value[index] = updatedProduct
      }

      if (currentProduct.value?.id === id) {
        currentProduct.value = updatedProduct
      }

      return updatedProduct
    } finally {
      isLoading.value = false
    }
  }

  const setSearchKeyword = (keyword: string) => {
    searchKeyword.value = keyword
  }

  const setCategory = (category: string) => {
    selectedCategory.value = category
  }

  const clearCurrentProduct = () => {
    currentProduct.value = null
  }

  const reset = () => {
    products.value = []
    currentProduct.value = null
    searchKeyword.value = ''
    selectedCategory.value = ''
    pagination.value = {
      page: 1,
      size: 10,
      total: 0
    }
  }

  return {
    // State
    products,
    currentProduct,
    isLoading,
    total,
    pagination,
    searchKeyword,
    selectedCategory,

    // Getter
    totalProducts,
    lowStockProducts,
    outOfStockProducts,
    filteredProducts,
    categories,
    stockValue,

    // Actions
    loadProducts,
    getProduct,
    addProduct,
    updateProduct,
    deleteProduct,
    updateStock,
    setSearchKeyword,
    setCategory,
    clearCurrentProduct,
    reset
  }
})
