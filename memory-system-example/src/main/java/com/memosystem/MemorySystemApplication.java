package com.memosystem;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;

/**
 * 记忆管理系统主应用类
 */
@SpringBootApplication
@Slf4j
public class MemorySystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(MemorySystemApplication.class, args);
    }

    /**
     * 应用启动完成后打印 Swagger 链接
     */
    @EventListener(ApplicationReadyEvent.class)
    public void afterApplicationStart() {
        log.info("========================================");
        log.info("  记忆管理系统已启动");
        log.info("========================================");
        log.info("");
        log.info("📚 API 文档访问地址:");
        log.info("   Swagger UI: http://localhost:8080/swagger-ui.html");
        log.info("   OpenAPI JSON: http://localhost:8080/v3/api-docs");
        log.info("");
        log.info("🔌 API 调用示例:");
        log.info("   对话接口: POST http://localhost:8080/api/conversation/chat?message=你好");
        log.info("   健康检查: GET http://localhost:8080/api/conversation/health");
        log.info("   版本信息: GET http://localhost:8080/api/conversation/version");
        log.info("");
        log.info("========================================");
    }
}
