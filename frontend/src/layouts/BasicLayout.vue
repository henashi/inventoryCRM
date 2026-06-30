<template>
  <a-layout class="app-layout">
    <a-layout-sider
      v-model:collapsed="collapsed"
      collapsible
      width="220"
      collapsed-width="64"
      :trigger="null"
      class="app-sider"
      :theme="themeStore.isDark ? 'dark' : 'light'"
    >
      <div class="sider-header">
        <span class="sider-logo">📦</span>
        <span class="sider-title" v-show="!collapsed">Inventory CRM</span>
      </div>
      <a-menu
        v-model:openKeys="openKeys"
        v-model:selectedKeys="selectedKeys"
        mode="inline"
        :theme="themeStore.isDark ? 'dark' : 'light'"
        @click="handleMenuClick"
      >
        <a-menu-item key="/dashboard">
          <dashboard-outlined />
          <span>仪表盘</span>
        </a-menu-item>

        <a-menu-item v-if="canShow('products') && !canShow('inventory')" key="/products">
          <shop-outlined />
          <span>商品管理</span>
        </a-menu-item>

        <a-menu-item v-if="canShow('products') && !canShow('customers')" key="/orders">
          <shopping-cart-outlined />
          <span>订单管理</span>
        </a-menu-item>

        <a-sub-menu v-if="canShow('inventory')" key="inventory">
          <template #title>
            <shop-outlined />
            <span>库存管理</span>
          </template>
          <a-menu-item key="/inventory">库存概览</a-menu-item>
          <a-menu-item key="/products">商品管理</a-menu-item>
          <a-menu-item v-if="canShow('ai')" key="/inventory/predictions">AI 预测</a-menu-item>
        </a-sub-menu>

        <a-sub-menu v-if="canShow('customers')" key="customer">
          <template #title>
            <team-outlined />
            <span>客户管理</span>
          </template>
          <a-menu-item key="/customers">客户列表</a-menu-item>
          <a-menu-item v-if="canShow('ai')" key="/ai/customers/scores">AI 评分</a-menu-item>
          <a-menu-item v-if="canShow('ai')" key="/ai/customers/gift-recommendations">礼品推荐</a-menu-item>
          <a-menu-item key="/orders">订单管理</a-menu-item>
        </a-sub-menu>

        <a-sub-menu key="gift">
          <template #title>
            <gift-outlined />
            <span>礼品管理</span>
          </template>
          <a-menu-item key="/gifts">礼品列表</a-menu-item>
          <a-menu-item key="/gift-logs">礼品发放</a-menu-item>
        </a-sub-menu>

        <a-sub-menu v-if="canShow('data-dicts')" key="system">
          <template #title>
            <setting-outlined />
            <span>系统管理</span>
          </template>
          <a-menu-item key="/data-dicts">配置管理</a-menu-item>
          <a-menu-item key="/users">用户管理</a-menu-item>
          <a-menu-item key="/roles">角色管理</a-menu-item>
          <a-menu-item key="/permission-defs">权限管理</a-menu-item>
          <a-menu-item key="/operation-logs">系统日志</a-menu-item>
        </a-sub-menu>
      </a-menu>
    </a-layout-sider>

    <a-layout>
      <a-layout-header class="app-header">
        <div class="header-left">
          <menu-fold-outlined v-if="!collapsed" @click="collapsed = !collapsed" class="trigger" />
          <menu-unfold-outlined v-else @click="collapsed = !collapsed" class="trigger" />
        </div>
        <div class="header-title">{{ route.meta?.title }}</div>
        <div class="header-right">
          <a-button
            class="theme-toggle-btn"
            type="text"
            size="small"
            @click="themeStore.toggleTheme()"
            :title="themeStore.isDark ? '切换到亮色模式' : '切换到暗色模式'"
          >
            <span class="theme-icon">{{ themeStore.isDark ? '☀️' : '🌙' }}</span>
          </a-button>
          <a-dropdown>
            <div class="user-info">
              <a-avatar
                :style="{ backgroundColor: avatarColor, verticalAlign: 'middle', fontWeight: 600 }"
                size="small"
                >{{ userInitial }}</a-avatar
              >
              <span class="user-name">{{ userName }}</span>
              <a-tag v-if="roleLabel" :color="avatarColor" class="role-tag">{{ roleLabel }}</a-tag>
            </div>
            <template #overlay>
              <a-menu @click="handleAccountMenu">
                <a-menu-item key="profile">个人中心</a-menu-item>
                <a-menu-divider />
                <a-menu-item key="logout">退出登录</a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
      </a-layout-header>

      <a-layout-content class="app-content">
        <router-view />
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script setup lang="ts">
  import { ref, computed, onMounted } from 'vue'
  import { useRouter, useRoute } from 'vue-router'
  import { useAuthStore } from '@/stores/auth'
  import { useThemeStore } from '@/stores/theme'
  import { canAccessFeature } from '@/router/accessControl'
  import {
    DashboardOutlined,
    ShopOutlined,
    ShoppingCartOutlined,
    TeamOutlined,
    SettingOutlined,
    GiftOutlined,
    MenuFoldOutlined,
    MenuUnfoldOutlined,
  } from '@ant-design/icons-vue'

  const router = useRouter()
  const route = useRoute()
  const authStore = useAuthStore()
  const themeStore = useThemeStore()
  const collapsed = ref(false)

  const userName = computed(() => authStore.user?.name || authStore.user?.username || '用户')
  const userInitial = computed(() => {
    const name = userName.value
    return name.charAt(0).toUpperCase()
  })
  const roleLabel = computed(() => {
    const role = authStore.user?.role
    switch (role) {
      case 'ADMIN':
        return '管理员'
      case 'MANAGER':
        return '经理'
      default:
        return '用户'
    }
  })
  const avatarColor = computed(() => {
    const role = authStore.user?.role
    switch (role) {
      case 'ADMIN':
        return '#1890ff'
      case 'MANAGER':
        return '#52c41a'
      default:
        return '#722ed1'
    }
  })

  const userRole = computed(() => authStore.userRole)

  const canShow = (feature: string) => canAccessFeature(userRole.value, feature as any)

  const selectedKeys = ref<string[]>(['/dashboard'])
  const openKeys = ref<string[]>([])

  onMounted(() => {
    const path = route.path
    selectedKeys.value = [path]
    if (path.startsWith('/inventory') || path.startsWith('/products'))
      openKeys.value.push('inventory')
    if (
      path.startsWith('/customer') ||
      path.startsWith('/ai/customers') ||
      path.startsWith('/orders')
    )
      openKeys.value.push('customer')
    if (path.startsWith('/gift')) openKeys.value.push('gift')
    if (path.startsWith('/data-dicts') || path.startsWith('/operation-logs') || path.startsWith('/users'))
      openKeys.value.push('system')
  })

  function handleMenuClick({ key }: { key: string }) {
    router.push(key)
  }

  async function handleAccountMenu({ key }: { key: string }) {
    if (key === 'profile') {
      router.push('/account')
    } else if (key === 'logout') {
      await authStore.logout()
      router.push('/login')
    }
  }
