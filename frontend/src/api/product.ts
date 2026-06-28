import request from './request'
import { mapPageContent, normalizeProduct } from './contracts'
import type { PageParams, PageResult, Product, ProductCreateDTO, ProductUpdateDTO } from '@/types'

export const productApi = {
  getProducts: async (params?: PageParams) => {
    const page = await request.get<PageResult<Product>>('/products', { params })
    return mapPageContent(page, normalizeProduct)
  },

  getProduct: async (id: number) => normalizeProduct(await request.get<Product>(`/products/${id}`)),

  createProduct: async (data: ProductCreateDTO) =>
    normalizeProduct(await request.post<Product>('/products', data)),

  updateProduct: async (id: number, data: Partial<ProductUpdateDTO>) =>
    normalizeProduct(await request.patch<Product>(`/products/${id}`, data)),

  deleteProduct: (id: number) => request.delete(`/products/${id}`),

  searchProducts: (keyword: string) =>
    request.get<Product[]>('/products/search', {
      params: { keyword },
    }),

  updateStock: async (id: number, quantity: number, type: 'IN' | 'OUT') =>
    normalizeProduct(await request.patch<Product>(`/products/${id}/stock`, { quantity, type })),

  exportProducts: (params?: Record<string, unknown>) =>
    request.get('/products/export', {
      params,
      responseType: 'blob',
    }),

  getImportTemplate: () => request.get('/products/import/template'),

  importProducts: (file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return request.post('/products/import', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    })
  },

  getStockStatistics: () => request.get('/products/stock/statistics'),

  getCategories: () => request.get<string[]>('/products/categories'),

  getLowStockProducts: async (threshold?: number) => {
    const products = await request.get<Product[]>('/products/low-stock', {
      params: { threshold },
    })
    return products.map(normalizeProduct)
  },
}
