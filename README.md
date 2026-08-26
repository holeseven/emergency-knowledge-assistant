# 安全应急智能问答 RAG 客服系统

> 基于信通院安全应急场景的企业级 RAG 智能客服系统，全栈真实运行。
> Author: lxy

## 技术栈

| 层 | 技术 |
|---|------|
| 前端 | React + Vite + TypeScript（SSE 流式聊天） |
| 网关 | Spring Boot + Cloud Gateway + WebFlux + Sentinel |
| 治理 | Redis + Redisson（限流/防重/护栏）、RocketMQ（异步削峰） |
| RAG | Python FastAPI + LangChain + Elasticsearch（混合检索） |
| 大模型 | 智谱 GLM（流式生成 + embedding） |
| 评估 | RAGAS（忠实度/相关性/精确度） |
| 部署 | Docker Compose + K8s manifests |

## Quick Start

```bash
# 1. 复制环境配置
cp .env.example .env
# 编辑 .env 填入 GLM API Key

# 2. 一键启动中间件
chmod +x start-all.sh
./start-all.sh

# 3. 启动各服务（见 start-all.sh 输出提示）
```

## 项目结构

- `rag-service/` — Python RAG 核心服务
- `gateway-service/` — Java 网关与治理层
- `frontend/` — React 聊天前端
- `data/` — 安全应急语料与评估数据集
- `deploy/` — K8s 部署清单
- `docs/` — 架构设计、技术选型、面试问答文档
