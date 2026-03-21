<!-- frontend/src/components/charts/LineChart.vue -->
<template>
  <div ref="chartRef" class="line-chart-container"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import * as echarts from 'echarts'

interface Props {
  data: {
    labels: string[]
    datasets: Array<{
      label: string
      data: number[]
      borderColor?: string
      backgroundColor?: string
      tension?: number
    }>
  }
  options?: any
  height?: string | number
}

const props = withDefaults(defineProps<Props>(), {
  data: () => ({ labels: [], datasets: [] }),
  options: () => ({}),
  height: '300px'
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
  
  const options = {
    grid: {
      left: '3%',
      right: '4%',
      bottom: '10%',
      top: '10%',
      containLabel: true
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross'
      }
    },
    legend: {
      data: props.data.datasets.map(dataset => dataset.label),
      bottom: 0
    },
    xAxis: {
      type: 'category',
      data: props.data.labels,
      axisLabel: {
        color: '#666'
      },
      axisLine: {
        lineStyle: {
          color: '#ddd'
        }
      }
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        color: '#666'
      },
      axisLine: {
        lineStyle: {
          color: '#ddd'
        }
      },
      splitLine: {
        lineStyle: {
          color: '#f0f0f0'
        }
      }
    },
    series: props.data.datasets.map(dataset => ({
      name: dataset.label,
      type: 'line',
      data: dataset.data,
      smooth: dataset.tension ?? 0.3,
      lineStyle: {
        color: dataset.borderColor || '#1890ff',
        width: 2
      },
      itemStyle: {
        color: dataset.borderColor || '#1890ff'
      },
      areaStyle: dataset.backgroundColor ? {
        color: dataset.backgroundColor
      } : undefined,
      showSymbol: props.data.labels.length <= 10
    })),
    ...props.options
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
.line-chart-container {
  width: 100%;
  height: v-bind(height);
}
</style>