<script setup lang="ts">
  import zhCN from 'ant-design-vue/es/locale/zh_CN'
  import dayjs from 'dayjs'
  import 'dayjs/locale/zh-cn'
  import FloatingChatButton from '@/components/ai/FloatingChatButton.vue'

  // 设置 dayjs 中文化
  dayjs.locale('zh-cn')

  const locale = zhCN
</script>

<template>
  <a-config-provider :locale="locale">
    <router-view />
    <FloatingChatButton />
  </a-config-provider>
</template>
