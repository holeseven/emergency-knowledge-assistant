# Author: lxy
"""Embedding Provider"""
from langchain_core.embeddings import Embeddings
from app.config import get_settings


def get_embeddings() -> Embeddings:
    settings = get_settings()
    if settings.use_mock:
        from app.mock.mock_embeddings import MockEmbeddings
        return MockEmbeddings()
    from langchain_openai import OpenAIEmbeddings
    return OpenAIEmbeddings(
        model=settings.embedding_model,
        openai_api_key=settings.glm_api_key,
        openai_api_base=settings.glm_base_url,
    )
