// @ts-nocheck
import { createPinia, setActivePinia } from 'pinia'
import { shallowMount } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router'
import type { ComponentPublicInstance } from 'vue'
import Antd from 'ant-design-vue'

/**
 * 挂载函数 — 配置全局 stubs/mocks。
 * 自动处理 Pinia 初始化、Ant Design Vue 注册、Router mock。
 */
export function mountWithSetup(
  component: any,
  options: {
    props?: Record<string, any>
    storeState?: Record<string, any>
    routePath?: string
    routeParams?: Record<string, string>
  } = {},
) {
  setActivePinia(createPinia())

  // mock router
  const pushFn = vi.fn()
  const router = createRouter({
    history: createWebHistory(),
    routes: [{ path: '/:pathMatch(.*)*', component: { template: '<div />' } }],
  })
  router.push = pushFn
  router.replace = vi.fn()

  return shallowMount(component, {
    props: options.props || {},
    global: {
      plugins: [router, Antd],
      stubs: {
        'a-button': { template: '<button class="ant-btn"><slot /></button>' },
        'a-card': { template: '<div class="ant-card"><slot /></div>' },
        'a-table': { template: '<div class="ant-table"><slot name="bodyCell" v-for="item in []" :key="item" /></div>' },
        'a-form': { template: '<form><slot /></form>' },
        'a-form-item': { template: '<div class="ant-form-item"><slot /></div>' },
        'a-select': { template: '<select class="ant-select"><slot /></select>' },
        'a-input': { template: '<input class="ant-input" />' },
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
        'a-drawer': { template: '<div class="ant-drawer"><slot /></div>' },
        'a-modal': { template: '<div class="ant-modal"><slot /></div>' },
        'a-tabs': { template: '<div class="ant-tabs"><slot /></div>' },
        'a-tab-pane': { template: '<div class="ant-tab-pane"><slot /></div>' },
        'a-badge': { template: '<span class="ant-badge"><slot /></span>' },
        'a-popconfirm': { template: '<span class="ant-popconfirm"><slot /></span>' },
        'a-spin': { template: '<div class="ant-spin"><slot /></div>' },
        'a-statistic': { template: '<div class="ant-statistic"><slot /></div>' },
        'a-range-picker': { template: '<input class="ant-range-picker" />' },
        'a-avatar': { template: '<span class="ant-avatar"><slot /></span>' },
        'a-alert': { template: '<div class="ant-alert"><slot /></div>' },
        'transition': false,
        'transition-group': false,
      },
      mocks: {
        $route: { path: options.routePath || '/', params: options.routeParams || {} },
        $router: { push: pushFn, replace: vi.fn() },
      },
      provide: {
        route: { path: options.routePath || '/', params: options.routeParams || {} },
      },
    },
  })
}
