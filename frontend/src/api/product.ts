// frontend/src/api/product.ts
import request from './request'
import type {
  Product,
  ProductCreateDTO,
  ProductUpdateDTO,
  PageParams,
  PageResult
} from '@/types'

export const productApi = {
  // 获取商品列表
  getProducts: (params?: PageParams) =>
    request.get<PageResult<Product>>('/products', { params }),

  // 获取商品详情
  getProduct: (id: number) =>
    request.get<Product>(`/products/${id}`),

  // 创建商品
  createProduct: (data: ProductCreateDTO) =>
    request.post<Product>('/products', data),

  // 更新商品
  updateProduct: (id: number, data: Partial<ProductUpdateDTO>) =>
    request.put<Product>(`/products/${id}`, data),

  // 删除商品
  deleteProduct: (id: number) =>
    request.delete(`/products/${id}`),

  // 搜索商品
  searchProducts: (keyword: string) =>
    request.get<Product[]>('/products/search', {
      params: { keyword }
    }),

  // 更新库存
  updateStock: (id: number, quantity: number, type: 'in' | 'out') =>
    request.patch<Product>(`/products/${id}/stock`, { quantity, type }),

  // 导出商品
  exportProducts: (params?: any) =>
    request.get('/products/export', {
      params,
      responseType: 'blob'
    }),

  // 导入商品
  importProducts: (file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return request.post('/products/import', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  },

  // 获取库存统计
  getStockStatistics: () =>
    request.get('/products/stock/statistics'),

  // 获取商品分类
  getCategories: () =>
    request.get<string[]>('/products/categories'),

  // 获取低库存商品
  getLowStockProducts: (threshold?: number) =>
    request.get<Product[]>('/products/low-stock', {
      params: { threshold }
    })
}
