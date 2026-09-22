# Emergency Knowledge Assistant

安全应急场景的全栈 RAG 知识问答原型，包含 React 流式界面、Spring WebFlux 网关和 Python FastAPI RAG 服务。

## Features

- Markdown/TXT 文档加载、递归切分与批量 Embedding
- Elasticsearch `dense_vector` + kNN 检索与相似度阈值过滤
- GLM/Mock Provider、约束 Prompt、引用来源和无结果拒答
- FastAPI SSE 生成与 Spring WebFlux 流式透传
- Sentinel 资源限流与 Redisson 集群共享速率限制
- 时间戳 + nonce 防重放、分布式锁防重复提交、Redis 输入护栏
- RocketMQ Producer 与 Python 直连降级
- Docker Compose、Kubernetes Deployment/Service、CPU HPA 示例

## Architecture

```text
React :5173
  -> Spring WebFlux Gateway :8080
  -> FastAPI RAG Service :8000
  -> Elasticsearch / GLM
```

问答链路：

```text
Question
  -> Gateway guardrails and rate limiting
  -> Query embedding
  -> Elasticsearch kNN retrieval
  -> Similarity threshold
  -> Prompt with retrieved context
  -> Streaming answer and citations
```

## Tech Stack

| Layer | Technology |
|---|---|
| Frontend | React, Vite, TypeScript, EventSource |
| Gateway | Spring Boot, Spring WebFlux, Sentinel, Redisson |
| RAG service | Python, FastAPI, LangChain |
| Retrieval | Elasticsearch dense vector and kNN |
| Model | GLM or local Mock Provider |
| Middleware | Redis, RocketMQ |
| Deployment | Docker Compose, Kubernetes, HPA, Prometheus config |

## Quick Start

### 1. Configure environment

```bash
cp .env.example .env
```

Without `GLM_API_KEY`, `RUN_MODE=auto` uses the Mock providers.

### 2. Start middleware

```bash
docker compose up -d
```

### 3. Start the RAG service

```bash
cd rag-service
python -m venv .venv
```

Linux/macOS:

```bash
source .venv/bin/activate
pip install -r requirements.txt
python -m uvicorn app.main:app --port 8000
```

Windows PowerShell:

```powershell
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
python -m uvicorn app.main:app --port 8000
```

### 4. Start the gateway

```bash
cd gateway-service
mvn spring-boot:run
```

### 5. Start the frontend

```bash
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173`.

### 6. Ingest sample documents

```bash
curl -X POST http://localhost:8000/api/ingest \
  -H "Content-Type: application/json" \
  -d '{}'
```

## API

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/health` | RAG service health and run mode |
| POST | `/api/chat` | Python SSE chat endpoint |
| GET | `/api/chat` | Gateway SSE chat endpoint for EventSource |
| POST | `/api/ingest` | Load and index local documents |
| GET | `/api/evaluate` | Run the local evaluation set |

## Current Scope

- Retrieval currently uses kNN only; BM25 fusion and reranking are not included.
- The evaluation module contains lightweight lexical proxy metrics rather than the complete RAGAS framework.
- The Java RocketMQ Producer is available, while the Python consumer remains a local extension point; direct ingestion is the complete runnable path.
- Kubernetes HPA uses CPU utilization. Prometheus files provide scrape configuration examples.
