// frontend/src/types/index.ts

// 基础分页参数
export interface PageParams {
  page?: number
  size?: number
  sort?: string
  direction?: 'asc' | 'desc'
  keyword?: string
  [key: string]: any
}

// 分页结果
export interface PageResult<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  first: boolean
  last: boolean
  empty: boolean
}

// 客户相关类型
export interface Customer {
  id?: number
  name: string
  phone: string
  email?: string
  birthday?: string
  gender?: 0 | 1  // 0: 女, 1: 男
  address?: string
  giftLevel: 0 | 1 | 2 | 3  // 礼品等级
  status: 0 | 1  // 0: 停用, 1: 正常
  remark?: string
  referrerId?: number
  createdAt?: string
  updatedAt?: string
}

export interface CustomerCreateDTO {
  name: string
  phone: string
  email?: string
  birthday?: string
  gender?: 0 | 1
  address?: string
  giftLevel?: 0 | 1 | 2 | 3
  remark?: string
  referrerPhone?: string
  referrerId?: number
}

export interface CustomerUpdateDTO extends Partial<CustomerCreateDTO> {
  status?: 0 | 1
  birthday?: string
  gender?: 0 | 1
}

// 商品相关类型
export interface Product {
  id?: number
  name: string
  code: string
  // category: string
  brand?: string
  unit: string
  price: number
  cost: number
  currentStock: number
  safeStock: number
  maxStock: number
  status: 0 | 1  // 0: 停售, 1: 在售
  description?: string
  imageUrl?: string
  createdAt?: string
  updatedAt?: string
}

export interface ProductCreateDTO {
  name: string
  code: string
  // category: string
  brand?: string
  unit: string
  price: number
  cost?: number
  currentStock?: number
  safeStock?: number
  maxStock?: number
  description?: string
  imageUrl?: string
}

export interface ProductUpdateDTO extends Partial<ProductCreateDTO> {
  status?: 0 | 1
}

// 库存变更记录
export interface StockChange {
  id?: number
  productId: number
  productName: string
  changeType: 'in' | 'out' | 'adjust'  // 入库/出库/调整
  quantity: number
  beforeStock: number
  afterStock: number
  reason: string
  operator?: string
  remark?: string
  createdAt?: string
}
// 库存相关类型
export interface Inventory {
  id?: number
  productId: number
  productName: string
  warehouseId?: number
  warehouseName?: string
  currentStock: number
  safeStock: number
  maxStock: number
  unit: string
  status: 0 | 1  // 0: 停用, 1: 正常
  lastUpdateTime?: string
  createdAt?: string
  updatedAt?: string
}

export interface InventoryChange {
  id?: number
  inventoryId: number
  productId: number
  productName: string
  changeType: 'in' | 'out' | 'adjust' | 'transfer'  // 入库/出库/调整/调拨
  changeQuantity: number
  beforeQuantity: number
  afterQuantity: number
  reason: string
  operator?: string
  remark?: string
  createdAt?: string
}

export interface InventoryInDTO {
  productId: number
  quantity: number
  warehouseId?: number
  batchNo?: string
  purchasePrice?: number
  supplier?: string
  remark?: string
}

export interface InventoryOutDTO {
  productId: number
  quantity: number
  warehouseId?: number
  customerId?: number
  orderNo?: string
  reason: string
  remark?: string
}

export interface InventoryAdjustDTO {
  actualQuantity: number
  reason: string
  remark?: string
}

// 库存查询参数
export interface InventoryQueryParams extends PageParams {
  productId?: number
  warehouseId?: number
  minStock?: number
  maxStock?: number
  lowStockOnly?: boolean
}

/**
 * 库存操作类型枚举
 */
export enum InventoryLogType {
  CREATE = 'CREATE',      // 新建商品
  IN = 'IN',              // 入库
  OUT = 'OUT',            // 出库
  ADJUST = 'ADJUST',      // 调整
  TRANSFER = 'TRANSFER',  // 调拨
  CHECK = 'CHECK'         // 盘点
}

/**
 * 库存操作来源枚举
 */
export enum InventoryLogSource {
  WEB = 'WEB',            // Web界面
  API = 'API',             // API接口
  INTERNAL = 'INTERNAL',   // 内部调用
  IMPORT = 'IMPORT',       // 批量导入
  SYSTEM = 'SYSTEM'       // 系统自动
}

