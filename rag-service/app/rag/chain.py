# Author: lxy
"""RAG Chain - 流式生成 + 防幻觉"""
from typing import AsyncGenerator, List
from langchain_core.documents import Document
from app.providers.llm import get_llm
from app.rag.retriever import retrieve

SYSTEM_PROMPT = """你是安全应急领域的专家客服。严格遵守以下规则：
1. 仅根据下方【参考资料】回答
2. 资料中没有相关内容时回答"抱歉，知识库中暂未收录该问题"
3. 严禁编造不在参考资料中的信息
4. 回答末尾标注引用来源"""

FALLBACK = "抱歉，知识库中暂未收录该问题，请联系人工客服。"


async def rag_chat_stream(question: str, history: List[dict] = None) -> AsyncGenerator[dict, None]:
    docs = retrieve(question)
    if not docs:
        yield {"type": "token", "content": FALLBACK}
        yield {"type": "done", "content": ""}
        return

    context_parts = []
    sources = set()
    for doc in docs:
        context_parts.append(doc.page_content)
        sources.add(doc.metadata.get("source", "未知"))
    context = "\n---\n".join(context_parts)

    messages = [{"role": "system", "content": SYSTEM_PROMPT}]
    if history:
        messages.extend(history[-10:])
    messages.append({"role": "user", "content": f"【参考资料】\n{context}\n\n【用户问题】\n{question}"})

    llm = get_llm()
    try:
        async for chunk in llm.astream(messages):
            if chunk.content:
                yield {"type": "token", "content": chunk.content}
    except Exception as e:
        yield {"type": "token", "content": f"生成出错: {e}"}

    for s in sources:
        yield {"type": "citation", "content": s}
    yield {"type": "done", "content": ""}
