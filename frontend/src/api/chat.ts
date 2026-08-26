// Author: lxy
// SSE 流式聊天 API：连接后端网关的 /api/chat 接口，逐 token 接收流式回答
export interface ChatCallbacks {
  onToken: (content: string) => void
  onCitation: (source: string) => void
  onDone: () => void
  onError: (err: string) => void
}

export function streamChat(question: string, sessionId: string, cb: ChatCallbacks): () => void {
  const url = `/api/chat?question=${encodeURIComponent(question)}&sessionId=${encodeURIComponent(sessionId)}`
  const es = new EventSource(url)
  es.onmessage = (e) => {
    try {
      const data = JSON.parse(e.data)
      if (data.type === 'token') cb.onToken(data.content)
      else if (data.type === 'citation') cb.onCitation(data.content)
      else if (data.type === 'done') { cb.onDone(); es.close() }
    } catch {
      // 忽略无法解析的事件
    }
  }
  es.onerror = () => { cb.onError('连接中断或服务不可用'); es.close() }
  return () => es.close()
}
