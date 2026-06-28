<template>
  <div class="pie-chart-wrapper">
    <div ref="chartRef" class="pie-chart-container"></div>
    <div v-if="showLegend && legendItems.length" class="pie-chart-legend" aria-label="礼品等级说明">
      <div
        v-for="item in legendItems"
        :key="item.name"
        class="pie-chart-legend-item"
        :class="{ 'is-active': activeDataIndex === item.index }"
      >
        <span class="pie-chart-legend-swatch" :style="{ backgroundColor: item.color }"></span>
        <span class="pie-chart-legend-text">{{ item.name }}</span>
      </div>
    </div>
  </div>
  <Teleport to="body">
    <svg
      v-if="focusOverlay"
      class="pie-chart-focus-overlay"
      :width="viewportSize.width"
      :height="viewportSize.height"
      :viewBox="`0 0 ${viewportSize.width} ${viewportSize.height}`"
      aria-hidden="true"
    >
      <polyline
        class="pie-chart-focus-line"
        :points="focusOverlay.linePoints"
        :stroke="focusOverlay.lineColor"
      />
      <text
        ref="focusTextRef"
        class="pie-chart-focus-label"
        :x="focusOverlay.textX"
        :y="focusOverlay.textY"
        :text-anchor="focusOverlay.textAnchor"
        dominant-baseline="middle"
      >
        {{ focusOverlay.text }}
      </text>
    </svg>
  </Teleport>
</template>

