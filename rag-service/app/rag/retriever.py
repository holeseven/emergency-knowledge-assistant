# Author: lxy
"""检索器 - 相似度阈值过滤"""
from typing import List
from langchain_core.documents import Document
from app.config import get_settings
from app.rag.es_store import get_es_store


def retrieve(query: str, top_k: int = 5) -> List[Document]:
    settings = get_settings()
    store = get_es_store()
    results = store.similarity_search(query, top_k=top_k)
    filtered = []
    for r in results:
        if r["score"] >= settings.similarity_threshold:
            filtered.append(Document(page_content=r["content"], metadata=r["metadata"]))
    return filtered
