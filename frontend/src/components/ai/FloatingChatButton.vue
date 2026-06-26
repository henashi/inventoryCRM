<template>
  <div ref="containerRef">
    <!-- 气泡弹窗 -->
    <div v-if="isOpen" class="fab-popup" :style="popupStyle">
      <div class="fab-popup-header">
        <span>✦ AI 助手</span>
        <a-button type="text" size="small" @click="closePopup">✕</a-button>
      </div>
      <div class="fab-popup-body" ref="fabMessagesRef">
        <div v-if="fabMessages.length === 0" class="fab-empty">
          <div class="fab-empty-text">有什么可以帮你的？</div>
          <div class="fab-suggestions">
            <a-tag v-for="q in suggestedQuestions" :key="q" class="fab-suggestion" @click="fabSend(q)">{{ q }}</a-tag>
          </div>
        </div>
        <div v-for="(msg, i) in fabMessages" :key="i" class="fab-msg" :class="msg.role">
          <div class="fab-bubble">{{ stripMarkdown(msg.content) }}</div>
        </div>
        <div v-if="fabLoading" class="fab-msg assistant">
          <div class="fab-bubble fab-loading">思考中...</div>
        </div>
      </div>
      <div class="fab-popup-footer">
        <a-input
          v-model:value="fabInput"
          placeholder="输入问题..."
          size="small"
          @pressEnter="fabSend(fabInput)"
          @keydown="handleFabKeyDown"
        />
        <a-button type="primary" size="small" :loading="fabLoading" @click="fabSend(fabInput)">发送</a-button>
      </div>
    </div>

    <!-- 悬浮球 -->
    <a-tooltip title="AI 助手" placement="left">
    <div
      class="fab-button"
      :style="{ transform: `translate(${posX}px, ${posY}px)` }"
      @mousedown="startDrag"
      @click="togglePopup"
    >
      <span v-if="unreadCount > 0" class="fab-badge">{{ unreadCount }}</span>
      ✦
    </div>
    
    </a-tooltip>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted, onUnmounted } from 'vue'
import { aiApi } from '@/api/ai'

const isOpen = ref(false)
const fabInput = ref('')
const fabMessages = ref<{ role: string; content: string }[]>([])
const fabLoading = ref(false)
const unreadCount = ref(0)
const fabMessagesRef = ref<HTMLElement>()
const containerRef = ref<HTMLElement>()
const fabHistory = ref<string[]>([])
const fabHistoryIndex = ref(-1)

// 拖拽状态
const posX = ref(0)
const posY = ref(0)
const dragging = ref(false)
const dragStartX = ref(0)
const dragStartY = ref(0)
const dragOriginX = ref(0)
const dragOriginY = ref(0)

// 弹窗位置（固定在按钮正上方）
const popupStyle = {
  position: 'fixed' as const,
  bottom: (80 + Math.abs(posY.value)) + 'px',
  right: (20 - posX.value) + 'px',
}

const suggestedQuestions = [
  '哪些商品库存不足？',
  '最近新增的客户',
  '有哪些礼品可以发放？',
  '上月出库总量是多少',
]

// 点击外部关闭
const handleClickOutside = (e: MouseEvent) => {
  if (!isOpen.value) return
  const el = containerRef.value
  if (el && !el.contains(e.target as Node)) {
    closePopup()
  }
}

onMounted(() => document.addEventListener('click', handleClickOutside))
onUnmounted(() => document.removeEventListener('click', handleClickOutside))

let dragDistance = 0

const startDrag = (e: MouseEvent) => {
  dragging.value = true
  dragDistance = 0
  dragStartX.value = e.clientX
  dragStartY.value = e.clientY
  dragOriginX.value = posX.value
  dragOriginY.value = posY.value
  document.addEventListener('mousemove', onDrag)
  document.addEventListener('mouseup', stopDrag)
}

const onDrag = (e: MouseEvent) => {
  if (!dragging.value) return
  posX.value = dragOriginX.value + e.clientX - dragStartX.value
  posY.value = dragOriginY.value + e.clientY - dragStartY.value
  dragDistance = Math.abs(e.clientX - dragStartX.value) + Math.abs(e.clientY - dragStartY.value)
}

const stripMarkdown = (text: string) => text.replace(/\*\*/g, '')

const stopDrag = () => {
  dragging.value = false
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', stopDrag)
}

const scrollFab = () => {
  nextTick(() => {
    if (fabMessagesRef.value) {
      fabMessagesRef.value.scrollTop = fabMessagesRef.value.scrollHeight
    }
  })
}

