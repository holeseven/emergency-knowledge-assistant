package com.caict.rag.config;

import com.alibaba.csp.sentinel.adapter.spring.webflux.callback.BlockRequestHandler;
import com.alibaba.csp.sentinel.adapter.spring.webflux.callback.WebFluxCallbackManager;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Sentinel 流控配置。
 *
 * <p>职责：以代码方式初始化流控规则，并自定义“被限流后的返回”。</p>
 *
 * <p>为什么用 Sentinel 做 QPS 流控：它面向“资源”维度做单机/集群限流，规则可热更新、可对接控制台，
 * 相比自研计数器更成熟。这里针对 /api/chat（大模型问答是最贵的资源）设定 QPS=10 做保护。</p>
 *
 * <p>与 {@code RateLimitFilter} 的分工：Sentinel 侧重“接口级别的资源保护 + 熔断降级”，
 * RateLimitFilter 侧重“基于 Redis 的全局滑动窗口限流”，两者叠加形成双层限流。</p>
 *
 * @author lxy
 */
@Configuration
public class SentinelConfig {

    /** 受保护的核心资源名：问答接口 */
    public static final String CHAT_RESOURCE = "/api/chat";

    /**
     * 初始化流控规则与限流响应处理器。
     */
    @PostConstruct
    public void init() {
        initFlowRules();
        initBlockHandler();
    }

    /**
     * 对 /api/chat 资源设置 QPS=10 的流控规则。
     */
    private void initFlowRules() {
        List<FlowRule> rules = new ArrayList<>();
        FlowRule rule = new FlowRule();
        rule.setResource(CHAT_RESOURCE);
        // 按 QPS 限流
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        rule.setCount(10);
        rules.add(rule);
        FlowRuleManager.loadRules(rules);
    }

    /**
     * 自定义被限流/降级时的响应：返回 HTTP 429 + 统一 JSON 结构。
     *
     * <p>为什么统一成 429 + 友好文案：前端只需按状态码兜底提示，无需解析 Sentinel 内部异常类型，
     * 降低前后端耦合。</p>
     */
    private void initBlockHandler() {
        BlockRequestHandler handler = (exchange, ex) -> {
            Map<String, Object> body = Map.of(
                    "code", 429,
                    "message", "当前访问人数较多，请稍后再试"
            );
            return ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(body));
        };
        WebFluxCallbackManager.setBlockHandler(handler);
    }
}
