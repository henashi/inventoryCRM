<template>
  <div class="recommendation-page">
    <div class="page-header">
      <div>
        <div class="page-subtitle">基于客户评分智能匹配最佳礼品，支持一键发放</div>
      </div>
    </div>

    <!-- 生日提醒横幅 -->
    <a-alert
      v-if="birthdayCustomers.length > 0"
      :message="`🎂 ${birthdayCustomers.length} 位客户即将生日`"
      :description="
        birthdayCustomers.map((c) => c.name).join('、') +
        ' — 点击「一键发放生日礼品」自动匹配并发放'
      "
      type="warning"
      show-icon
      class="mb-6"
    >
      <template #action>
        <a-button size="small" type="primary" @click="handleBatchBirthday"
          >一键发放生日礼品</a-button
        >
      </template>
    </a-alert>

    <a-row :gutter="16">
      <!-- 左侧：客户选择 -->
      <a-col :xs="24" :lg="10">
        <a-card title="选择客户" :bordered="false" class="mb-6">
          <a-input-search
            v-model:value="searchKeyword"
            placeholder="搜索客户姓名 / 手机号"
            @search="handleSearch"
            class="mb-3"
          />
          <a-table
            row-key="customerId"
            :columns="custColumns"
            :data-source="filteredCustomers"
            :loading="loading.customers"
            size="small"
            :pagination="{
              pageSize: 6,
              showSizeChanger: false,
              showTotal: (t: number) => `共 ${t} 人`,
            }"
            :scroll="{ y: 380 }"
            :row-class="(r: any) => (r.customerId === selectedCustomerId ? 'selected-row' : '')"
            :custom-row="(record) => ({ onClick: () => handleSelectCustomer(record) })"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'name'">
                <div>
                  <div class="font-medium">{{ record.customerName }}</div>
                  <div class="text-xs text-gray-400">{{ record.phone }}</div>
                </div>
              </template>
              <template v-if="column.key === 'score'">
                <a-tag :color="getScoreTagColor(record.totalScore)">{{ record.totalScore }}</a-tag>
              </template>
              <template v-if="column.key === 'segment'">
                <a-tag :color="getSegColor(record.segment)">{{
                  getSegLabel(record.segment)
                }}</a-tag>
              </template>
              <template v-if="column.key === 'birthday'">
                <span v-if="record.isBirthdaySoon" class="text-blue-500"
                  >🎂 {{ record.daysToBirthday }}天</span
                >
              </template>
            </template>
          </a-table>
        </a-card>
      </a-col>

      <!-- 右侧：推荐结果 -->
      <a-col :xs="24" :lg="14">
        <a-card :bordered="false">
          <template #title>
            <span v-if="selectedCustomerInfo">
              为 {{ selectedCustomerInfo.customerName }} 推荐
              <a-tag :color="getSegColor(selectedCustomerInfo.segment)">{{
                getSegLabel(selectedCustomerInfo.segment)
              }}</a-tag>
            </span>
            <span v-else>推荐结果</span>
          </template>

          <div v-if="!selectedCustomerInfo" class="empty-state">
            <a-empty description="请从左侧选择一个客户" />
          </div>

          <div v-else-if="loading.recommendations" class="empty-state">
            <a-spin />
          </div>

          <div v-else-if="recommendations.length === 0" class="empty-state">
            <a-empty description="暂无可推荐的礼品" />
          </div>

          <div v-else class="recommendation-list">
            <div
              v-for="(rec, index) in recommendations"
              :key="rec.giftId"
              class="recommendation-card"
              :class="{ 'top-1': index === 0 }"
            >
              <div class="rec-rank">#{{ index + 1 }}</div>
              <div class="rec-body">
                <div class="rec-header">
                  <span class="rec-name">{{ rec.giftName }}</span>
                  <a-tag v-if="rec.giftType" color="blue">{{ rec.giftType }}</a-tag>
                </div>
                <div class="rec-match">
                  <a-progress
                    :percent="rec.matchScore"
                    :stroke-color="getMatchColor(rec.matchScore)"
                    :format="() => rec.matchScore + '%'"
                    size="small"
                  />
                </div>
                <div class="rec-reason">💡 {{ rec.reason }}</div>
                <div class="rec-actions">
                  <a-button type="primary" size="small" @click="handleIssueGift(rec)"
                    >发放</a-button
                  >
                </div>
              </div>
            </div>
          </div>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
  import { computed, onMounted, ref } from 'vue'
  import { message, Modal } from 'ant-design-vue'
  import request from '@/api/request'
  import { aiApi } from '@/api/ai'
  import type { CustomerScore, GiftRecommendation } from '@/types'

  const allCustomers = ref<CustomerScore[]>([])
  const birthdayCustomers = ref<{ id: number; name: string }[]>([])
  const recommendations = ref<GiftRecommendation[]>([])
  const selectedCustomerId = ref<number | null>(null)
  const searchKeyword = ref('')

  const loading = ref({ customers: false, recommendations: false })

  const selectedCustomerInfo = computed(
    () => allCustomers.value.find((c) => c.customerId === selectedCustomerId.value) || null,
  )

  const filteredCustomers = computed(() => {
    if (!searchKeyword.value) return allCustomers.value
    const kw = searchKeyword.value.toLowerCase()
    return allCustomers.value.filter(
      (c) => c.customerName.toLowerCase().includes(kw) || c.phone.includes(kw),
    )
  })

  const custColumns = [
    { title: '客户', key: 'name', width: 160 },
    { title: '评分', key: 'score', width: 60 },
    { title: '类型', key: 'segment', width: 70 },
    { title: '生日', key: 'birthday', width: 60 },
  ]

  const getScoreTagColor = (s: number) => (s >= 80 ? 'green' : s >= 60 ? 'orange' : 'red')
  const getSegColor = (seg: string) => {
    switch (seg) {
      case 'HIGH_VALUE':
        return 'green'
      case 'GROWING':
        return 'orange'
      default:
        return 'default'
    }
  }
  const getSegLabel = (seg: string) => {
    switch (seg) {
      case 'HIGH_VALUE':
        return '高价值'
      case 'GROWING':
        return '成长'
      default:
        return '待激活'
    }
  }
  const getMatchColor = (s: number) => (s >= 80 ? '#52c41a' : s >= 60 ? '#faad14' : '#f5222d')

  const loadData = async () => {
    loading.value.customers = true
    try {
      const [scoreRes, birthdayRes] = await Promise.all([
        aiApi.getCustomerScores({ page: 0, size: 999 }),
        aiApi.getUpcomingBirthdayCustomers(),
      ])
      allCustomers.value = scoreRes.content
      birthdayCustomers.value = birthdayRes
    } catch {
      message.error('加载客户数据失败')
    } finally {
      loading.value.customers = false
    }
  }

  const handleSearch = () => {}

  const handleSelectCustomer = async (record: CustomerScore) => {
    selectedCustomerId.value = record.customerId
    loading.value.recommendations = true
    try {
      recommendations.value = await aiApi.getRecommendations(record.customerId)
    } catch (e) {
      recommendations.value = []
      console.error('推荐失败:', e)
    } finally {
      loading.value.recommendations = false
    }
  }

  const handleIssueGift = async (rec: GiftRecommendation) => {
    if (!selectedCustomerId.value) return
    Modal.confirm({
      title: '发放礼品',
      content: `确定要为 ${selectedCustomerInfo.value?.customerName} 发放「${rec.giftName}」吗？`,
      onOk: async () => {
        try {
          await request.post('/gift-logs', {
            customerId: selectedCustomerId.value!,
            giftId: rec.giftId,
            quantity: 1,
            operator: 'system',
            issueNotes: 'AI 推荐发放',
          })
          message.success(`已成功发放 ${rec.giftName}`)
          // 刷新推荐列表
          recommendations.value = await aiApi.getRecommendations(selectedCustomerId.value!)
        } catch {
          message.error('发放失败')
        }
      },
    })
  }

  const handleBatchBirthday = async () => {
    Modal.confirm({
      title: '批量发放生日礼品',
      content: `将为 ${birthdayCustomers.value.length} 位生日客户自动匹配并发放礼品，确认？`,
      onOk: async () => {
        let success = 0
        for (const customer of birthdayCustomers.value) {
          try {
            const recs = await aiApi.getRecommendations(customer.id)
            if (recs.length > 0) {
              await request.post('/gift-logs', {
                customerId: customer.id,
                giftId: recs[0].giftId,
                quantity: 1,
                operator: 'system',
                issueNotes: 'AI 推荐发放：生日礼品',
              })
              success++
            }
          } catch {
            // skip
          }
        }
        message.success(`成功为 ${success}/${birthdayCustomers.value.length} 位客户发放生日礼品`)
        // 刷新推荐
        if (selectedCustomerId.value) {
          recommendations.value = await aiApi.getRecommendations(selectedCustomerId.value)
        }
      },
    })
  }

  onMounted(() => {
    loadData()
  })
