// Author: lxy
import ChatWindow from './components/ChatWindow'

export default function App() {
  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-100 to-blue-50 flex items-center justify-center p-4">
      <div className="w-full max-w-3xl">
        <div className="bg-gradient-to-r from-blue-600 to-indigo-600 text-white rounded-t-2xl px-6 py-4 shadow-lg">
          <h1 className="text-xl font-bold">安全应急智能问答助手</h1>
          <p className="text-sm text-blue-100 mt-1">基于 RAG 检索增强的安全应急知识库问答系统</p>
        </div>
        <ChatWindow />
      </div>
    </div>
  )
}
