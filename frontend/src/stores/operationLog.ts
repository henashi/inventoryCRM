import { defineStore } from 'pinia'
import { ref } from 'vue'
import { operationLogApi } from '@/api/operationLog'
import type { OperationLogRecord, OperationLogQuery } from '@/api/operationLog'

export const useOperationLogStore = defineStore('operationLog', () => {
  const logs = ref<OperationLogRecord[]>([])
  const isLoading = ref(false)
  const pagination = ref({
    page: 1,
    size: 20,
    total: 0,
  })

  /** 统计概览（基于当前分页数据估算） */
  const stats = ref({
    totalCount: 0,
    successCount: 0,
    failCount: 0,
    moduleCount: 0,
  })

  /** 模块列表（用于筛选下拉） */
  const moduleOptions = ref<string[]>([])

  /** 所有已知模块（跨页累积） */
  const allKnownModules = ref<Set<string>>(new Set())

  async function loadLogs(params?: OperationLogQuery) {
    try {
      isLoading.value = true
      const res = await operationLogApi.search({
        keyword: params?.keyword,
        module: params?.module,
        operator: params?.operator,
        status: params?.status,
        startTime: params?.startTime,
        endTime: params?.endTime,
        page: params?.page ?? 0,
        size: params?.size ?? pagination.value.size,
      })
      logs.value = res.content
      pagination.value.total = res.totalElements
      if (params?.page !== undefined) {
        pagination.value.page = params.page + 1
      }
      if (params?.size !== undefined) {
        pagination.value.size = params.size
      }
      // 更新统计和模块列表
      updateStats(res.content)
      collectModules(res.content)
    } finally {
      isLoading.value = false
    }
  }

  function updateStats(data: OperationLogRecord[]) {
    const success = data.filter((r) => r.status === 1 || r.status == null).length
    const fail = data.filter((r) => r.status === 0).length
    stats.value = {
      totalCount: pagination.value.total,
      successCount: success,
      failCount: fail,
      moduleCount: allKnownModules.value.size,
    }
  }

  function collectModules(data: OperationLogRecord[]) {
    for (const r of data) {
      if (r.module) allKnownModules.value.add(r.module)
    }
    moduleOptions.value = [...allKnownModules.value].sort()
  }

  return {
    logs,
    isLoading,
    pagination,
    stats,
    moduleOptions,
    loadLogs,
    collectModules,
  }
})
