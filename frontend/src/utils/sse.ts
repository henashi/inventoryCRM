/**
 * SSE 流式聊天客户端
 *
 * 解析后端 /api/ai/chat/stream 的 Server-Sent Events 响应。
 * 使用 fetch + ReadableStream，不依赖 EventSource（支持 POST + Body）。
 */

export interface SseCallbacks {
  /** 状态更新（"正在理解你的问题…" / "正在查询数据…" / "正在生成回答…"） */
  onStatus?: (status: string) => void
  /** 收到一个 token（逐字追加到消息气泡） */
  onToken?: (token: string) => void
  /** 流完成 */
  onDone?: (fallback: boolean) => void
  /** 出错 */
  onError?: (message: string) => void
}

/**
 * 发送 SSE 聊天请求
 *
 * @param message 用户消息
 * @param history 历史消息（不含当前消息）
 * @param callbacks 事件回调
 * @returns 取消函数（调用可中断请求）
 */
export function streamChat(
  message: string,
  history: { role: string; content: string }[],
  callbacks: SseCallbacks,
): () => void {
  const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'
  const url = `${baseUrl}/ai/chat/stream`

  const controller = new AbortController()

  const body = JSON.stringify({ message, history })

  fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      // 附加 JWT（与 axios 拦截器同理）
      Authorization: `Bearer ${localStorage.getItem('token') || ''}`,
    },
    body,
    signal: controller.signal,
  })
    .then(async (response) => {
      if (!response.ok) {
        callbacks.onError?.(`请求失败 (${response.status})`)
        return
      }

      const reader = response.body?.getReader()
      if (!reader) {
        callbacks.onError?.('浏览器不支持流式读取')
        return
      }

      const decoder = new TextDecoder()
      let buffer = ''

      try {
        while (true) {
          const { done, value } = await reader.read()
          if (done) break

          buffer += decoder.decode(value, { stream: true })

          // 解析 SSE 事件（可能跨 chunk）
          const lines = buffer.split('\n')
          buffer = lines.pop() || '' // 最后一个可能不完整，保留到下次

          let currentEvent = ''
          for (const line of lines) {
            if (line.startsWith('event: ')) {
              currentEvent = line.slice(7).trim()
            } else if (line.startsWith('data: ')) {
              const data = line.slice(6).trim()
              handleEvent(currentEvent, data, callbacks)
            }
            // 空行 = 事件结束，重置 event 名
            if (line.trim() === '') {
              currentEvent = ''
            }
          }
        }

        // 处理 buffer 中可能残留的最后一个事件
        if (buffer.trim()) {
          const lines = buffer.split('\n')
          for (const line of lines) {
            if (line.startsWith('event: ')) {
              // 无后续 data，忽略
            } else if (line.startsWith('data: ')) {
              handleEvent('', line.slice(6).trim(), callbacks)
            }
          }
        }

        // 流正常结束但没收到 done 事件
        callbacks.onDone?.(true)
      } catch (err: any) {
        if (err.name !== 'AbortError') {
          callbacks.onError?.(err.message || '流式读取异常')
        }
      } finally {
        reader.releaseLock()
      }
    })
    .catch((err) => {
      if (err.name !== 'AbortError') {
        callbacks.onError?.(err.message || '网络连接失败')
      }
    })

  return () => controller.abort()
}

function handleEvent(event: string, data: string, callbacks: SseCallbacks) {
  switch (event) {
    case 'status':
      callbacks.onStatus?.(data)
      break
    case 'intent':
      // 意图识别结果，目前前端不需要处理
      break
    case 'token':
      callbacks.onToken?.(data)
      break
    case 'done':
      try {
        const payload = JSON.parse(data)
        callbacks.onDone?.(!!payload.fallback)
      } catch {
        callbacks.onDone?.(false)
      }
      break
    case 'error':
      callbacks.onError?.(data)
      break
    default:
      // 无 event 名称的 data 行，当成 token 处理
      if (!event && data) {
        callbacks.onToken?.(data)
      }
      break
  }
}
