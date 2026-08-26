# Author: lxy
"""测试检索器"""
from app.rag.retriever import retrieve


def test_retrieve_returns_list():
    results = retrieve("网络安全事件上报时限")
    assert isinstance(results, list)
