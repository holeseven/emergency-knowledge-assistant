package com.caict.rag.filter;

import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * 防重放攻击过滤器。
 *
 * <p>职责：校验请求头中的 timestamp 与 nonce，拦截“抓包后原样重发”的重放请求。</p>
 *
 * <p>设计原理：
 * <ul>
 *   <li>timestamp：请求时间戳，超过 5 分钟视为过期请求，直接拒绝，缩小重放窗口；</li>
 *   <li>nonce：一次性随机串，用 Redis 的 {@code SET NX EX 300} 语义写入，
 *       若已存在说明该请求在有效期内被重复提交，判定为重放并拒绝。</li>
 * </ul>
 * 两者配合：timestamp 限制“时间窗口”，nonce 保证“窗口内唯一”，从而实现防重放。</p>
 *
 * <p>为什么对 SSE 的 GET /api/chat 放宽：EventSource 无法自定义请求头，
 * 强校验会导致正常问答失败，因此此处仅演示逻辑，对 GET 直接放行。</p>
 *
 * <p>优雅降级：Redis 不可用时对 nonce 校验 fail-open，仅保留 timestamp 的过期判断。</p>
 *
 * @author lxy
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class AntiReplayFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(AntiReplayFilter.class);

    /** nonce 在 Redis 中的 key 前缀 */
    private static final String NONCE_KEY_PREFIX = "RAG:GATEWAY:NONCE:";
    /** 时间戳有效期：5 分钟（毫秒） */
    private static final long MAX_INTERVAL_MILLIS = 5 * 60 * 1000L;
    /** nonce 的 Redis 存活时间，与时间戳窗口一致 */
    private static final Duration NONCE_TTL = Duration.ofSeconds(300);

    private final ObjectProvider<RedissonClient> redissonClientProvider;

    public AntiReplayFilter(ObjectProvider<RedissonClient> redissonClientProvider) {
        this.redissonClientProvider = redissonClientProvider;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // SSE 问答放宽校验
        if (HttpMethod.GET.equals(request.getMethod())) {
            return chain.filter(exchange);
        }

        String timestamp = request.getHeaders().getFirst("timestamp");
        String nonce = request.getHeaders().getFirst("nonce");

        // 未携带防重放头则放行（演示用，生产可改为强制拒绝）
        if (timestamp == null || nonce == null) {
            return chain.filter(exchange);
        }

        // 1) 时间戳过期校验
        try {
            long ts = Long.parseLong(timestamp);
            if (Math.abs(System.currentTimeMillis() - ts) > MAX_INTERVAL_MILLIS) {
                return reject(exchange, "请求已过期");
            }
        } catch (NumberFormatException e) {
            return reject(exchange, "非法的时间戳");
        }

        // 2) nonce 唯一性校验（SET NX EX 300）
        try {
            RedissonClient client = redissonClientProvider.getIfAvailable();
            if (client != null) {
                RBucket<String> bucket = client.getBucket(NONCE_KEY_PREFIX + nonce);
                boolean firstSeen = bucket.setIfAbsent("1", NONCE_TTL);
                if (!firstSeen) {
                    return reject(exchange, "检测到重放攻击");
                }
            }
        } catch (Exception e) {
            // Redis 不可用时降级：仅保留时间戳校验
            log.warn("nonce 校验降级(Redis 不可用), reason={}", e.getMessage());
        }

        return chain.filter(exchange);
    }

    /**
     * 以 403 拒绝请求。
     */
    private Mono<Void> reject(ServerWebExchange exchange, String reason) {
        log.warn("防重放拦截: {}", reason);
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        return exchange.getResponse().setComplete();
    }
}
