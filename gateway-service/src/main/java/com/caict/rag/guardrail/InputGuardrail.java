package com.caict.rag.guardrail;

import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 输入安全护栏（敏感词过滤）。
 *
 * <p>职责：在把用户输入透传给大模型前，先做一道敏感词过滤，命中则拒绝，
 * 防止违规内容进入 RAG 链路，属于 AI 安全护栏(Guardrail)的输入侧防护。</p>
 *
 * <p>为什么用 Redis Set 存敏感词：
 * <ul>
 *   <li>Set 天然去重，且 {@code SISMEMBER} 判定是否命中是 O(1)，适合高频校验；</li>
 *   <li>词库集中存储，多实例共享、可动态增删，无需重启发布。</li>
 * </ul></p>
 *
 * <p>优雅降级：Redis 不可用时放行（fail-open），避免因护栏组件故障导致问答完全不可用。</p>
 *
 * @author lxy
 */
@Component
public class InputGuardrail {

    private static final Logger log = LoggerFactory.getLogger(InputGuardrail.class);

    /** 敏感词库在 Redis 中的 key */
    private static final String SENSITIVE_WORDS_KEY = "sensitive_words";

    /** 启动时预置的示例敏感词 */
    private static final List<String> DEFAULT_WORDS = List.of("暴恐", "诈骗", "赌博", "违禁品");

    private final ObjectProvider<RedissonClient> redissonClientProvider;

    public InputGuardrail(ObjectProvider<RedissonClient> redissonClientProvider) {
        this.redissonClientProvider = redissonClientProvider;
    }

    /**
     * 应用就绪后初始化示例敏感词到 Redis。
     *
     * <p>为什么用 {@link ApplicationReadyEvent} 而不是构造/@PostConstruct：
     * 确保在容器完全就绪、Redis 连接可用后再写入，且失败不影响启动。</p>
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initSensitiveWords() {
        try {
            RedissonClient client = redissonClientProvider.getIfAvailable();
            if (client == null) {
                log.warn("RedissonClient 不可用，跳过敏感词初始化");
                return;
            }
            RSet<String> set = client.getSet(SENSITIVE_WORDS_KEY);
            set.addAll(DEFAULT_WORDS);
            log.info("敏感词库初始化完成, 当前词数={}", set.size());
        } catch (Exception e) {
            log.warn("敏感词初始化失败(Redis 不可用), reason={}", e.getMessage());
        }
    }

    /**
     * 校验输入是否命中敏感词。
     *
     * @param input 用户输入
     * @return true=安全放行；false=命中敏感词需拒绝
     */
    public boolean isSafe(String input) {
        if (input == null || input.isBlank()) {
            return true;
        }
        try {
            RedissonClient client = redissonClientProvider.getIfAvailable();
            if (client == null) {
                return true;
            }
            RSet<String> set = client.getSet(SENSITIVE_WORDS_KEY);
            for (String word : set) {
                if (input.contains(word)) {
                    log.warn("输入命中敏感词: {}", word);
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            // 护栏组件故障时放行，保证可用性
            log.warn("敏感词校验降级放行(Redis 不可用), reason={}", e.getMessage());
            return true;
        }
    }
}
