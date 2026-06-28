import { describe, expect, it } from 'vitest'
import {
  normalizeCustomer,
  normalizeGift,
  normalizeGiftLog,
  normalizeInventoryLog,
  sanitizeDataDictPayload,
  sanitizeGiftLogPayload,
  sanitizeGiftPayload,
} from './contracts'

describe('contract adapters', () => {
  it('normalizes backend gift dto into the view model shape used by the page', () => {
    const gift = normalizeGift({
      id: 1,
      name: '夏日礼包',
      code: 'GIFT001',
      description: '限时活动礼品',
      stock: 12,
      type: 'PHYSICAL',
      productId: 9,
      productName: '防晒霜',
      status: 'ACTIVE',
      limitEnabled: true,
      limitPerPerson: 2,
      createdTime: '2026-06-01T08:00:00',
      statusUpdatedTime: '2026-06-10T09:00:00',
      contentUpdatedTime: '2026-06-11T10:00:00',
      remark: '重点活动',
    })

    expect(gift).toMatchObject({
      stock: 12,
      createdTime: '2026-06-01T08:00:00',
      updatedTime: '2026-06-11T10:00:00',
      isDeleted: 0,
    })
  })

  it('normalizes backend gift log dto into the shape consumed by the page', () => {
    const giftLog = normalizeGiftLog({
      id: 8,
      customerId: 3,
      giftId: 5,
      operationType: 'ISSUE',
      quantity: 1,
      customerName: '张三',
      giftName: '首单礼',
      issueAt: '2026-06-12T13:00:00',
      createdTime: '2026-06-12T12:00:00',
      statusUpdatedTime: '2026-06-12T14:00:00',
      contentUpdatedTime: '2026-06-12T15:00:00',
      issueNotes: '现场发放',
      status: 'ISSUED',
      operator: 'admin',
      remark: '已签收',
    })

    expect(giftLog).toMatchObject({
      issuedAt: '2026-06-12T13:00:00',
      updatedTime: '2026-06-12T15:00:00',
      createdTime: '2026-06-12T12:00:00',
      issueNotes: '现场发放',
      status: 'ISSUED',
    })
  })

  it('normalizes backend inventory log dto into the current page model without inventing missing fields', () => {
    const inventoryLog = normalizeInventoryLog({
      id: 11,
      productId: 6,
      productName: '马克杯',
      productCode: 'SKU-006',
      type: 'IN',
      quantity: 20,
      beforeStock: 5,
      afterStock: 25,
      reason: '补货',
      operator: 'manager',
      createdTime: '2026-06-13 10:00:00',
      status: 'SUCCESS',
    })

    expect(inventoryLog).toMatchObject({
      logType: 'IN',
      createdAt: '2026-06-13 10:00:00',
      success: true,
      productUnit: '',
    })
  })

  it('normalizes customer dates so dashboard and list views can use a consistent field', () => {
    const customer = normalizeCustomer({
      id: 2,
      name: '李四',
      phone: '13800000000',
      email: 'lisi@example.com',
      address: '上海',
      birthday: '1995-05-01',
      gender: 1,
      giftLevel: 2,
      type: 0,
      referralCount: 1,
      referrerName: '王五',
      referrerId: 10,
      registeredAt: '2026-06-02',
      remark: '重点客户',
      status: 1,
    })

    expect(customer.createdAt).toBe('2026-06-02')
    expect(customer.registeredAt).toBe('2026-06-02')
  })

  it('drops unsupported frontend-only fields before calling gift and gift-log endpoints', () => {
    expect(
      sanitizeGiftPayload({
        name: '礼品',
        code: 'G-1',
        description: '描述',
        limitEnabled: true,
        limitPerPerson: 1,
        remark: '备注',
        productId: 1,
        type: 'NEW',
        status: 'ACTIVE',
        isDeleted: 0,
        startTime: '2026-06-01',
        endTime: '2026-06-30',
      } as any),
    ).toEqual({
      name: '礼品',
      code: 'G-1',
      description: '描述',
      limitEnabled: true,
      limitPerPerson: 1,
      remark: '备注',
      productId: 1,
      type: 'NEW',
      status: 'ACTIVE',
    })

    expect(
      sanitizeGiftLogPayload({
        id: 1,
        giftId: 2,
        customerId: 3,
        giftName: '礼品',
        customerName: '客户',
        quantity: 1,
        issueNotes: '备注',
        remark: '扩展备注',
        operator: 'admin',
        status: 'ISSUED',
        issuedAt: '2026-06-12',
        createdTime: '2026-06-11',
        updatedTime: '2026-06-13',
        limitEnabled: true,
      } as any),
    ).toEqual({
      giftId: 2,
      customerId: 3,
      quantity: 1,
      issueNotes: '备注',
      remark: '扩展备注',
      operator: 'admin',
      status: 'ISSUED',
    })
  })

  it('drops unsupported data-dict fields before submit', () => {
    expect(
      sanitizeDataDictPayload({
        groupName: '库存',
        groupCode: 'inventory',
        paramName: '低库存阈值',
        paramCode: 'low_stock_threshold',
        paramValue: '10',
        description: '库存预警值',
        status: 'DICT_STATUS_ACTIVE',
        isDeleted: 0,
      } as any),
    ).toEqual({
      groupName: '库存',
      groupCode: 'inventory',
      paramName: '低库存阈值',
      paramCode: 'low_stock_threshold',
      paramValue: '10',
      description: '库存预警值',
    })
  })
})
