package com.caict.rag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 网关服务启动类。
 *
 * <p>职责：作为安全应急 RAG 客服系统的统一流量入口（Java 网关与治理层）。
 * 前端 React 的请求先到达本服务，经过“限流 / 防重放 / 幂等 / 敏感词过滤”等
 * 治理链路后，再转发给 Python RAG 服务（http://localhost:8000）。</p>
 *
 * <p>为什么单独拆一个 Java 网关：把安全治理（Sentinel 流控、Redisson 分布式锁、
 * 防重放、输入护栏）与 RAG 业务（Python）解耦，治理逻辑复用成熟的 Java 中间件生态，
 * Python 侧专注检索与生成，职责清晰、可独立伸缩。</p>
 *
 * @author lxy
 */
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
