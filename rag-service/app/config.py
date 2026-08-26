# Author: lxy
"""应用配置模块 - 从 .env 加载配置并提供单例访问"""

import os
from functools import lru_cache
from pathlib import Path

from dotenv import load_dotenv
from pydantic import Field
from pydantic_settings import BaseSettings

# 加载项目根目录的 .env 文件
_env_path = Path(__file__).resolve().parent.parent.parent / ".env"
if _env_path.exists():
    load_dotenv(_env_path)
else:
    # 尝试加载 rag-service 目录下的 .env
    _local_env = Path(__file__).resolve().parent.parent / ".env"
    if _local_env.exists():
        load_dotenv(_local_env)


class Settings(BaseSettings):
    """应用配置"""

    # 大模型配置
    glm_api_key: str = Field(default="", alias="GLM_API_KEY")
    glm_base_url: str = Field(
        default="https://open.bigmodel.cn/api/paas/v4", alias="GLM_BASE_URL"
    )
    glm_model: str = Field(default="glm-4-flash", alias="GLM_MODEL")
    embedding_model: str = Field(default="embedding-3", alias="EMBEDDING_MODEL")

    # 运行模式: auto | real | mock
    run_mode: str = Field(default="auto", alias="RUN_MODE")

    # 中间件地址
    es_host: str = Field(default="localhost:9200", alias="ES_HOST")
    redis_host: str = Field(default="localhost:6379", alias="REDIS_HOST")
    rocketmq_namesrv: str = Field(default="localhost:9876", alias="ROCKETMQ_NAMESRV")

    # RAG 参数
    similarity_threshold: float = Field(default=0.75, alias="SIMILARITY_THRESHOLD")
    chunk_size: int = Field(default=500)
    chunk_overlap: int = Field(default=50)
    top_k: int = Field(default=5)
    memory_rounds: int = Field(default=10)

    # ES 索引名
    es_index: str = Field(default="rag_knowledge_base", alias="ES_INDEX")

    class Config:
        env_file = ".env"
        populate_by_name = True

    @property
    def use_mock(self) -> bool:
        """判断是否应使用 Mock 模式"""
        if self.run_mode == "mock":
            return True
        if self.run_mode == "real":
            return False
        # auto 模式: 无 key 时用 mock
        return not self.glm_api_key or self.glm_api_key == "your-api-key-here"


@lru_cache()
def get_settings() -> Settings:
    """获取配置单例"""
    return Settings()