/**
 * 库存日志记录接口
 */
export interface InventoryLog {
  id: number
  productId: number
  productName: string
  productCode: string
  productUnit: string
  logType: InventoryLogType          // 操作类型
  beforeStock: number
  afterStock: number
  quantity: number
  operator: string
  logTime: string                    // 操作时间
  reason?: string
  success: boolean
  errorMessage?: string
  source?: InventoryLogSource         // 操作来源
  referenceNo?: string               // 关联单号
  warehouseId?: number               // 仓库ID
  warehouseName?: string             // 仓库名称
  batchNo?: string                  // 批次号
  expirationDate?: string           // 有效期
  location?: string                 // 库位
  createdAt: string
  updatedAt: string

  // 扩展信息（可选）
  clientIp?: string                 // 客户端IP
  userAgent?: string                // 用户代理
  requestId?: string                // 请求ID
  costTime?: number                 // 操作耗时（毫秒）
}

/**
 * 库存日志查询参数接口
 */
export interface InventoryLogQueryParams extends PageParams {
  productId?: number
  productName?: string
  productCode?: string
  category?: string
  logType?: InventoryLogType
  operator?: string
  startTime?: string
  endTime?: string
  success?: boolean
  minQuantity?: number
  maxQuantity?: number
  source?: InventoryLogSource
  warehouseId?: number
  referenceNo?: string
  batchNo?: string
  location?: string
}

/**
 * 礼品/赠品表实体
 */
export interface Gift {
  id: number                      // 主键ID
  code: string                    // 礼品兑换码/唯一标识
  description?: string            // 礼品详细描述
  endTime?: string                // 活动结束时间
  limitEnabled: boolean           // 是否启用限额 (true:启用, false:不启用)
  limitPerPerson?: number         // 每人限领数量
  name: string                    // 礼品名称
  remark?: string                 // 备注信息
  startTime?: string              // 活动开始时间
  status: 'ACTIVE' | 'DEPLETED' | 'DRAFT' | 'EXPIRED' | 'PAUSED' // 状态: ACTIVE-活动中, DEPLETED-已领完, DRAFT-草稿, EXPIRED-已过期, PAUSED-已暂停
  type: 'COUPON' | 'PHYSICAL' | 'POINTS' | 'VIRTUAL' // 类型: COUPON-优惠券, PHYSICAL-实物, POINTS-积分, VIRTUAL-虚拟商品
  isDeleted: number              // 是否删除 (软删除标识)
  productId?: number              // 关联的商品ID
  productName?: string            // 关联的商品名称
  createdTime: string            // 创建时间
  updatedTime: string             // 更新时间
  // newGiftLevel?: 0 | 1 | 2          // 邀新礼品等级
}

/**
 * 礼品查询参数
 */
export interface GiftQuery {
  id?: number                     // 主键ID
  code?: string                   // 礼品兑换码 (模糊查询)
  name?: string                   // 礼品名称 (模糊查询)
  statusList?: ('ACTIVE' | 'DEPLETED' | 'DRAFT' | 'EXPIRED' | 'PAUSED')[] // 状态列表
  typeList?: ('COUPON' | 'PHYSICAL' | 'POINTS' | 'VIRTUAL')[] // 类型列表
  productId?: number              // 关联的商品ID
  startTimeStart?: string         // 活动开始时间-开始 (查询范围)
  startTimeEnd?: string           // 活动开始时间-结束 (查询范围)
  endTimeStart?: string           // 活动结束时间-开始 (查询范围)
  endTimeEnd?: string             // 活动结束时间-结束 (查询范围)
  current?: number                // 当前页码
  size?: number                   // 每页条数
}
/**
 * 礼品/赠品表实体
 */
export interface GiftCreateDTO {
  code: string                    // 礼品兑换码/唯一标识
  description?: string            // 礼品详细描述
  endTime?: string                // 活动结束时间
  limitEnabled: boolean           // 是否启用限额 (true:启用, false:不启用)
  limitPerPerson?: number         // 每人限领数量
  name: string                    // 礼品名称
  remark?: string                 // 备注信息
  startTime?: string              // 活动开始时间
  status: 'ACTIVE' | 'DEPLETED' | 'DRAFT' | 'EXPIRED' | 'PAUSED' // 状态: ACTIVE-活动中, DEPLETED-已领完, DRAFT-草稿, EXPIRED-已过期, PAUSED-已暂停
  type: 'COUPON' | 'PHYSICAL' | 'POINTS' | 'VIRTUAL' // 类型: COUPON-优惠券, PHYSICAL-实物, POINTS-积分, VIRTUAL-虚拟商品
  isDeleted: number              // 是否删除 (软删除标识)
  productId?: number              // 关联的商品ID
}