</script>

<style scoped>
  .recommendation-page {
    padding: 20px;
    background: var(--bg-page);
    min-height: 100vh;
  }
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 20px;
  }
  .page-title {
    font-size: 24px;
    font-weight: 700;
    margin: 0;
    color: #111827;
  }
  .page-subtitle {
    font-size: 14px;
    color: #6b7280;
    margin-top: 4px;
  }
  .mb-6 {
    margin-bottom: 16px;
  }
  .mb-3 {
    margin-bottom: 12px;
  }
  .font-medium {
    font-weight: 500;
  }
  .text-xs {
    font-size: 12px;
  }
  .text-gray-400 {
    color: #9ca3af;
  }
  .text-blue-500 {
    color: #1890ff;
    font-weight: 500;
  }

  .selected-row {
    background-color: #e6f7ff !important;
  }
  :deep(.ant-table-row) {
    cursor: pointer;
  }

  .empty-state {
    height: 400px;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .recommendation-list {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  .recommendation-card {
    display: flex;
    gap: 16px;
    padding: 16px;
    border: 1px solid #e8eef7;
    border-radius: 12px;
    background: #fff;
    transition: box-shadow 0.2s;
  }
  .recommendation-card:hover {
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
  }
  .recommendation-card.top-1 {
    border-color: #52c41a;
    background: linear-gradient(135deg, #f6ffed 0%, #fff 100%);
  }

  .rec-rank {
    font-size: 24px;
    font-weight: 700;
    color: #d9d9d9;
    min-width: 40px;
    text-align: center;
    line-height: 1;
  }
  .top-1 .rec-rank {
    color: #52c41a;
  }

  .rec-body {
    flex: 1;
    min-width: 0;
  }
  .rec-header {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 8px;
  }
  .rec-name {
    font-size: 16px;
    font-weight: 600;
  }
  .rec-match {
    margin-bottom: 8px;
  }
  .rec-reason {
    font-size: 13px;
    color: #6b7280;
    margin-bottom: 8px;
    line-height: 1.5;
  }
  .rec-actions {
    display: flex;
    gap: 8px;
  }

  /* ===== 暗色模式 ===== */
  [data-theme='dark'] .page-title {
    color: var(--text-primary);
  }
  [data-theme='dark'] .page-subtitle {
    color: var(--text-secondary);
  }
  [data-theme='dark'] .recommendation-card {
    background: var(--bg-card);
    border-color: var(--border-color);
  }
  [data-theme='dark'] .recommendation-card.top-1 {
    border-color: #52c41a;
    background: linear-gradient(135deg, rgba(82, 196, 26, 0.12) 0%, var(--bg-card) 100%);
  }
  [data-theme='dark'] .rec-rank {
    color: #4a4a4a;
  }
  [data-theme='dark'] .top-1 .rec-rank {
    color: #52c41a;
  }
  [data-theme='dark'] .rec-name {
    color: var(--text-primary);
  }
  [data-theme='dark'] .rec-reason {
    color: var(--text-secondary);
  }
  [data-theme='dark'] .selected-row {
    background-color: rgba(24, 144, 255, 0.15) !important;
  }
  [data-theme='dark'] .text-gray-400 {
    color: var(--text-tertiary) !important;
  }
</style>
