<template>
  <div class="chat-page">
    <div class="chat-container">
      <!-- 头部 -->
      <div class="chat-header">
        <div class="chat-header-info">
          <h1 class="chat-title">💬 AI 运营助手</h1>
          <span class="chat-subtitle">用自然语言查询库存、客户、礼品和统计数据</span>
        </div>
        <a-space>
          <a-button size="small" @click="goBack">← 返回</a-button>
          <a-button size="small" @click="clearMessages">清除对话</a-button>
        </a-space>
      </div>

      <!-- 消息列表 -->
      <div class="chat-messages" ref="messagesRef">
        <div v-if="messages.length === 0" class="chat-empty">
          <div class="empty-icon">🤖</div>
          <div class="empty-title">有什么可以帮你的？</div>
          <div class="empty-suggestions">
            <a-tag
              v-for="q in suggestedQuestions"
              :key="q"
              class="suggestion-tag"
              @click="sendMessage(q)"
            >
              {{ q }}
            </a-tag>
          </div>
        </div>

        <div
          v-for="(msg, index) in messages"
          :key="index"
          class="message-row"
          :class="msg.role"
        >
          <div class="message-avatar">
            {{ msg.role === 'user' ? '👤' : '🤖' }}
          </div>
          <div class="message-bubble">
            <div class="message-text">{{ stripMarkdown(msg.content) }}</div>
            <div v-if="msg.role === 'assistant' && msg.fallback" class="message-badge">
              离线模式
            </div>
          </div>
        </div>

        <!-- 加载中 -->
        <div v-if="loading" class="message-row assistant">
          <div class="message-avatar">🤖</div>
          <div class="message-bubble loading-bubble">
            <a-spin size="small" />
            <span class="loading-text">思考中...</span>
          </div>
        </div>
      </div>

      <!-- 输入区 -->
      <div class="chat-input-area">
        <a-input
          v-model:value="inputText"
          placeholder="输入你的问题，例如：哪些商品库存不足？"
          size="large"
          @pressEnter="handleSend"
          @keydown="handleKeyDown"
          :disabled="loading"
        />
        <a-button
          type="primary"
          size="large"
          :loading="loading"
          @click="handleSend"
          class="send-btn"
        >
          发送
        </a-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { aiApi } from '@/api/ai'

interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
  fallback?: boolean
}

const router = useRouter()
const inputText = ref('')
const messages = ref<ChatMessage[]>([])
const loading = ref(false)
const messagesRef = ref<HTMLElement>()
const messageHistory = ref<string[]>([])
const historyIndex = ref(-1)

const suggestedQuestions = [
  '哪些商品库存不足？',
  '最近新增的客户',
  '有哪些礼品可以发放？',
  '上月出库总量是多少',
  '近7天有哪些入库记录',
  '当前总共有多少商品',
]

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

const sendMessage = async (text: string) => {
  await handleSendWithText(text)
}

const handleSendWithText = async (text: string) => {
  if (!text || loading.value) return
  messageHistory.value.push(text)
  historyIndex.value = -1
  messages.value.push({ role: 'user', content: text })
  inputText.value = ''
  scrollToBottom()
  await doChat(text)
}

const handleSend = async () => {
  await handleSendWithText(inputText.value.trim())
}

const doChat = async (text: string) => {
  loading.value = true
  try {
    const history = messages.value.slice(-10).map(m => ({ role: m.role === 'user' ? 'user' : 'assistant', content: m.content }))
    const response = await aiApi.chat(text, history)
    messages.value.push({
      role: 'assistant',
      content: stripMarkdown(response.reply),
      fallback: response.fallback,
    })
  } catch {
    messages.value.push({
      role: 'assistant',
      content: '抱歉，我暂时无法回答这个问题，请稍后再试。',
    })
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

const handleKeyDown = (e: KeyboardEvent) => {
  if (e.key === 'ArrowUp') {
    e.preventDefault()
    if (messageHistory.value.length === 0) return
    const newIndex = historyIndex.value === -1
      ? messageHistory.value.length - 1
      : Math.max(0, historyIndex.value - 1)
    historyIndex.value = newIndex
    inputText.value = messageHistory.value[newIndex]
  } else if (e.key === 'ArrowDown') {
    e.preventDefault()
    if (historyIndex.value === -1) return
    const newIndex = historyIndex.value + 1
    if (newIndex >= messageHistory.value.length) {
      historyIndex.value = -1
      inputText.value = ''
    } else {
      historyIndex.value = newIndex
      inputText.value = messageHistory.value[newIndex]
    }
  }
}

const goBack = () => {
  router.back()
}

const stripMarkdown = (text: string) => text.replace(/\*\*/g, '')

const clearMessages = () => {
  messages.value = []
}
</script>

<style scoped>
.chat-page {
  height: calc(100vh - 48px);
  background: #f5f7fa;
  display: flex;
  justify-content: center;
  padding: 16px;
}

.chat-container {
  width: 100%;
  max-width: 800px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
}
.chat-title { font-size: 18px; font-weight: 600; margin: 0; }
.chat-subtitle { font-size: 13px; color: #9ca3af; margin-left: 8px; }

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.chat-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  color: #9ca3af;
}
.empty-icon { font-size: 48px; }
.empty-title { font-size: 18px; font-weight: 500; color: #6b7280; }
.empty-suggestions { display: flex; flex-wrap: wrap; gap: 8px; justify-content: center; max-width: 500px; }
.suggestion-tag {
  cursor: pointer;
  padding: 4px 12px;
  font-size: 13px;
  border-radius: 16px;
  transition: all 0.2s;
}
.suggestion-tag:hover { transform: scale(1.05); }

.message-row {
  display: flex;
  gap: 12px;
  max-width: 85%;
}
.message-row.user { align-self: flex-end; flex-direction: row-reverse; }
.message-row.assistant { align-self: flex-start; }

.message-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
  background: #f0f0f0;
}

.message-bubble {
  padding: 12px 16px;
  border-radius: 12px;
  line-height: 1.6;
  font-size: 14px;
  white-space: pre-wrap;
  word-break: break-word;
}
.user .message-bubble {
  background: #1890ff;
  color: #fff;
  border-bottom-right-radius: 4px;
}
.assistant .message-bubble {
  background: #f5f7fa;
  color: #1f2937;
  border-bottom-left-radius: 4px;
}

.message-badge {
  display: inline-block;
  font-size: 11px;
  color: #faad14;
  margin-top: 4px;
}

.loading-bubble {
  display: flex;
  align-items: center;
  gap: 8px;
}
.loading-text { color: #9ca3af; font-size: 13px; }

.chat-input-area {
  display: flex;
  gap: 8px;
  padding: 16px 24px;
  border-top: 1px solid #f0f0f0;
  flex-shrink: 0;
}
.chat-input-area :deep(.ant-input) { border-radius: 8px; }
.send-btn { border-radius: 8px; }
</style>
