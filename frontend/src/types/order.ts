// 消费记录
export interface OrderItem {
  id: number
  customerId: number
  customerName: string
  productId: number | null
  productName: string
  quantity: number
  unitPrice: number | null
  totalAmount: number
  orderTime: string
  remark: string
  createdTime: string
}

export interface OrderItemCreateDTO {
  customerId: number
  productId?: number
  productName?: string
  quantity: number
  unitPrice?: number
  totalAmount: number
  remark?: string
}