<script setup lang="ts">
  import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
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

  type FocusOverlay = {
    text: string
    textX: number
    textY: number
    textAnchor: 'start' | 'end'
    linePoints: string
    lineColor: string
  }

  type PieSeriesSnapshot = {
    center?: Array<string | number>
    radius?: Array<string | number> | string | number
  }

  type PieChartEventParams = {
    componentType?: string
    seriesType?: string
    dataIndex?: number
    name?: string | number
    value?: unknown
    percent?: number | string
    color?: unknown
    event?: unknown
  }

  const props = withDefaults(defineProps<Props>(), {
    data: () => ({ labels: [], datasets: [] }),
    options: () => ({}),
    height: '300px',
    showLegend: true,
    showLabel: true,
  })

  const chartRef = ref<HTMLElement>()
  const focusTextRef = ref<SVGTextElement>()
  const focusOverlay = ref<FocusOverlay | null>(null)
  const activeDataIndex = ref<number | null>(null)
  const viewportSize = ref({
    width: window.innerWidth,
    height: window.innerHeight,
  })
  let chartInstance: echarts.ECharts | null = null
  const textMeasureCanvas = document.createElement('canvas')
  const textMeasureContext = textMeasureCanvas.getContext('2d')

  const getDatasetColors = () => {
    const dataset = props.data.datasets[0]

    return (
      dataset?.backgroundColor || [
        '#1890ff',
        '#52c41a',
        '#faad14',
        '#f5222d',
        '#722ed1',
        '#13c2c2',
        '#eb2f96',
        '#fa8c16',
      ]
    )
  }

  const legendItems = computed(() => {
    const colors = getDatasetColors()

    return props.data.labels.map((name, index) => ({
      index,
      name,
      color: colors[index % colors.length],
    }))
  })

  const formatPercent = (percent: number) => {
    if (Number.isInteger(percent)) {
      return percent.toFixed(0)
    }

    return percent.toFixed(2)
  }

  const updateViewportSize = () => {
    viewportSize.value = {
      width: window.innerWidth,
      height: window.innerHeight,
    }
  }

  const getSeriesData = () => {
    const dataset = props.data.datasets[0]
    const colors = getDatasetColors()

    return props.data.labels.map((label, index) => ({
      name: label,
      value: dataset?.data[index] || 0,
      itemStyle: {
        color: colors[index % colors.length],
      },
      label:
        activeDataIndex.value === index
          ? {
              show: false,
            }
          : undefined,
      labelLine:
        activeDataIndex.value === index
          ? {
              show: false,
            }
          : undefined,
    }))
  }

  const getFocusTextWidth = (text: string) => {
    if (textMeasureContext) {
      textMeasureContext.font = '900 30px system-ui'
      return Math.ceil(textMeasureContext.measureText(text).width)
    }

    if (focusTextRef.value) {
      return focusTextRef.value.getBBox().width
    }

    return Math.max(220, text.length * 24)
  }

  const clearFocusOverlay = (shouldRender = true) => {
    focusOverlay.value = null
    if (activeDataIndex.value !== null) {
      activeDataIndex.value = null
      if (shouldRender) {
        updateChart()
      }
    }
  }

  const getChartGeometry = () => {
    if (!chartRef.value) {
      return null
    }

    const rect = chartRef.value.getBoundingClientRect()
    const series = chartInstance?.getOption()?.series
    const option = Array.isArray(series) ? (series[0] as PieSeriesSnapshot | undefined) : undefined
    const center = Array.isArray(option?.center) ? option.center : ['50%', '50%']
    const radius = Array.isArray(option?.radius)
      ? (option.radius[1] ?? option.radius[0])
      : (option?.radius ?? '68%')
    const centerX =
      typeof center[0] === 'string' && center[0].endsWith('%')
        ? rect.left + (rect.width * Number.parseFloat(center[0])) / 100
        : rect.left + Number(center[0] ?? rect.width * 0.5)
    const centerY =
      typeof center[1] === 'string' && center[1].endsWith('%')
        ? rect.top + (rect.height * Number.parseFloat(center[1])) / 100
        : rect.top + Number(center[1] ?? rect.height * 0.5)
    const radiusBase = Math.min(rect.width, rect.height) / 2
    const outerRadius =
      typeof radius === 'string' && radius.endsWith('%')
        ? (radiusBase * Number.parseFloat(radius)) / 100
        : Number(radius ?? radiusBase * 0.68)

    return {
      rect,
      centerX,
      centerY,
      outerRadius,
    }
  }

  const syncFocusOverlay = (params: PieChartEventParams, text: string, index: number | null) => {
    const geometry = getChartGeometry()
    if (!geometry || activeDataIndex.value !== index) {
      return
    }

    const { rect, centerX, centerY, outerRadius } = geometry
    const zrEvent =
      typeof params.event === 'object' && params.event !== null
        ? (params.event as { offsetX?: number; offsetY?: number })
        : {}
    const offsetX = typeof zrEvent.offsetX === 'number' ? zrEvent.offsetX : rect.width * 0.5
    const offsetY = typeof zrEvent.offsetY === 'number' ? zrEvent.offsetY : rect.height * 0.5
    const anchorX = rect.left + offsetX
    const anchorY = rect.top + offsetY

    let vectorX = anchorX - centerX
    let vectorY = anchorY - centerY
    const vectorLength = Math.hypot(vectorX, vectorY) || 1
    vectorX /= vectorLength
    vectorY /= vectorLength

    const isLeftSide = vectorX < 0
    const radialDistance = outerRadius + 18
    const elbowDistance = outerRadius + 42
    const horizontalLength = 34
    const textGap = 14
    const viewportPadding = 14

    const radialAnchorX = centerX + vectorX * radialDistance
    const radialAnchorY = centerY + vectorY * radialDistance
    const elbowX = centerX + vectorX * elbowDistance
    const elbowY = centerY + vectorY * elbowDistance

    const textWidth = getFocusTextWidth(text)
    const textAnchor: 'start' | 'end' = isLeftSide ? 'end' : 'start'
    const lineEndX = isLeftSide ? elbowX - horizontalLength : elbowX + horizontalLength
    const unclampedTextX = isLeftSide ? lineEndX - textGap : lineEndX + textGap
    const textX = isLeftSide
      ? Math.min(
          viewportSize.value.width - viewportPadding,
          Math.max(viewportPadding + textWidth, unclampedTextX),
        )
      : Math.max(
          viewportPadding,
          Math.min(viewportSize.value.width - viewportPadding - textWidth, unclampedTextX),
        )
    const textY = Math.min(viewportSize.value.height - 24, Math.max(24, elbowY))
    const linePoints = `${lineEndX},${textY} ${elbowX},${textY} ${radialAnchorX},${radialAnchorY}`

    focusOverlay.value = {
      text,
      textX,
      textY,
      textAnchor,
      linePoints,
      lineColor: typeof params.color === 'string' ? params.color : '#111827',
    }
  }

  const updateFocusOverlay = (params: PieChartEventParams) => {
    if (
      !chartRef.value ||
      !chartInstance ||
      params.componentType !== 'series' ||
      params.seriesType !== 'pie'
    ) {
      return
    }

    const lineColor = typeof params.color === 'string' ? params.color : '#111827'
    const nextActiveIndex = typeof params.dataIndex === 'number' ? params.dataIndex : null
    activeDataIndex.value = nextActiveIndex
    updateChart()

    const text = `${params.name || ''}: ${params.value || 0} (${formatPercent(Number(params.percent) || 0)}%)`
    focusOverlay.value = {
      text,
      textX: 0,
      textY: 0,
      textAnchor: 'start',
      linePoints: '',
      lineColor,
    }

    void nextTick(() => {
      syncFocusOverlay(params, text, nextActiveIndex)
    })
  }

  const bindChartEvents = () => {
    if (!chartInstance) {
      return
    }

    chartInstance.off('mouseover')
    chartInstance.off('click')
    chartInstance.off('mouseout')
    chartInstance.off('globalout')

    chartInstance.on('mouseover', updateFocusOverlay)
    chartInstance.on('click', updateFocusOverlay)
    chartInstance.on('mouseout', () => clearFocusOverlay())
    chartInstance.on('globalout', () => clearFocusOverlay())
  }

  const updateChart = () => {
    if (!chartInstance || !props.data.labels.length) {
      return
    }

    const dataset = props.data.datasets[0]
    const defaultRadius = dataset?.radius || ['50%', '68%']

    const options = {
      tooltip: {
        show: false,
      },
      legend: {
        show: false,
      },
      series: [
        {
          name: '数据',
          type: 'pie',
          radius: defaultRadius,
          center: ['50%', '50%'],
          selectedMode: 'single',
          selectedOffset: 10,
          avoidLabelOverlap: true,
          label: props.showLabel
            ? {
                show: true,
                position: 'outside',
                formatter: '{b}: {c} ({d}%)',
                fontSize: 13,
                fontWeight: 500,
              }
            : {
                show: false,
              },
          emphasis: {
            scale: true,
            scaleSize: 8,
            label: {
              show: false,
            },
          },
          labelLine: {
            show: props.showLabel,
            length: 16,
            length2: 14,
            smooth: false,
          },
          data: getSeriesData(),
          ...props.options,
        },
      ],
    }

    chartInstance.setOption(options)
    bindChartEvents()
  }

  const resizeChart = () => {
    updateViewportSize()
    chartInstance?.resize()
    clearFocusOverlay(false)
    updateChart()
  }

  const initChart = () => {
    if (!chartRef.value) {
      return
    }

    chartInstance = echarts.init(chartRef.value)
    updateChart()
  }

  onMounted(() => {
    initChart()
    window.addEventListener('resize', resizeChart)
  })

  onUnmounted(() => {
    window.removeEventListener('resize', resizeChart)
    clearFocusOverlay(false)
    chartInstance?.dispose()
    chartInstance = null
  })

  watch(
    () => props.data,
    () => {
      clearFocusOverlay(false)
      updateChart()
    },
    { deep: true },
  )

  watch(
    () => props.height,
    () => {
      setTimeout(() => resizeChart(), 100)
    },
  )
