<script setup lang="ts">
  import zhCN from 'ant-design-vue/es/locale/zh_CN'
  import dayjs from 'dayjs'
  import 'dayjs/locale/zh-cn'

  // 设置 dayjs 中文化
  dayjs.locale('zh-cn')

  const locale = zhCN
</script>

<template>
  <a-config-provider :locale="locale">
    <router-view />
  </a-config-provider>
</template>
