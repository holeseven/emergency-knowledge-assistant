# Author: lxy
"""数据模型定义"""

from typing import Optional

from pydantic import BaseModel, Field


class ChatRequest(BaseModel):
    """对话请求"""
    question: str = Field(..., description="用户问题")
    session_id: Optional[str] = Field(default=None, description="会话ID，用于对话记忆")


class ChatEvent(BaseModel):
    """SSE 事件"""
    type: str = Field(..., description="事件类型: token|citation|done")
    content: str = Field(default="", description="事件内容")


class IngestRequest(BaseModel):
    """文档入库请求"""
    file_paths: Optional[list[str]] = Field(
        default=None, description="指定文件路径列表，为空则加载 data/raw/ 下所有文件"
    )


class EvalResponse(BaseModel):
    """评估结果响应"""
    metrics: dict = Field(default_factory=dict, description="评估指标")
    detail: Optional[list[dict]] = Field(default=None, description="详细结果")