/**
 * 礼品/赠品表实体
 */
export interface GiftUpdateDTO {
  id: number                      // 主键ID
  code: string                    // 礼品兑换码/唯一标识
  description?: string            // 礼品详细描述
  endTime?: string                // 活动结束时间
  limitEnabled: boolean           // 是否启用限额 (true:启用, false:不启用)
  limitPerPerson?: number         // 每人限领数量
  name: string                    // 礼品名称
  remark?: string                 // 备注信息
  startTime?: string              // 活动开始时间
  status: 'ACTIVE' | 'DEPLETED' | 'DRAFT' | 'EXPIRED' | 'PAUSED' // 状态: ACTIVE-活动中, DEPLETED-已领完, DRAFT-草稿, EXPIRED-已过期, PAUSED-已暂停
  type: 'COUPON' | 'PHYSICAL' | 'POINTS' | 'VIRTUAL' // 类型: COUPON-优惠券, PHYSICAL-实物, POINTS-积分, VIRTUAL-虚拟商品
  isDeleted: number              // 是否删除 (软删除标识)
  productId?: number              // 关联的商品ID
}

export interface GiftLogDTO {
  id: number | null                     // 主键ID
  giftId: number | null                  // 礼品ID
  customerId: number | null                // 客户ID
  giftName: string                // 礼品名称
  customerName: string            // 客户名称
  issuedAt: string                // 发放时间
  issueNotes: string                // 发放备注
  operator: string                // 操作人
  remark?: string                 // 备注信息
  status: 'CANCELLED' | 'ISSUED' | 'PENDING' // 状态: CANCELLED-已取消, ISSUED-已发放, PENDING-待发放
  quantity: number                // 发放数量
  createdTime: string            // 创建时间
  updatedTime: string             // 更新时间
}

/**
 * 礼品查询参数
 */
export interface GiftLogQuery {
  id?: number                     // 主键ID
  giftId?: number                  // 礼品ID
  customerId?: number                // 客户ID
  giftName?: string                // 礼品名称
  customerName?: string            // 客户名称
  issuedAt?: string                // 发放时间
  operator?: string                // 操作人
  remark?: string                 // 备注信息
  status?: ('CANCELLED' | 'ISSUED' | 'PENDING')[] // 状态: CANCELLED-已取消, ISSUED-已发放, PENDING-待发放
  quantity?: number                // 发放数量
  createdTime?: string            // 创建时间
  updatedTime?: string             // 更新时间
}

/**
 * 配置表实体
 */
export interface DataDict {
  id: number                      // 主键ID
  groupCode: string              // 字典分组编码
  groupName: string               //配置组名称
  paramCode: string                 // 字典键
  paramName: string               // 字典名称
  paramValue: string               // 字典值
  description: string                // 字典描述
  createdTime: string            // 创建时间
  updatedTime: string             // 更新时间
  // sortOrder: number                // 排序
  status: 'ACTIVE' | 'PAUSED' // 状态: ACTIVE-生效, PAUSED-失效
}
/**
 * 配置表创建实体
 */
export interface DataDictCreateDTO {
  groupCode?: string              // 字典分组编码
  paramCode?: string                 // 字典键
  paramValue?: string               // 字典值
  description?: string                // 字典描述
  createdTime?: string            // 创建时间
  updatedTime?: string             // 更新时间
}

/**
 * 配置表更新实体
 */
export interface DataDictUpdateDTO {
  id?: number                      // 主键ID
  groupCode?: string              // 字典分组编码
  paramCode?: string                 // 字典键
  paramValue?: string               // 字典值
  description?: string                // 字典描述
  createdTime?: string            // 创建时间
  updatedTime?: string             // 更新时间
  status?: string                // 状态: ACTIVE-生效, PAUSED-失效
}

/**
 * 库存日志统计接口
 */
export interface InventoryLogStats {
  inCount: number         // 入库次数
  outCount: number        // 出库次数
  inQuantity: number      // 入库总量
  outQuantity: number     // 出库总量
  totalOperations: number // 总操作次数
  successCount: number    // 成功次数
  failureCount: number    // 失败次数
  successRate: number     // 成功率
  avgCostTime: number     // 平均耗时
}

