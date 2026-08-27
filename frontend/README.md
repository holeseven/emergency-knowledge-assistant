# Emergency Knowledge Assistant - 前端

React + Vite + TypeScript 实现的 SSE 流式聊天界面。Author: lxy

## 启动

```bash
npm install
npm run dev   # 访问 http://localhost:5173
```

需先启动后端网关（localhost:8080）与 Python RAG 服务（localhost:8000）。

## 说明

- `src/api/chat.ts`：通过 EventSource 连接网关 SSE 接口，逐 token 接收流式回答
- `src/components/ChatWindow.tsx`：聊天窗口，流式打字机效果 + 引用来源展示 + 多轮对话
- Vite dev server 已配置 `/api` 代理到网关 `localhost:8080`
