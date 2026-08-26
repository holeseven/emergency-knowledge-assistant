# Author: lxy
"""Pipeline - 入库 + 问答编排"""
from typing import List, AsyncGenerator
from app.rag.loader import load_documents
from app.rag.splitter import split_documents
from app.rag.es_store import get_es_store
from app.rag.chain import rag_chat_stream


def ingest_documents(data_dir: str = None) -> int:
    documents = load_documents(data_dir)
    if not documents:
        return 0
    chunks = split_documents(documents)
    store = get_es_store()
    return store.add_documents(chunks)


async def query(question: str, history: List[dict] = None) -> AsyncGenerator[dict, None]:
    async for event in rag_chat_stream(question, history):
        yield event
