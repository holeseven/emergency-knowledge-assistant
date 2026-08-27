#!/bin/bash
# Emergency Knowledge Assistant - 一键启动脚本
# Author: lxy

echo "=== 启动中间件 ==="
docker-compose up -d

echo "=== 等待 Elasticsearch 就绪 ==="
until curl -s http://localhost:9200/_cluster/health > /dev/null 2>&1; do
    echo "  等待 ES..."
    sleep 3
done
echo "  ES 就绪!"

echo "=== 等待 Redis 就绪 ==="
until redis-cli ping > /dev/null 2>&1; do
    echo "  等待 Redis..."
    sleep 2
done
echo "  Redis 就绪!"

echo ""
echo "=== 中间件已启动 ==="
echo "  ES:       http://localhost:9200"
echo "  Redis:    localhost:6379"
echo "  RocketMQ: localhost:9876"
echo ""
echo "=== 请手动启动以下服务 ==="
echo "  1. Python RAG:  cd rag-service && source .venv/bin/activate && uvicorn app.main:app --port 8000"
echo "  2. Java 网关:    cd gateway-service && mvn spring-boot:run"
echo "  3. React 前端:   cd frontend && npm run dev"
