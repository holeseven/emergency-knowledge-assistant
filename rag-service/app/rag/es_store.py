# Author: lxy
"""Elasticsearch 向量存储"""
from typing import List, Optional
from langchain_core.documents import Document
from app.config import get_settings
from app.providers.embeddings import get_embeddings


class ESVectorStore:
    def __init__(self):
        self.settings = get_settings()
        self.embeddings = get_embeddings()
        self.index_name = "rag-safety-emergency"
        self._client = None
        self._tried_connect = False
        self._memory_store: List[Document] = []

    @property
    def client(self):
        if self._client is None and not self._tried_connect:
            self._tried_connect = True
            try:
                from elasticsearch import Elasticsearch
                es = Elasticsearch(f"http://{self.settings.es_host}", request_timeout=10)
                # 先真实探活，通了才赋值
                if es.ping():
                    self._client = es
                    self._ensure_index()
                else:
                    print("ES ping 失败，降级为内存模式")
                    self._client = None
            except Exception as e:
                print(f"ES 连接失败，降级为内存模式: {e}")
                self._client = None
        return self._client

    def _ensure_index(self):
        if not self.client.indices.exists(index=self.index_name):
            self.client.indices.create(index=self.index_name, body={
                "mappings": {
                    "properties": {
                        "content": {"type": "text", "analyzer": "standard"},
                        "embedding": {"type": "dense_vector", "dims": 1024, "index": True, "similarity": "cosine"},
                        "metadata": {"type": "object", "enabled": False}
                    }
                }
            })

    def add_documents(self, documents: List[Document]) -> int:
        if self.client is None:
            self._memory_store.extend(documents)
            return len(documents)
        from elasticsearch.helpers import bulk
        texts = [doc.page_content for doc in documents]
        embeddings = self.embeddings.embed_documents(texts)
        actions = []
        for doc, emb in zip(documents, embeddings):
            actions.append({"_index": self.index_name, "_source": {"content": doc.page_content, "embedding": emb, "metadata": doc.metadata}})
        success, _ = bulk(self.client, actions)
        self.client.indices.refresh(index=self.index_name)
        return success

    def similarity_search(self, query: str, top_k: int = 5) -> List[dict]:
        if self.client is None:
            return [{"content": d.page_content, "score": 0.85, "metadata": d.metadata} for d in self._memory_store[:top_k]]
        query_embedding = self.embeddings.embed_query(query)
        response = self.client.search(index=self.index_name, body={"size": top_k, "knn": {"field": "embedding", "query_vector": query_embedding, "k": top_k, "num_candidates": top_k * 10}})
        return [{"content": hit["_source"]["content"], "score": hit["_score"], "metadata": hit["_source"].get("metadata", {})} for hit in response["hits"]["hits"]]


_store_instance: Optional[ESVectorStore] = None

def get_es_store() -> ESVectorStore:
    global _store_instance
    if _store_instance is None:
        _store_instance = ESVectorStore()
    return _store_instance
