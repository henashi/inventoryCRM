import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'

// Ant Design Vue
import Antd from 'ant-design-vue'
// Use the locally installed package's reset stylesheet to avoid external CDN CORS/404 issues.
// ant-design-vue v4 doesn't include a single antd.css in this package; import reset.css and basic styles.
import 'ant-design-vue/dist/reset.css'
import 'ant-design-vue/es/style/reset.css'
import * as AntdIcons from '@ant-design/icons-vue'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(Antd)

// 从 localStorage 恢复用户信息（如果有），以避免 token 存在但 user 为空导致权限判断出错
import { useAuthStore } from '@/stores/auth'
const authStore = useAuthStore()
authStore.initFromStorage()

// register icons globally
Object.keys(AntdIcons).forEach((key) => {
	// @ts-ignore
	app.component(key, (AntdIcons as any)[key])
})

app.mount('#app')
