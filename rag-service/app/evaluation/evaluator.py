# Author: lxy
"""评估执行器"""
import json
import os
from app.rag.retriever import retrieve
from app.evaluation.metrics import faithfulness_score, retrieval_hit_rate


def run_evaluation() -> dict:
    path = os.path.join(os.path.dirname(__file__), "../../../data/eval/qa_dataset.json")
    path = os.path.abspath(path)
    if not os.path.exists(path):
        return {"status": "error", "message": "评估数据集不存在"}
    with open(path, "r", encoding="utf-8") as f:
        dataset = json.load(f)
    results = []
    total_f, total_h = 0.0, 0.0
    for item in dataset:
        docs = retrieve(item["question"])
        contents = [d.page_content for d in docs]
        answer = " ".join(contents[:2]) if contents else ""
        f = faithfulness_score(answer, contents)
        h = retrieval_hit_rate(item["ground_truth"], contents)
        total_f += f
        total_h += h
        results.append({"question": item["question"], "retrieved": len(docs), "faithfulness": round(f, 3), "hit_rate": round(h, 3)})
    n = max(len(dataset), 1)
    return {"status": "ok", "total": len(dataset), "avg_faithfulness": round(total_f/n, 3), "avg_hit_rate": round(total_h/n, 3), "details": results}
