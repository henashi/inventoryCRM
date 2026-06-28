<script setup lang="ts">
  import { theme } from 'ant-design-vue'
  import zhCN from 'ant-design-vue/es/locale/zh_CN'
  import dayjs from 'dayjs'
  import 'dayjs/locale/zh-cn'
  import { useRoute } from 'vue-router'
  import FloatingChatButton from '@/components/ai/FloatingChatButton.vue'
  import { useThemeStore } from '@/stores/theme'

  // 设置 dayjs 中文化
  dayjs.locale('zh-cn')

  const route = useRoute()
  const locale = zhCN
  const themeStore = useThemeStore()
  const { defaultAlgorithm, darkAlgorithm } = theme
</script>

<template>
  <a-config-provider
    :locale="locale"
    :theme="{ algorithm: themeStore.isDark ? darkAlgorithm : defaultAlgorithm }"
  >
    <router-view />
    <FloatingChatButton v-if="route.path !== '/login'" />
  </a-config-provider>
</template>

<style>
  /* ===== 全局 CSS 变量 — 亮色主题（默认） ===== */
  :root,
  [data-theme='light'] {
    --bg-page: #f5f7fa;
    --bg-card: #ffffff;
    --bg-header: #ffffff;
    --bg-chat-message: #f5f7fa;
    --bg-chat-avatar: #f0f0f0;
    --text-primary: #1f2937;
    --text-secondary: #6b7280;
    --text-tertiary: #9ca3af;
    --border-color: #f0f0f0;
    --shadow-card: 0 2px 12px rgba(0, 0, 0, 0.06);
    --shadow-header: 0 1px 4px rgba(0, 0, 0, 0.05);
  }

  /* ===== 暗色主题变量覆盖（VS Code Dark+ 风格） ===== */
  [data-theme='dark'] {
    --bg-page: #1e1e1e;
    --bg-card: #252526;
    --bg-header: #2d2d2d;
    --bg-chat-message: #2d2d2d;
    --bg-chat-avatar: #333333;
    --text-primary: #cccccc;
    --text-secondary: #969696;
    --text-tertiary: #6e6e6e;
    --border-color: #3c3c3c;
    --shadow-card: 0 2px 12px rgba(0, 0, 0, 0.4);
    --shadow-header: 0 1px 4px rgba(0, 0, 0, 0.4);
  }

  /* 暗色模式 — Ant Design 卡片背景覆盖 */
  [data-theme='dark'] .ant-card,
  [data-theme='dark'] .ant-card-bordered,
  [data-theme='dark'] .ant-card .ant-card-body {
    background: var(--bg-card) !important;
  }
  /* 暗色模式 — 表头 / 卡片标题 */
  [data-theme='dark'] .ant-table-thead > tr > th,
  [data-theme='dark'] .ant-card-head-title,
  [data-theme='dark'] .ant-card-extra {
    color: #dddddd !important;
  }
  [data-theme='dark'] .ant-table-thead > tr > th {
    background: #2d2d2d !important;
    border-bottom-color: #3c3c3c !important;
  }
  /* 暗色模式 — 表格背景覆盖 */
  [data-theme='dark'] .ant-table,
  [data-theme='dark'] .ant-table-container,
  [data-theme='dark'] .ant-table-content,
  [data-theme='dark'] .ant-table-body,
  [data-theme='dark'] .ant-table-tbody > tr > td {
    background: var(--bg-card) !important;
  }
  [data-theme='dark'] .ant-table-tbody > tr.ant-table-row:hover > td {
    background: #2a2d2e !important;
  }
  [data-theme='dark'] .ant-table-tbody > tr > td {
    border-bottom-color: #3c3c3c !important;
  }
  /* 暗色模式 — 全局文字颜色 */
  [data-theme='dark'] .ant-table-thead > tr > th,
  [data-theme='dark'] .ant-table-tbody > tr > td,
  [data-theme='dark'] .ant-card,
  [data-theme='dark'] .ant-modal-content,
  [data-theme='dark'] .ant-drawer-content,
  [data-theme='dark'] .ant-statistic-content,
  [data-theme='dark'] .ant-form-item-label > label,
  [data-theme='dark'] .ant-input,
  [data-theme='dark'] .ant-input-affix-wrapper,
  [data-theme='dark'] .ant-select-selection-item,
  [data-theme='dark'] .ant-select,
  [data-theme='dark'] .ant-pagination-item a,
  [data-theme='dark'] .ant-pagination-item-active a,
  [data-theme='dark'] .ant-empty-description,
  [data-theme='dark'] .ant-alert-message,
  [data-theme='dark'] .ant-tag {
    color: var(--text-primary) !important;
  }
  /* 暗色模式 — 次要文字 */
  [data-theme='dark'] .ant-table-column-sorter,
  [data-theme='dark'] .ant-alert-description {
    color: var(--text-secondary);
  }
  /* 暗色模式 — 输入框/选择器/按钮背景 */
  [data-theme='dark'] .ant-input,
  [data-theme='dark'] .ant-input-affix-wrapper,
  [data-theme='dark'] .ant-input-group-addon,
  [data-theme='dark'] .ant-select-selector,
  [data-theme='dark'] .ant-picker {
    background: #3c3c3c !important;
    border-color: #3c3c3c !important;
  }
  [data-theme='dark'] .ant-btn-default {
    background: #3c3c3c !important;
    border-color: #555555 !important;
    color: #cccccc !important;
  }
  [data-theme='dark'] .ant-btn-default:hover,
  [data-theme='dark'] .ant-btn-default:focus {
    border-color: #777777 !important;
    color: #e0e0e0 !important;
  }
  [data-theme='dark'] .ant-btn-text {
    color: #cccccc !important;
  }
  /* 暗色模式 — 弹窗 / 下拉菜单 / 分页 */
  [data-theme='dark'] .ant-modal-content,
  [data-theme='dark'] .ant-modal-header,
  [data-theme='dark'] .ant-drawer-content,
  [data-theme='dark'] .ant-drawer-header,
  [data-theme='dark'] .ant-dropdown-menu,
  [data-theme='dark'] .ant-pagination-item,
  [data-theme='dark'] .ant-pagination-prev .ant-pagination-item-link,
  [data-theme='dark'] .ant-pagination-next .ant-pagination-item-link {
    background: var(--bg-card) !important;
  }
  [data-theme='dark'] .ant-modal-header,
  [data-theme='dark'] .ant-drawer-header {
    border-bottom-color: #3c3c3c !important;
  }
  /* 暗色模式 — 统计卡片大号数字加亮 */
  [data-theme='dark'] .stat-value {
    color: #f0f0f0 !important;
  }
  /* 暗色模式 — 商品管理统计卡片 */
  [data-theme='dark'] .summary-card :deep(.ant-card-body) {
    background: transparent !important;
  }
  [data-theme='dark'] .summary-value-inline {
    color: #e8e8e8 !important;
  }
  [data-theme='dark'] .summary-title {
    color: #a0a0a0 !important;
  }
  [data-theme='dark'] .summary-secondary-line {
    color: #888888 !important;
  }
  [data-theme='dark'] .summary-row :deep(.ant-col:nth-child(1)) .summary-card {
    background: linear-gradient(180deg, #2d2d2d 0%, #252526 100%) !important;
    border-color: #3c3c3c !important;
  }
  [data-theme='dark'] .summary-row :deep(.ant-col:nth-child(2)) .summary-card {
    background: linear-gradient(180deg, #2d2d2d 0%, #2a2825 100%) !important;
    border-color: #5a4a20 !important;
  }
  [data-theme='dark'] .summary-row :deep(.ant-col:nth-child(3)) .summary-card {
    background: linear-gradient(180deg, #2d2d2d 0%, #252a26 100%) !important;
    border-color: #1a4a2a !important;
  }
  /* 暗色模式 — 系统设置页卡片、文字、快捷入口 */
  [data-theme='dark'] .shortcut-item {
    background: var(--bg-card) !important;
    border: 1px solid #3c3c3c !important;
  }
  [data-theme='dark'] .shortcut-item:hover {
    box-shadow: 0 8px 20px rgba(0, 0, 0, 0.3) !important;
  }
  [data-theme='dark'] .shortcut-title,
  [data-theme='dark'] .warning-title,
  [data-theme='dark'] .config-title,
  [data-theme='dark'] .category-title {
    color: #e0e0e0 !important;
  }
  [data-theme='dark'] .shortcut-description,
  [data-theme='dark'] .summary-card-label,
  [data-theme='dark'] .page-subtitle,
  [data-theme='dark'] .permission-desc,
  [data-theme='dark'] .warning-desc,
  [data-theme='dark'] .config-desc,
  [data-theme='dark'] .log-desc,
  [data-theme='dark'] .category-desc,
  [data-theme='dark'] .runtime-label,
  [data-theme='dark'] .log-meta {
    color: #a0a0a0 !important;
  }
  [data-theme='dark'] .summary-card-value,
  [data-theme='dark'] .page-title {
    color: #e8e8e8 !important;
  }
  [data-theme='dark'] .summary-card-helper {
    color: #b0b0b0 !important;
  }
  [data-theme='dark'] .permission-item,
  [data-theme='dark'] .warning-item,
  [data-theme='dark'] .config-item,
  [data-theme='dark'] .log-item,
  [data-theme='dark'] .capability-item,
  [data-theme='dark'] .category-item {
    border-color: #3c3c3c !important;
  }
  [data-theme='dark'] .category-count {
    color: #60a5fa !important;
  }
  /* 暗色模式 — 商品管理统计卡片装饰条 */
  [data-theme='dark'] .summary-row :deep(.ant-col:nth-child(1)) .summary-card::before {
    background: linear-gradient(90deg, #3b82f6 0%, #6366f1 100%) !important;
  }
  [data-theme='dark'] .summary-row :deep(.ant-col:nth-child(2)) .summary-card::before {
    background: linear-gradient(90deg, #f59e0b 0%, #d97706 100%) !important;
  }
  [data-theme='dark'] .summary-row :deep(.ant-col:nth-child(3)) .summary-card::before {
    background: linear-gradient(90deg, #10b981 0%, #059669 100%) !important;
  }
  /* 暗色模式 — 统计卡片图标背景 */
  [data-theme='dark'] .stat-icon-wrap {
    background: rgba(255, 255, 255, 0.06) !important;
  }
  /* 暗色模式 — 滚动条（VS Code 风格） */
  [data-theme='dark'] ::-webkit-scrollbar {
    width: 10px;
    height: 10px;
  }
  [data-theme='dark'] ::-webkit-scrollbar-track {
    background: transparent;
  }
  [data-theme='dark'] ::-webkit-scrollbar-thumb {
    background: #424242;
    border-radius: 5px;
  }
  [data-theme='dark'] ::-webkit-scrollbar-thumb:hover {
    background: #4f4f4f;
  }
  /* 暗色模式 — 分割线 */
  [data-theme='dark'] .ant-divider {
    border-color: #3c3c3c !important;
  }

  /* ===== 基础全局样式 ===== */
  html,
  body {
    margin: 0;
    padding: 0;
    background: var(--bg-page);
    transition:
      background 0.3s ease,
      color 0.3s ease;
  }
</style>
