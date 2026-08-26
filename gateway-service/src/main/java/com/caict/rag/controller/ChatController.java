package com.caict.rag.controller;

import com.caict.rag.client.RagServiceClient;
import com.caict.rag.guardrail.InputGuardrail;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * 对话接口控制器。
 *
 * <p>职责：接收前端的问答请求，把 Python RAG 服务的 SSE 流式响应“原样透传”给前端。</p>
 *
 * <p>为什么用 GET + text/event-stream：SSE（Server-Sent Events）是浏览器 EventSource
 * 的标准协议，天然基于 GET；返回 {@code Flux<String>} 时 WebFlux 会以流式方式逐段 flush，
 * 实现“边生成边显示”的打字机效果，而不是等全部生成完再一次性返回。</p>
 *
 * @author lxy
 */
@RestController
public class ChatController {

    private final RagServiceClient ragServiceClient;
    private final InputGuardrail inputGuardrail;

    public ChatController(RagServiceClient ragServiceClient, InputGuardrail inputGuardrail) {
        this.ragServiceClient = ragServiceClient;
        this.inputGuardrail = inputGuardrail;
    }

    /**
     * 流式问答。前端通过 EventSource 订阅本接口，网关再向下游 RAG 服务取流并透传。
     *
     * @param question  用户问题
     * @param sessionId 会话标识（多轮对话隔离）
     * @return SSE 数据流
     */
    @GetMapping(value = "/api/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestParam("question") String question,
                             @RequestParam(value = "sessionId", required = false) String sessionId) {
        // 输入安全护栏：命中敏感词直接拒绝，不进入 RAG 链路
        if (!inputGuardrail.isSafe(question)) {
            return Flux.just("{\"type\":\"error\",\"content\":\"输入包含敏感内容，已被拦截\"}");
        }
        return ragServiceClient.chatStream(question, sessionId)
                // 下游异常时不让连接直接断开为 500，而是推送一条错误事件，前端可优雅提示
                .onErrorResume(ex -> Flux.just("{\"type\":\"error\",\"content\":\"下游服务暂不可用，请稍后再试\"}"));
    }
}
