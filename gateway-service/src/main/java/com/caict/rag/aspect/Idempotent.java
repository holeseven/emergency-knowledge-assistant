package com.caict.rag.aspect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 幂等注解。
 *
 * <p>职责：标注在需要“防重复提交”的方法上，由 {@link IdempotentAspect} 拦截并加分布式锁。</p>
 *
 * <p>为什么用注解 + 切面：把“幂等”这一横切关注点从业务代码中剥离，业务方法只需加一个注解，
 * 无需关心加锁/解锁细节，符合单一职责与 AOP 的解耦思想。</p>
 *
 * @author lxy
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /** 持锁最长时间（秒），到期自动释放，避免死锁 */
    long leaseTime() default 10L;

    /** 重复提交时的提示语 */
    String message() default "请勿重复提交";
}
