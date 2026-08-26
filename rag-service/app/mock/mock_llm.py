# Author: lxy
"""Mock LLM - 无需真实 API Key 即可演示完整链路"""

import asyncio
from typing import Any, Iterator, List, Optional

from langchain_core.callbacks import CallbackManagerForLLMRun
from langchain_core.language_models.chat_models import BaseChatModel
from langchain_core.messages import AIMessage, AIMessageChunk, BaseMessage
from langchain_core.outputs import ChatGeneration, ChatGenerationChunk, ChatResult


_MOCK_RESPONSE = (
    "根据安全应急知识库，针对您的问题，建议按照以下步骤进行处置：\n"
    "1. 首先确认事件类型和影响范围\n"
    "2. 启动对应等级的应急响应预案\n"
    "3. 通知相关责任人并组建应急处置小组\n"
    "4. 按照标准SOP流程执行处置操作\n"
    "5. 事件处置完成后进行复盘总结\n\n"
    "引用来源: 应急处置SOP标准流程.md"
)


class MockChatModel(BaseChatModel):
    """Mock 聊天模型，流式返回模拟回答"""

    model_name: str = "mock-glm"

    @property
    def _llm_type(self) -> str:
        return "mock-chat-model"

    def _generate(
        self,
        messages: List[BaseMessage],
        stop: Optional[List[str]] = None,
        run_manager: Optional[CallbackManagerForLLMRun] = None,
        **kwargs: Any,
    ) -> ChatResult:
        """同步生成"""
        message = AIMessage(content=_MOCK_RESPONSE)
        return ChatResult(generations=[ChatGeneration(message=message)])

    def _stream(
        self,
        messages: List[BaseMessage],
        stop: Optional[List[str]] = None,
        run_manager: Optional[CallbackManagerForLLMRun] = None,
        **kwargs: Any,
    ) -> Iterator[ChatGenerationChunk]:
        """流式生成 - 逐字符输出，模拟打字效果"""
        import time

        for char in _MOCK_RESPONSE:
            chunk = ChatGenerationChunk(message=AIMessageChunk(content=char))
            if run_manager:
                run_manager.on_llm_new_token(char)
            yield chunk
            time.sleep(0.05)  # 50ms 间隔模拟打字

    @property
    def _identifying_params(self) -> dict:
        return {"model_name": self.model_name}
