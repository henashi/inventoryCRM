import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'

const componentSource = readFileSync(new URL('./AiAssistant.vue', import.meta.url), 'utf-8')

describe('AiAssistant layout', () => {
  it('renders the chat page container', () => {
    expect(componentSource).toContain('class="chat-page"')
    expect(componentSource).toContain('class="chat-container"')
  })

  it('renders chat header with title and subtitle', () => {
    expect(componentSource).toContain('class="chat-header"')
    expect(componentSource).toContain('class="chat-header-info"')
    expect(componentSource).toContain('AI 运营助手')
    expect(componentSource).toContain('用自然语言查询库存、客户、礼品和统计数据')
  })

  it('renders header action buttons: 返回 and 清除对话', () => {
    expect(componentSource).toContain('@click="goBack"')
    expect(componentSource).toContain('@click="clearMessages"')
    expect(componentSource).toContain('清除对话')
  })

  it('renders chat messages area with empty state', () => {
    expect(componentSource).toContain('class="chat-messages"')
    expect(componentSource).toContain('class="chat-empty"')
    expect(componentSource).toContain('class="empty-title"')
    expect(componentSource).toContain('有什么可以帮你的？')
    expect(componentSource).toContain('class="empty-suggestions"')
  })

  it('renders suggested questions tags', () => {
    expect(componentSource).toContain('哪些商品库存不足？')
    expect(componentSource).toContain('最近新增的客户')
    expect(componentSource).toContain('有哪些礼品可以发放？')
    expect(componentSource).toContain('上月出库总量是多少')
    expect(componentSource).toContain('近7天有哪些入库记录')
    expect(componentSource).toContain('当前总共有多少商品')
  })

  it('renders message bubbles for user and assistant roles', () => {
    expect(componentSource).toContain('class="message-row"')
    expect(componentSource).toContain('class="message-avatar"')
    expect(componentSource).toContain('class="message-bubble"')
    expect(componentSource).toContain('class="message-text"')
    expect(componentSource).toContain("msg.role === 'user'")
    expect(componentSource).toContain("msg.role === 'assistant'")
  })

  it('renders streaming status area', () => {
    expect(componentSource).toContain('v-if="streaming"')
    expect(componentSource).toContain('streaming-bubble')
    expect(componentSource).toContain('class="streaming-status"')
    expect(componentSource).toContain('class="cursor-blink"')
  })

  it('renders chat input area with send/stop buttons', () => {
    expect(componentSource).toContain('class="chat-input-area"')
    expect(componentSource).toContain('v-model:value="inputText"')
    expect(componentSource).toContain('@click="handleSend"')
    expect(componentSource).toContain('@click="stopStreaming"')
  })

  it('uses css animation for cursor blink', () => {
    expect(componentSource).toContain('@keyframes blink')
    expect(componentSource).toContain('animation: blink 0.8s step-end infinite')
  })

  it('renders animated pop-in for messages', () => {
    expect(componentSource).toContain('message-badge')
    expect(componentSource).toContain('fallback')
    expect(componentSource).toContain('离线模式')
  })
})
