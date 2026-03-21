<!-- frontend/src/components/charts/PieChart.vue -->
<template>
  <div ref="chartRef" class="pie-chart-container"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import * as echarts from 'echarts'

interface Props {
  data: {
    labels: string[]
    datasets: Array<{
      data: number[]
      backgroundColor?: string[]
      radius?: string | string[]
    }>
  }
  options?: any
  height?: string | number
  showLegend?: boolean
  showLabel?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  data: () => ({ labels: [], datasets: [] }),
  options: () => ({}),
  height: '300px',
  showLegend: true,
  showLabel: true
})

const chartRef = ref<HTMLElement>()
let chartInstance: echarts.ECharts | null = null

const initChart = () => {
  if (!chartRef.value) return
  
  chartInstance = echarts.init(chartRef.value)
  updateChart()
}

const updateChart = () => {
  if (!chartInstance || !props.data.labels.length) return
  
  const dataset = props.data.datasets[0] || { data: [], backgroundColor: [] }
  const colors = dataset.backgroundColor || [
    '#1890ff', '#52c41a', '#faad14', '#f5222d',
    '#722ed1', '#13c2c2', '#eb2f96', '#fa8c16'
  ]
  
  const options = {
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b}: {c} ({d}%)'
    },
    legend: props.showLegend ? {
      type: 'scroll',
      orient: 'vertical',
      right: 10,
      top: 'center',
      data: props.data.labels
    } : undefined,
    series: [
      {
        name: '数据',
        type: 'pie',
        radius: dataset.radius || ['50%', '70%'],
        center: ['40%', '50%'],
        avoidLabelOverlap: false,
        label: props.showLabel ? {
          show: true,
          position: 'outside',
          formatter: '{b}: {c} ({d}%)'
        } : {
          show: false
        },
        emphasis: {
          label: {
            show: true,
            fontSize: '14',
            fontWeight: 'bold'
          }
        },
        labelLine: {
          show: props.showLabel
        },
        data: props.data.labels.map((label, index) => ({
          name: label,
          value: dataset.data[index] || 0,
          itemStyle: {
            color: colors[index % colors.length]
          }
        })),
        ...props.options
      }
    ]
  }
  
  chartInstance.setOption(options)
}

const resizeChart = () => {
  chartInstance?.resize()
}

onMounted(() => {
  initChart()
  window.addEventListener('resize', resizeChart)
})

onUnmounted(() => {
  window.removeEventListener('resize', resizeChart)
  chartInstance?.dispose()
  chartInstance = null
})

watch(() => props.data, updateChart, { deep: true })
watch(() => props.height, () => {
  setTimeout(() => resizeChart(), 100)
})
</script>

<style scoped>
.pie-chart-container {
  width: 100%;
  height: v-bind(height);
}
</style>