</script>

<style scoped>
  .app-layout {
    min-height: 100vh;
  }
  .app-sider {
    overflow: auto;
    height: 100vh;
    position: sticky;
    top: 0;
    left: 0;
  }
  .app-sider :deep(.ant-layout-sider-children) {
    display: flex;
    flex-direction: column;
  }
  .sider-header {
    height: 64px;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    flex-shrink: 0;
    background: var(--bg-header);
  }
  .sider-logo {
    font-size: 24px;
  }
  .sider-title {
    font-size: 16px;
    font-weight: 600;
    color: var(--text-primary);
    white-space: nowrap;
  }
  .sider-trigger {
    height: 48px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: rgba(255, 255, 255, 0.65);
    cursor: pointer;
    border-top: 1px solid rgba(255, 255, 255, 0.1);
  }
  .sider-trigger:hover {
    color: #fff;
  }

  .app-header {
    background: var(--bg-header);
    padding: 0 24px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    box-shadow: var(--shadow-header);
    height: 64px;
    flex-shrink: 0;
    position: sticky;
    top: 0;
    z-index: 100;
    transition:
      background 0.3s ease,
      box-shadow 0.3s ease;
  }
  .header-left {
    display: flex;
    align-items: center;
  }
  .trigger {
    font-size: 18px;
    cursor: pointer;
    color: var(--text-primary);
  }
  .header-title {
    flex: 1;
    margin-left: 16px;
    font-size: 16px;
    font-weight: 600;
    color: var(--text-primary);
    transition: color 0.3s ease;
  }
  .header-right {
    display: flex;
    align-items: center;
    gap: 16px;
  }
  .theme-toggle-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    border: none;
  }
  .theme-icon {
    font-size: 18px;
    line-height: 1;
  }
  .user-info {
    display: flex;
    align-items: center;
    gap: 8px;
    cursor: pointer;
  }
  .user-name {
    font-size: 14px;
    color: var(--text-primary);
  }

  .app-content {
    margin: 16px;
    min-height: calc(100vh - 64px - 32px);
    background: var(--bg-page);
    transition: background 0.3s ease;
  }
</style>
