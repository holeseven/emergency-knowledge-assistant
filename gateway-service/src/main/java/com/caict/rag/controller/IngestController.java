package com.caict.rag.controller;

import com.caict.rag.aspect.Idempotent;
import com.caict.rag.client.RagServiceClient;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 文档入库接口控制器。
 *
 * <p>职责：接收文档上传/入库请求，优先把任务投递到 RocketMQ（topic: rag-ingest-topic）实现异步削峰；
 * 若 RocketMQ 不可用，则降级为同步直连 Python /api/ingest，保证功能可用。</p>
 *
 * <p>为什么走消息队列：文档入库（切分 + 向量化）是重 IO/CPU 的慢操作，用 MQ 把“提交”与“执行”解耦，
 * 前端秒回“已提交”，下游消费者按自身节奏消费，避免请求线程被长时间占用。</p>
 *
 * <p>为什么用 {@link ObjectProvider} 注入 RocketMQTemplate：即便 MQ 相关 Bean 缺失或初始化异常，
 * 也不影响本控制器加载，从而实现“中间件不可用不拖垮服务启动”的优雅降级。</p>
 *
 * @author lxy
 */
@RestController
public class IngestController {

    private static final Logger log = LoggerFactory.getLogger(IngestController.class);

    /** 入库任务主题，消费者订阅该主题后调用 Python 侧执行真正入库 */
    private static final String INGEST_TOPIC = "rag-ingest-topic";

    private final ObjectProvider<RocketMQTemplate> rocketMQTemplateProvider;
    private final RagServiceClient ragServiceClient;

    public IngestController(ObjectProvider<RocketMQTemplate> rocketMQTemplateProvider,
                            RagServiceClient ragServiceClient) {
        this.rocketMQTemplateProvider = rocketMQTemplateProvider;
        this.ragServiceClient = ragServiceClient;
    }

    /**
     * 提交入库任务。
     *
     * <p>用 {@link Idempotent} 保证同一份提交在短时间内不会被重复入库（防抖 + 防重复点击）。</p>
     *
     * @param body 请求体，可包含 dataDir 字段
     * @return 处理结果说明
     */
    @Idempotent
    @PostMapping("/api/ingest")
    public String ingest(@RequestBody(required = false) Map<String, Object> body) {
        String dataDir = body == null ? null : (String) body.get("dataDir");

        RocketMQTemplate template = rocketMQTemplateProvider.getIfAvailable();
        if (template != null) {
            try {
                template.convertAndSend(INGEST_TOPIC, body == null ? Map.of() : body);
                return "已提交入库任务";
            } catch (Exception e) {
                // MQ 发送失败（如 broker 不可达）走降级，不把异常抛给前端
                log.warn("RocketMQ 投递失败，降级为直连 Python 入库, reason={}", e.getMessage());
            }
        } else {
            log.warn("RocketMQTemplate 不可用，降级为直连 Python 入库");
        }

        // 降级链路：直接调用下游同步入库
        try {
            String resp = ragServiceClient.ingestDirectly(dataDir);
            return "已提交入库任务(降级直连): " + resp;
        } catch (Exception e) {
            log.error("直连 Python 入库失败", e);
            return "入库任务提交失败，请稍后重试";
        }
    }
}
