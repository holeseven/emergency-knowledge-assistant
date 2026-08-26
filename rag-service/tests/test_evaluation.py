# Author: lxy
"""测试评估指标"""
from app.evaluation.metrics import faithfulness_score, retrieval_hit_rate


def test_faithfulness_high():
    score = faithfulness_score("1小时内上报", ["网络安全事件必须在1小时内上报"])
    assert score > 0.3


def test_hit_rate():
    rate = retrieval_hit_rate("1小时上报", ["必须在1小时内上报主管部门"])
    assert rate > 0.3
