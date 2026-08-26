# Author: lxy
"""Mock Embeddings - 基于文本 hash 生成确定性向量"""

from typing import List

import numpy as np
from langchain_core.embeddings import Embeddings


class MockEmbeddings(Embeddings):
    """Mock 嵌入模型 - 基于文本 hash 生成确定性 1024 维向量"""

    dimensions: int = 1024

    def embed_documents(self, texts: List[str]) -> List[List[float]]:
        """批量生成文本嵌入向量"""
        return [self._generate_vector(text) for text in texts]

    def embed_query(self, text: str) -> List[float]:
        """生成单个查询文本的嵌入向量"""
        return self._generate_vector(text)

    def _generate_vector(self, text: str) -> List[float]:
        """基于文本 hash 生成确定性向量"""
        seed = hash(text) % (2**32)
        rng = np.random.RandomState(seed)
        vector = rng.randn(self.dimensions)
        # 归一化为单位向量
        norm = np.linalg.norm(vector)
        if norm > 0:
            vector = vector / norm
        return vector.tolist()
