// Author: lxy
import { useState, useRef, useEffect } from 'react'
import { streamChat } from '../api/chat'

interface Message {
  role: 'user' | 'assistant'
  content: string
  citations?: string[]
}

const EXAMPLES = ['网络安全事件多久要上报？', '突发事件如何分级？', '危化品泄漏如何处置？']

export default function ChatWindow() {
  const [messages, setMessages] = useState<Message[]>([])
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(false)
  const [sessionId] = useState(() => 'session-' + Date.now())
  const bottomRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  const send = (q: string) => {
    if (!q.trim() || loading) return
    setMessages((m) => [
      ...m,
      { role: 'user', content: q },
      { role: 'assistant', content: '', citations: [] },
    ])
    setInput('')
    setLoading(true)
    streamChat(q, sessionId, {
      onToken: (c) =>
        setMessages((m) => {
          const copy = [...m]
          const last = copy[copy.length - 1]
          copy[copy.length - 1] = { ...last, content: last.content + c }
          return copy
        }),
      onCitation: (s) =>
        setMessages((m) => {
          const copy = [...m]
          const last = copy[copy.length - 1]
          copy[copy.length - 1] = { ...last, citations: [...(last.citations || []), s] }
          return copy
        }),
      onDone: () => setLoading(false),
      onError: (err) => {
        setMessages((m) => {
          const copy = [...m]
          const last = copy[copy.length - 1]
          copy[copy.length - 1] = { ...last, content: last.content || '[' + err + ']' }
          return copy
        })
        setLoading(false)
      },
    })
  }

  return (
    <div className="bg-white rounded-b-2xl shadow-xl flex flex-col h-[600px]">
      <div className="flex-1 overflow-y-auto p-6 space-y-4">
        {messages.length === 0 && (
          <div className="text-center text-gray-500 mt-8">
            <p className="mb-4">您好，我是安全应急智能问答助手，请问有什么可以帮您？</p>
            <div className="flex flex-col gap-2 items-center">
              {EXAMPLES.map((ex) => (
                <button
                  key={ex}
                  onClick={() => send(ex)}
                  className="px-4 py-2 bg-blue-50 text-blue-600 rounded-full text-sm hover:bg-blue-100 transition"
                >
                  {ex}
                </button>
              ))}
            </div>
          </div>
        )}
        {messages.map((msg, i) => (
          <div key={i} className={'flex ' + (msg.role === 'user' ? 'justify-end' : 'justify-start')}>
            <div
              className={
                'max-w-[75%] px-4 py-3 rounded-2xl ' +
                (msg.role === 'user' ? 'bg-blue-600 text-white' : 'bg-gray-100 text-gray-800 shadow')
              }
            >
              <div className="whitespace-pre-wrap break-words">
                {msg.content || (loading && i === messages.length - 1 ? '思考中...' : '')}
              </div>
              {msg.citations && msg.citations.length > 0 && (
                <div className="mt-2 flex flex-wrap gap-1">
                  {msg.citations.map((c, j) => (
                    <span key={j} className="text-xs bg-white text-gray-500 px-2 py-1 rounded border">
                      📎 引用: {c}
                    </span>
                  ))}
                </div>
              )}
            </div>
          </div>
        ))}
        <div ref={bottomRef} />
      </div>
      <div className="border-t p-4 flex gap-2">
        <input
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter') send(input)
          }}
          placeholder="请输入您的安全应急问题..."
          disabled={loading}
          className="flex-1 border rounded-xl px-4 py-2 focus:outline-none focus:ring-2 focus:ring-blue-400"
        />
        <button
          onClick={() => send(input)}
          disabled={loading}
          className="bg-blue-600 text-white px-6 py-2 rounded-xl hover:bg-blue-700 disabled:opacity-50 transition"
        >
          发送
        </button>
      </div>
    </div>
  )
}
