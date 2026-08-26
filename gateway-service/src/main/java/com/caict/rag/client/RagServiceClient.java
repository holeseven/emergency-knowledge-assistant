package com.caict.rag.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * Python RAG 服务的调用客户端。
 *
 * <p>职责：封装对下游 Python RAG 服务（默认 http://localhost:8000）的调用，
 * 对上层 Controller 屏蔽 WebClient 的构建细节。</p>
 *
 * <p>为什么用 WebClient 而不是 RestTemplate：网关基于 WebFlux 响应式栈，
 * 下游 /api/chat 是 SSE 流式接口，只有 WebClient 能以非阻塞方式把
 * {@code Flux<String>} 逐段透传给前端，避免线程被长连接占满。</p>
 *
 * @author lxy
 */
@Component
public class RagServiceClient {

    private final WebClient webClient;

    /**
     * 构造时固定 baseUrl。baseUrl 从配置注入，便于多环境切换。
     */
    public RagServiceClient(@Value("${rag-service.base-url:http://localhost:8000}") String baseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    /**
     * 调用下游 /api/chat，返回 SSE 数据流。
     *
     * <p>Python 侧是 POST + EventSourceResponse，这里以 body 提交 question / sessionId，
     * 并以 {@code Flux<String>} 的形式接收流式 token，交由 Controller 透传。</p>
     *
     * @param question  用户问题
     * @param sessionId 会话标识，用于下游维护多轮对话历史
     * @return SSE 数据流（每个元素是一段 data）
     */
    public Flux<String> chatStream(String question, String sessionId) {
        return webClient.post()
                .uri("/api/chat")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(Map.of(
                        "question", question == null ? "" : question,
                        "session_id", sessionId == null ? "default" : sessionId
                ))
                .retrieve()
                .bodyToFlux(String.class);
    }

    /**
     * 降级调用：直接触发下游入库。当 RocketMQ 不可用时，IngestController 会退回到这里。
     *
     * @param dataDir 可选的数据目录
     * @return 下游返回的原始 JSON 字符串
     */
    public String ingestDirectly(String dataDir) {
        return webClient.post()
                .uri("/api/ingest")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("data_dir", dataDir == null ? "" : dataDir))
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}
