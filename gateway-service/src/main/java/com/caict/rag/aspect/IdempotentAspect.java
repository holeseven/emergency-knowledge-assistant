package com.caict.rag.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;

/**
 * 幂等切面。
 *
 * <p>职责：拦截 {@link Idempotent} 注解的方法，用 Redisson 分布式锁保证“同一请求同一时刻只执行一次”。</p>
 *
 * <p>锁 key 设计：用户 + 接口(方法全名) + 参数 MD5。
 * <ul>
 *   <li>用户维度：不同用户的相同请求互不影响；</li>
 *   <li>接口维度：区分不同业务方法；</li>
 *   <li>参数 MD5：相同参数才算“重复提交”，把可能很长的参数压缩成定长摘要作为 key。</li>
 * </ul></p>
 *
 * <p>为什么用 {@code tryLock} 而非 {@code lock}：重复提交场景下我们希望“抢不到锁立即失败并提示”，
 * 而不是排队等待，因此用非阻塞的 tryLock，失败即抛“请勿重复提交”。</p>
 *
 * <p>优雅降级：Redisson 不可用时直接放行执行，不因锁组件故障阻断正常业务。</p>
 *
 * @author lxy
 */
@Aspect
@Component
public class IdempotentAspect {

    private static final Logger log = LoggerFactory.getLogger(IdempotentAspect.class);

    /** 幂等锁 key 前缀 */
    private static final String LOCK_KEY_PREFIX = "RAG:GATEWAY:IDEMPOTENT:";

    private final ObjectProvider<RedissonClient> redissonClientProvider;

    public IdempotentAspect(ObjectProvider<RedissonClient> redissonClientProvider) {
        this.redissonClientProvider = redissonClientProvider;
    }

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint pjp, Idempotent idempotent) throws Throwable {
        RedissonClient client;
        try {
            client = redissonClientProvider.getIfAvailable();
        } catch (Exception e) {
            // 锁组件创建/连接异常，降级直接执行
            log.warn("获取 RedissonClient 失败，幂等降级执行, reason={}", e.getMessage());
            return pjp.proceed();
        }
        if (client == null) {
            // 锁组件不可用，降级直接执行
            return pjp.proceed();
        }

        String lockKey = buildLockKey(pjp);
        RLock lock = client.getLock(lockKey);

        boolean locked;
        try {
            // 不等待，抢不到即判定重复提交；leaseTime 后自动释放，防止死锁
            locked = lock.tryLock(0, idempotent.leaseTime(), java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("幂等加锁异常，降级执行, reason={}", e.getMessage());
            return pjp.proceed();
        }

        if (!locked) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, idempotent.message());
        }

        try {
            return pjp.proceed();
        } finally {
            // 仅释放当前线程持有的锁
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 构建锁 key：前缀 + 用户 + 方法全名 + 参数 MD5。
     */
    private String buildLockKey(ProceedingJoinPoint pjp) {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        String methodName = method.getDeclaringClass().getName() + "#" + method.getName();

        // 演示：真实场景应从鉴权上下文(如 SecurityContext/请求头)取用户标识
        String user = "anonymous";
        String argsMd5 = md5(Arrays.deepToString(pjp.getArgs()));

        return LOCK_KEY_PREFIX + user + ":" + methodName + ":" + argsMd5;
    }

    /**
     * 计算 MD5 摘要；异常时退化为普通 hashCode，保证不影响主流程。
     */
    private String md5(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(raw.hashCode());
        }
    }
}
