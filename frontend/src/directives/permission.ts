import type { DirectiveBinding } from 'vue'
import { useAuthStore } from '@/stores/auth'

/**
 * v-permission 指令
 * 根据权限 key 控制元素显隐，无权限时从 DOM 移除
 *
 * 用法：
 * <a-button v-permission="'products:delete'">删除</a-button>
 * <a-button v-permission="PERMISSIONS.PRODUCTS_DELETE">删除</a-button>
 */
const permission = {
  mounted(el: HTMLElement, binding: DirectiveBinding<string>) {
    const authStore = useAuthStore()
    if (!authStore.hasPermission(binding.value)) {
      el.parentNode?.removeChild(el)
    }
  },
}

export default permission
