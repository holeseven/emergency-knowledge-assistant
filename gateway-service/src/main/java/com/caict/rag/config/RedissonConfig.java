package com.caict.rag.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * Redisson 客户端配置。
 *
 * <p>职责：自定义 {@link RedissonClient} 的构建方式，替代 starter 的默认自动装配。</p>
 *
 * <p>为什么用 {@link Lazy} 懒加载：Redisson 默认在 Bean 创建时就与 Redis 建链，
 * 一旦 Redis 未启动，容器启动就会直接失败。加上 @Lazy 后，只有当限流/幂等/护栏等
 * 组件第一次真正用到时才创建连接；配合各调用方的 try-catch fail-open，
 * 就能做到“Redis 不可用也不影响服务启动”，满足中间件故障优雅降级的要求。</p>
 *
 * @author lxy
 */
@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String host;

    @Value("${spring.data.redis.port:6379}")
    private int port;

    /**
     * 懒加载的 RedissonClient。使用单机模式连接本地 Redis。
     */
    @Bean(destroyMethod = "shutdown")
    @Lazy
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + host + ":" + port)
                // 连接不上时快速失败，避免请求线程被长时间阻塞
                .setConnectTimeout(3000)
                .setTimeout(3000)
                .setRetryAttempts(1);
        return Redisson.create(config);
    }
}
