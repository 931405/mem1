package com.memosystem.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger/OpenAPI 配置类
 * 用于生成 API 文档
 * 仅当存在 OpenAPI 类时自动配置
 */
@Configuration("mem0SwaggerConfig")
@ConditionalOnClass(OpenAPI.class)
public class SwaggerConfig {

        @Bean
        @ConditionalOnMissingBean(OpenAPI.class)
        public OpenAPI memorySystemOpenAPI() {
                return new OpenAPI()
                                .info(new Info()
                                                .title("记忆管理系统 API")
                                                .version("1.0.0")
                                                .description("mem0-like 记忆管理系统 API 接口文档")
                                                .contact(new Contact()
                                                                .name("开发团队")
                                                                .url("https://github.com"))
                                                .license(new License()
                                                                .name("MIT License")));
        }
}