/**
 * 库存日志分页结果
 */
export interface InventoryLogPageResult {
  content: InventoryLog[]
  totalElements: number
  totalPages: number
  number: number
  size: number
  first: boolean
  last: boolean
  empty: boolean
  stats?: InventoryLogStats
}

/**
 * 库存日志趋势统计
 */
export interface InventoryLogTrendStat {
  date: string
  inCount: number
  outCount: number
  inQuantity: number
  outQuantity: number
  netChange: number
  operationCount: number
}

/**
 * 操作类型统计
 */
export interface InventoryLogTypeStat {
  logType: InventoryLogType
  typeName: string
  count: number
  totalQuantity: number
  percentage: number
  avgQuantity: number
}

/**
 * 操作人统计
 */
export interface InventoryLogOperatorStat {
  operator: string
  operationCount: number
  inCount: number
  outCount: number
  adjustCount: number
  lastOperationTime: string
  successRate: number
  totalQuantity: number
}

/**
 * 库存日志创建请求
 */
export interface CreateInventoryLogRequest {
  productId: number
  productName?: string
  productCode?: string
  productUnit?: string
  logType: InventoryLogType
  beforeStock: number
  changeQuantity: number
  operator?: string
  reason?: string
  source?: InventoryLogSource
  referenceNo?: string
  warehouseId?: number
  warehouseName?: string
  batchNo?: string
  expirationDate?: string
  location?: string
  clientIp?: string
  userAgent?: string
  requestId?: string
}

/**
 * 批量创建库存日志请求
 */
export interface BatchCreateInventoryLogRequest {
  logs: CreateInventoryLogRequest[]
  source: InventoryLogSource
  operator: string
  requestId?: string
}

/**
 * 库存日志更新请求
 */
export interface UpdateInventoryLogRequest {
  reason?: string
  success?: boolean
  errorMessage?: string
  costTime?: number
}

/**
 * 库存日志导出配置
 */
export interface InventoryLogExportConfig {
  format: 'EXCEL' | 'CSV' | 'PDF'
  includeFields: Array<keyof InventoryLog>
  filters: Partial<InventoryLogQueryParams>
  timeRange?: {
    start: string
    end: string
  }
  groupBy?: Array<'product' | 'operator' | 'logType' | 'date'>
  sortBy?: Array<{
    field: keyof InventoryLog
    direction: 'asc' | 'desc'
  }>
  fileName?: string
}

// 类型守卫函数
export function isInventoryLog(obj: any): obj is InventoryLog {
  return obj && typeof obj === 'object' && 'id' in obj && 'productId' in obj && 'logType' in obj
}

// 获取操作类型显示文本
export function getInventoryLogTypeText(logType: InventoryLogType): string {
  const typeMap: Record<InventoryLogType, string> = {
    [InventoryLogType.CREATE]: '新建商品',
    [InventoryLogType.IN]: '入库',
    [InventoryLogType.OUT]: '出库',
    [InventoryLogType.ADJUST]: '调整',
    [InventoryLogType.TRANSFER]: '调拨',
    [InventoryLogType.CHECK]: '盘点'
  }
  return typeMap[logType] || logType
}

// 获取操作类型颜色
export function getInventoryLogTypeColor(logType: InventoryLogType): string {
  const colorMap: Record<InventoryLogType, string> = {
    [InventoryLogType.CREATE]: 'blue',
    [InventoryLogType.IN]: 'green',
    [InventoryLogType.OUT]: 'red',
    [InventoryLogType.ADJUST]: 'orange',
    [InventoryLogType.TRANSFER]: 'purple',
    [InventoryLogType.CHECK]: 'cyan'
  }
  return colorMap[logType] || 'default'
}

// 获取操作来源显示文本
export function getInventoryLogSourceText(source?: InventoryLogSource): string {
  if (!source) return '未知'
  const sourceMap: Record<InventoryLogSource, string> = {
    [InventoryLogSource.WEB]: 'Web界面',
    [InventoryLogSource.API]: 'API接口',
    [InventoryLogSource.INTERNAL]: '内部调用',
    [InventoryLogSource.IMPORT]: '批量导入',
    [InventoryLogSource.SYSTEM]: '系统自动'
  }
  return sourceMap[source] || source
}
