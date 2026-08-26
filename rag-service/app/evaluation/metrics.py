# Author: lxy
"""评估指标"""
import re

_STOPS = {"的", "是", "在", "了", "和", "与", "或", "及", "等", "为", "中"}


def _tokenize(text: str) -> set:
    """简单中文分词：按空格拆分后，对含中文片段按字符级 bigram 拆分"""
    raw = text.lower().replace("。", " ").replace("，", " ").split()
    tokens = set()
    for w in raw:
        if re.search(r'[\u4e00-\u9fff]', w):
            # 逐字符加入
            for ch in w:
                if ch not in _STOPS:
                    tokens.add(ch)
            # 加入 bigram 以保留短语信息
            for i in range(len(w) - 1):
                tokens.add(w[i:i+2])
        else:
            tokens.add(w)
    tokens -= _STOPS
    return tokens


def faithfulness_score(answer: str, contexts: list) -> float:
    if not contexts or not answer:
        return 0.0
    context_text = " ".join(contexts).lower()
    words = _tokenize(answer)
    if not words:
        return 1.0
    return sum(1 for w in words if w in context_text) / len(words)


def retrieval_hit_rate(ground_truth: str, retrieved: list) -> float:
    if not retrieved or not ground_truth:
        return 0.0
    text = " ".join(retrieved).lower()
    words = _tokenize(ground_truth)
    if not words:
        return 1.0
    return sum(1 for w in words if w in text) / len(words)
