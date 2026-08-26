# Author: lxy
"""FastAPI 入口 - 安全应急智能问答 RAG 客服系统"""
import json
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from sse_starlette.sse import EventSourceResponse
from app.schemas import ChatRequest, IngestRequest
from app.rag.pipeline import ingest_documents, query
from app.config import get_settings

app = FastAPI(title="安全应急 RAG 客服系统", version="1.0.0")
app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_credentials=True, allow_methods=["*"], allow_headers=["*"])

_chat_history: dict = {}


@app.get("/api/health")
async def health():
    settings = get_settings()
    return {"status": "ok", "mode": "mock" if settings.use_mock else "real"}


@app.post("/api/chat")
async def chat(request: ChatRequest):
    session_id = request.session_id or "default"
    history = _chat_history.get(session_id, [])

    async def event_generator():
        full_response = ""
        async for event in query(request.question, history):
            yield {"data": json.dumps(event, ensure_ascii=False)}
            if event["type"] == "token":
                full_response += event["content"]
        history.append({"role": "user", "content": request.question})
        history.append({"role": "assistant", "content": full_response})
        _chat_history[session_id] = history[-20:]

    return EventSourceResponse(event_generator())


@app.post("/api/ingest")
async def ingest(request: IngestRequest = None):
    data_dir = request.data_dir if request and request.data_dir else None
    count = ingest_documents(data_dir)
    return {"status": "ok", "ingested_chunks": count}


@app.get("/api/evaluate")
async def evaluate():
    try:
        from app.evaluation.evaluator import run_evaluation
        return run_evaluation()
    except Exception as e:
        return {"status": "error", "message": str(e)}
