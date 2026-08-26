# Author: lxy
"""文档加载器"""
import os
from typing import List
from langchain_core.documents import Document
from langchain_community.document_loaders import TextLoader


def load_documents(data_dir: str = None) -> List[Document]:
    if data_dir is None:
        data_dir = os.path.join(os.path.dirname(__file__), "../../../data/raw")
    data_dir = os.path.abspath(data_dir)
    documents = []
    if not os.path.exists(data_dir):
        return documents
    for filename in sorted(os.listdir(data_dir)):
        filepath = os.path.join(data_dir, filename)
        if filename.endswith((".md", ".txt")):
            try:
                loader = TextLoader(filepath, encoding="utf-8")
                docs = loader.load()
                for doc in docs:
                    doc.metadata["source"] = filename
                documents.extend(docs)
            except Exception as e:
                print(f"加载 {filename} 失败: {e}")
    return documents