</script>

<style scoped>
  .pie-chart-wrapper {
    width: 100%;
    height: v-bind(height);
    display: flex;
    flex-direction: column;
    overflow: visible;
  }

  .pie-chart-container {
    width: 100%;
    flex: 1;
    min-height: 0;
    overflow: visible;
  }

  .pie-chart-legend {
    display: flex;
    flex-wrap: wrap;
    justify-content: center;
    gap: 10px 20px;
    padding-top: 14px;
    margin-top: 8px;
    border-top: 1px solid #f1f5f9;
    flex-shrink: 0;
  }

  .pie-chart-legend-item {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    color: #4b5563;
    font-size: 14px;
    font-weight: 500;
    line-height: 1;
    transition:
      color 0.2s ease,
      transform 0.2s ease;
  }

  .pie-chart-legend-item.is-active {
    color: #111827;
    transform: translateY(-1px);
  }

  .pie-chart-legend-swatch {
    width: 12px;
    height: 12px;
    border-radius: 999px;
    flex-shrink: 0;
  }

  .pie-chart-legend-text {
    white-space: nowrap;
  }

  .pie-chart-focus-overlay {
    position: fixed;
    inset: 0;
    z-index: 9999;
    pointer-events: none;
    overflow: visible;
  }

  .pie-chart-focus-line {
    fill: none;
    stroke-width: 5;
    stroke-linecap: round;
    stroke-linejoin: round;
  }

  .pie-chart-focus-label {
    fill: #111827;
    stroke: rgba(255, 255, 255, 0.98);
    stroke-width: 12px;
    paint-order: stroke fill;
    stroke-linejoin: round;
    font-size: 30px;
    font-weight: 900;
    letter-spacing: -0.03em;
  }
</style>
