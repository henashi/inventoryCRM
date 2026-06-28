/**
 * 测试挂载辅助函数。
 * 使用 shallowMount + stubs 隔离组件依赖。
 */
import { shallowMount } from '@vue/test-utils'

/**
 * 公共组件 Stub 表 — 覆盖 Ant Design Vue 常用组件。
 */
export const antStubs: Record<string, any> = {
  'a-button': { template: '<button class="ant-btn"><slot /></button>' },
  'a-card': { template: '<div class="ant-card"><slot /></div>' },
  'a-table': { template: '<div class="ant-table"><slot name="title" /><slot /></div>' },
  'a-form': { template: '<form><slot /></form>' },
  'a-form-item': { template: '<div class="ant-form-item"><slot /></div>' },
  'a-select': { template: '<select class="ant-select"><slot /></select>' },
  'a-select-option': { template: '<option class="ant-select-option"><slot /></option>' },
  'a-input': { template: '<input class="ant-input" />' },
  'a-input-password': { template: '<input class="ant-input-password" type="password" />' },
  'a-input-number': { template: '<input class="ant-input-number" type="number" />' },
  'a-tag': { template: '<span class="ant-tag"><slot /></span>' },
  'a-space': { template: '<div class="ant-space"><slot /></div>' },
  'a-row': { template: '<div class="ant-row"><slot /></div>' },
  'a-col': { template: '<div class="ant-col"><slot /></div>' },
  'a-radio-group': { template: '<div class="ant-radio-group"><slot /></div>' },
  'a-radio-button': { template: '<label class="ant-radio-button"><slot /></label>' },
  'a-list': { template: '<div class="ant-list"><slot /></div>' },
  'a-list-item': { template: '<div class="ant-list-item"><slot /></div>' },
  'a-list-item-meta': { template: '<div class="ant-list-item-meta"><slot /></div>' },
  'a-empty': { template: '<div class="ant-empty"><slot /></div>' },
  'a-drawer': { template: '<div class="ant-drawer"><slot /></div>', props: ['open', 'v-model:open'] },
  'a-modal': { template: '<div class="ant-modal"><slot /></div>', props: ['open', 'v-model:open'] },
  'a-tabs': { template: '<div class="ant-tabs"><slot /></div>' },
  'a-tab-pane': { template: '<div class="ant-tab-pane"><slot /></div>' },
  'a-badge': { template: '<span class="ant-badge"><slot /></span>' },
  'a-popconfirm': { template: '<span class="ant-popconfirm"><slot /></span>' },
  'a-spin': { template: '<div class="ant-spin"><slot /></div>' },
  'a-statistic': { template: '<div class="ant-statistic"><slot /></div>' },
  'a-range-picker': { template: '<input class="ant-range-picker" />' },
  'a-avatar': { template: '<span class="ant-avatar"><slot /></span>' },
  'a-alert': { template: '<div class="ant-alert"><slot /></div>' },
  'a-switch': { template: '<input class="ant-switch" type="checkbox" />' },
  'a-checkbox': { template: '<input class="ant-checkbox" type="checkbox" />' },
  'a-checkbox-group': { template: '<div class="ant-checkbox-group"><slot /></div>' },
  'a-textarea': { template: '<textarea class="ant-textarea" />' },
  'a-upload': { template: '<div class="ant-upload"><slot /></div>' },
  'a-dropdown': { template: '<div class="ant-dropdown"><slot /></div>' },
  'a-menu': { template: '<div class="ant-menu"><slot /></div>' },
  'a-menu-item': { template: '<div class="ant-menu-item"><slot /></div>' },
  'a-sub-menu': { template: '<div class="ant-sub-menu"><slot /></div>' },
  'a-layout': { template: '<div class="ant-layout"><slot /></div>' },
  'a-layout-sider': { template: '<div class="ant-layout-sider"><slot /></div>' },
  'a-layout-header': { template: '<div class="ant-layout-header"><slot /></div>' },
  'a-layout-content': { template: '<div class="ant-layout-content"><slot /></div>' },
  'a-breadcrumb': { template: '<div class="ant-breadcrumb"><slot /></div>' },
  'a-breadcrumb-item': { template: '<span class="ant-breadcrumb-item"><slot /></span>' },
  'a-result': { template: '<div class="ant-result"><slot /></div>' },
  'a-descriptions': { template: '<div class="ant-descriptions"><slot /></div>' },
  'a-descriptions-item': { template: '<div class="ant-descriptions-item"><slot /></div>' },
  'a-collapse': { template: '<div class="ant-collapse"><slot /></div>' },
  'a-collapse-panel': { template: '<div class="ant-collapse-panel"><slot /></div>' },
  'a-tooltip': { template: '<span class="ant-tooltip"><slot /></span>' },
  'a-progress': { template: '<div class="ant-progress"><slot /></div>' },
  'a-divider': { template: '<hr class="ant-divider" />' },
  'router-link': { template: '<a class="router-link"><slot /></a>' },
  'router-view': { template: '<div class="router-view" />' },
  'gift-outlined': { template: '<span class="anticon anticon-gift" />' },
  'left-outlined': { template: '<span class="anticon anticon-left" />' },
  'right-outlined': { template: '<span class="anticon anticon-right" />' },
  'reload-outlined': { template: '<span class="anticon anticon-reload" />' },
  'plus-outlined': { template: '<span class="anticon anticon-plus" />' },
  'import-outlined': { template: '<span class="anticon anticon-import" />' },
  'export-outlined': { template: '<span class="anticon anticon-export" />' },
  'stop-outlined': { template: '<span class="anticon anticon-stop" />' },
  'check-outlined': { template: '<span class="anticon anticon-check" />' },
  'search-outlined': { template: '<span class="anticon anticon-search" />' },
  'edit-outlined': { template: '<span class="anticon anticon-edit" />' },
  'delete-outlined': { template: '<span class="anticon anticon-delete" />' },
  'sync-outlined': { template: '<span class="anticon anticon-sync" />' },
  'home-outlined': { template: '<span class="anticon anticon-home" />' },
  'robot-outlined': { template: '<span class="anticon anticon-robot" />' },
  transition: false,
  'transition-group': false,
}

/**
 * 执行 shallowMount，合并公共 stubs 与自定义选项。
 * 调用方需自行创建 Pinia (`setActivePinia(createPinia())`)。
 */
export function mountShallow(
  component: any,
  options: {
    props?: Record<string, any>
    global?: Record<string, any>
  } = {},
) {
  const mergedStubs = { ...antStubs, ...(options.global?.stubs || {}) }

  return shallowMount(component, {
    props: options.props || {},
    global: {
      ...options.global,
      stubs: mergedStubs,
    },
  })
}
