package com.caict.rag.filter;

import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * 全局限流过滤器（Redisson 分布式限流）。
 *
 * <p>职责：在请求进入业务前做一道“全局每秒 100 次”的滑动窗口限流，超限直接返回 429。</p>
 *
 * <p>为什么用 Redisson 的 {@link RRateLimiter}：它把限流令牌存在 Redis，天然支持多实例集群共享同一份配额，
 * 相比单机 Guava/Semaphore 能在水平扩容时仍保证全局 QPS 不被放大。</p>
 *
 * <p>优雅降级：Redis 不可用时（获取客户端或令牌抛异常）采用“fail-open”放行，
 * 宁可少限一次流，也不能因为限流组件故障把正常请求全部打挂。</p>
 *
 * @author lxy
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    /** 全局限流令牌桶在 Redis 中的 key */
    private static final String LIMITER_KEY = "RAG:GATEWAY:GLOBAL:RATE_LIMIT";
    /** 每个窗口允许的最大请求数 */
    private static final int PERMITS = 100;
    /** 窗口大小（秒） */
    private static final int WINDOW_SECONDS = 1;

    private final ObjectProvider<RedissonClient> redissonClientProvider;

    public RateLimitFilter(ObjectProvider<RedissonClient> redissonClientProvider) {
        this.redissonClientProvider = redissonClientProvider;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (tryAcquire()) {
            return chain.filter(exchange);
        }
        // 超限：返回 429
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        return exchange.getResponse().setComplete();
    }

    /**
     * 尝试获取一个令牌。任何异常都视为放行（fail-open）。
     */
    private boolean tryAcquire() {
        try {
            RedissonClient client = redissonClientProvider.getIfAvailable();
            if (client == null) {
                return true;
            }
            RRateLimiter rateLimiter = client.getRateLimiter(LIMITER_KEY);
            if (!rateLimiter.isExists()) {
                // OVERALL：所有实例共享同一份速率配置
                rateLimiter.trySetRate(RateType.OVERALL, PERMITS, WINDOW_SECONDS, RateIntervalUnit.SECONDS);
            }
            return rateLimiter.tryAcquire();
        } catch (Exception e) {
            log.warn("Redisson 限流不可用，放行请求, reason={}", e.getMessage());
            return true;
        }
    }
}
