package com.henashi.inventorycrm.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("库存客户关系管理项目 API 文档")
                        .version("1.0.0")
                        .description("基于 SpringDoc OpenAPI 的接口文档")
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://springdoc.org")))
                // 安全方案配置
                .addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("BearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")))
                // 其他全局配置
                .externalDocs(new ExternalDocumentation()
                        .description("更多信息")
                        .url("https://example.com"));
    }
}