const togglePopup = () => {
  if (dragging.value || dragDistance > 5) return
  isOpen.value = !isOpen.value
  if (isOpen.value) unreadCount.value = 0
}

const closePopup = () => { isOpen.value = false }

const handleFabKeyDown = (e: KeyboardEvent) => {
  if (e.key === 'ArrowUp') {
    e.preventDefault()
    if (fabHistory.value.length === 0) return
    const idx = fabHistoryIndex.value === -1 ? fabHistory.value.length - 1 : Math.max(0, fabHistoryIndex.value - 1)
    fabHistoryIndex.value = idx
    fabInput.value = fabHistory.value[idx]
  } else if (e.key === 'ArrowDown') {
    e.preventDefault()
    if (fabHistoryIndex.value === -1) return
    const idx = fabHistoryIndex.value + 1
    if (idx >= fabHistory.value.length) {
      fabHistoryIndex.value = -1
      fabInput.value = ''
    } else {
      fabHistoryIndex.value = idx
      fabInput.value = fabHistory.value[idx]
    }
  }
}

const fabSend = async (text: string) => {
  const msg = (typeof text === 'string' ? text : fabInput.value).trim()
  if (!msg || fabLoading.value) return
  fabHistory.value.push(msg)
  fabHistoryIndex.value = -1
  fabInput.value = ''
  fabMessages.value.push({ role: 'user', content: msg })
  scrollFab()
  fabLoading.value = true
  try {
    const history = fabMessages.value.slice(-10).map(m => ({ role: m.role === 'user' ? 'user' : 'assistant', content: m.content }))
    const res = await aiApi.chat(msg, history)
    fabMessages.value.push({ role: 'assistant', content: stripMarkdown(res.reply) })
    if (!isOpen.value) unreadCount.value++
  } catch {
    fabMessages.value.push({ role: 'assistant', content: '暂时无法回答，请稍后再试。' })
  } finally {
    fabLoading.value = false
    scrollFab()
  }
}
</script>

<style scoped>
/* 悬浮球 */
.fab-button {
  position: fixed;
  bottom: 30px;
  right: 20px;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: linear-gradient(135deg, #1890ff, #096dd9);
  color: #fff;
  font-size: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 14px rgba(24,144,255,0.4);
  cursor: pointer;
  z-index: 1060;
  user-select: none;
  transition: box-shadow 0.2s, transform 0.2s;
}
.fab-button:hover { box-shadow: 0 6px 24px rgba(24,144,255,0.6); }
.fab-badge {
  position: absolute;
  top: -4px;
  right: -4px;
  background: #f5222d;
  color: #fff;
  font-size: 11px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
}

/* 气泡弹窗 */
.fab-popup {
  position: fixed;
  bottom: 152px;
  right: 20px;
  width: 360px;
  height: 480px;
  background: var(--bg-card);
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.15);
  display: flex;
  flex-direction: column;
  z-index: 1050;
  overflow: hidden;
  animation: popIn 0.25s ease-out;
}
@keyframes popIn {
  from { opacity: 0; transform: scale(0.9) translateY(10px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}

.fab-popup-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 16px;
  border-bottom: 1px solid var(--border-color);
  font-weight: 600;
  font-size: 15px;
  flex-shrink: 0;
}
.fab-popup-body {
  flex: 1;
  overflow-y: auto;
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.fab-popup-footer {
  display: flex;
  gap: 6px;
  padding: 10px 16px;
  border-top: 1px solid var(--border-color);
  flex-shrink: 0;
}

.fab-empty { text-align: center; padding-top: 60px; color: var(--text-tertiary); }
.fab-empty-text { font-size: 15px; margin-bottom: 12px; }
.fab-suggestions { display: flex; flex-wrap: wrap; gap: 6px; justify-content: center; }
.fab-suggestion { cursor: pointer; font-size: 12px; border-radius: 12px; }
.fab-msg { display: flex; }
.fab-msg.user { justify-content: flex-end; }
.fab-bubble {
  max-width: 85%;
  padding: 8px 12px;
  border-radius: 10px;
  font-size: 13px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
}
.user .fab-bubble { background: #1890ff; color: #fff; border-bottom-right-radius: 2px; }
.assistant .fab-bubble { background: var(--bg-chat-message); color: var(--text-primary); border-bottom-left-radius: 2px; }
.fab-loading { color: #9ca3af; }
</style>
