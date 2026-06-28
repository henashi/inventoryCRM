import type {
  Customer,
  DataDict,
  DataDictCreateDTO,
  DataDictUpdateDTO,
  Gift,
  GiftCreateDTO,
  GiftLogDTO,
  GiftUpdateDTO,
  InventoryLog,
  InventoryLogType,
  Product,
} from '@/types'

type Nullable<T> = T | null | undefined

type BackendCustomer = {
  id?: number
  name: string
  phone: string
  email?: string
  address?: string
  birthday?: string
  gender?: 0 | 1
  giftLevel: 0 | 1 | 2 | 3
  type?: number
  referralCount?: number
  referrerName?: string
  referrerId?: number
  registeredAt?: string
  remark?: string
  status: 0 | 1
  createdAt?: string
}

type BackendProduct = {
  id?: number
  name: string
  code: string
  category?: string
  unit: string
  price: number
  cost: number
  currentStock: number
  safeStock: number
  maxStock?: number
  status: 0 | 1
  description?: string
  createdTime?: string
}

type BackendGift = {
  id: number
  code: string
  description?: string
  limitEnabled: boolean
  limitPerPerson?: number
  name: string
  remark?: string
  status: Gift['status'] | 'NEW'
  type: Gift['type'] | 'NEW'
  productId?: number
  productName?: string
  stock?: number
  createdTime?: string
  statusUpdatedTime?: string
  contentUpdatedTime?: string
  updatedTime?: string
  isDeleted?: number
}

type BackendGiftLog = {
  id: number | null
  giftId: number | null
  customerId: number | null
  giftName: string
  customerName: string
  issueAt?: string
  issueNotes: string
  operator: string
  remark?: string
  status: GiftLogDTO['status']
  quantity: number
  createdTime: string
  statusUpdatedTime?: string
  contentUpdatedTime?: string
  updatedTime?: string
  operationType?: string
}

type BackendInventoryLog = {
  id: number
  productId: number
  productName: string
  productCode: string
  type?: string
  quantity: number
  beforeStock: number
  afterStock: number
  reason?: string
  operator: string
  createdTime?: string
  createdAt?: string
  status?: string
}

const compactObject = <T extends Record<string, unknown>>(input: T) =>
  Object.fromEntries(Object.entries(input).filter(([, value]) => value !== undefined))

const toOptionalString = (value: Nullable<string>) => {
  if (typeof value !== 'string') {
    return undefined
  }

  const trimmed = value.trim()
  return trimmed ? trimmed : undefined
}

const toOptionalNumber = (value: Nullable<number>) => {
  if (typeof value !== 'number' || Number.isNaN(value)) {
    return undefined
  }

  return value
}

export const mapPageContent = <TInput, TOutput, TPage extends { content: TInput[] }>(
  page: TPage,
  mapItem: (item: TInput) => TOutput,
): Omit<TPage, 'content'> & { content: TOutput[] } => ({
  ...page,
  content: Array.isArray(page.content) ? page.content.map(mapItem) : [],
})

export const normalizeCustomer = (customer: BackendCustomer): Customer => ({
  ...customer,
  createdAt: customer.createdAt ?? customer.registeredAt,
})

export const normalizeProduct = (product: BackendProduct): Product =>
  ({
    ...product,
    createdAt: product.createdTime,
  }) as Product

export const normalizeGift = (gift: BackendGift): Gift =>
  ({
    ...gift,
    updatedTime:
      gift.updatedTime ?? gift.contentUpdatedTime ?? gift.statusUpdatedTime ?? gift.createdTime,
    isDeleted: gift.isDeleted ?? 0,
  }) as Gift

export const normalizeGiftLog = (giftLog: BackendGiftLog): GiftLogDTO => ({
  ...giftLog,
  issuedAt: giftLog.issueAt ?? '',
  updatedTime:
    giftLog.updatedTime ??
    giftLog.contentUpdatedTime ??
    giftLog.statusUpdatedTime ??
    giftLog.createdTime,
})

export const normalizeInventoryLog = (log: BackendInventoryLog): InventoryLog => ({
  id: log.id,
  productId: log.productId,
  productName: log.productName,
  productCode: log.productCode,
  productUnit: '',
  logType: (log.type ?? '') as InventoryLogType,
  beforeStock: log.beforeStock,
  afterStock: log.afterStock,
  quantity: log.quantity,
  operator: log.operator,
  logTime: log.createdAt ?? log.createdTime ?? '',
  reason: log.reason,
  success: ['SUCCESS', '1'].includes(String(log.status || '').toUpperCase()),
  createdAt: log.createdAt ?? log.createdTime ?? '',
  updatedAt: log.createdAt ?? log.createdTime ?? '',
})

export const normalizeDataDict = (dataDict: DataDict): DataDict => ({
  ...dataDict,
})

export const sanitizeGiftPayload = (payload: GiftCreateDTO | GiftUpdateDTO) =>
  compactObject({
    name: toOptionalString(payload.name),
    code: toOptionalString(payload.code),
    description: toOptionalString(payload.description),
    limitEnabled: payload.limitEnabled,
    limitPerPerson: payload.limitEnabled
      ? toOptionalNumber(payload.limitPerPerson ?? undefined)
      : undefined,
    remark: toOptionalString(payload.remark),
    productId: toOptionalNumber(payload.productId ?? undefined),
    type: payload.type,
    status: payload.status,
  }) as unknown as GiftCreateDTO | GiftUpdateDTO

export const sanitizeGiftLogPayload = (payload: GiftLogDTO & { limitEnabled?: boolean }) =>
  compactObject({
    customerId: toOptionalNumber(payload.customerId ?? undefined),
    giftId: toOptionalNumber(payload.giftId ?? undefined),
    operator: toOptionalString(payload.operator),
    remark: toOptionalString(payload.remark),
    quantity: toOptionalNumber(payload.quantity),
    issueNotes: toOptionalString(payload.issueNotes),
    status: payload.status,
  })

export const sanitizeDataDictPayload = (
  payload: DataDictCreateDTO | DataDictUpdateDTO | Partial<DataDict>,
) =>
  compactObject({
    groupCode: toOptionalString(payload.groupCode),
    groupName: toOptionalString((payload as Partial<DataDict>).groupName),
    paramCode: toOptionalString(payload.paramCode),
    paramName: toOptionalString((payload as Partial<DataDict>).paramName),
    paramValue: toOptionalString(payload.paramValue),
    description: toOptionalString(payload.description),
  }) as unknown as DataDictCreateDTO | DataDictUpdateDTO
