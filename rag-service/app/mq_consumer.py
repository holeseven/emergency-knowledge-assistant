# Author: lxy
"""
RocketMQ Consumer - 文档入库消费者
注意：rocketmq-client-python 在 arm64 Mac 上兼容性较差。
生产环境由 Java gateway-service 的 Consumer 处理。
本地开发直接调用 /api/ingest 接口触发入库。
"""
import asyncio
from app.rag.pipeline import ingest_documents


async def consume_ingest_tasks():
    """模拟 MQ 消费循环（生产环境接 RocketMQ）"""
    print("[MQ Consumer] 启动（本地模式：通过 /api/ingest 触发）")
    while True:
        await asyncio.sleep(60)


if __name__ == "__main__":
    asyncio.run(consume_ingest_tasks())
