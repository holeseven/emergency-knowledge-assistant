# Author: lxy
"""LLM Provider - 根据运行模式返回真实或 Mock 大模型实例"""

from langchain_core.language_models.chat_models import BaseChatModel

from app.config import get_settings


def get_llm() -> BaseChatModel:
    """
    工厂方法：根据 RUN_MODE 和 Key 是否存在，返回对应的 LLM 实例
    - 真实模式：ChatOpenAI（对接智谱 GLM，OpenAI 兼容协议）
    - Mock 模式：MockChatModel（流式返回模板回答）
    """
    settings = get_settings()

    if settings.use_mock:
        from app.mock.mock_llm import MockChatModel
        return MockChatModel()

    from langchain_openai import ChatOpenAI
    return ChatOpenAI(
        model=settings.glm_model,
        openai_api_key=settings.glm_api_key,
        openai_api_base=settings.glm_base_url,
        streaming=True,
        temperature=0.1,
    )
