package com.ecommerce.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Tài liệu OpenAPI cho từng service (NFR-MNT-002). Server = URL gateway: client chỉ gọi qua gateway.
 * Xem tất cả ở một chỗ: http://localhost:8080/swagger-ui.html (gateway gộp lại).
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI(@Value("${spring.application.name}") String name,
                           @Value("${app.public-base-url:${PUBLIC_BASE_URL:http://localhost:8080}}") String publicUrl) {
        return new OpenAPI()
                .info(new Info().title(name + " API").version("v1")
                        .description("SRS-OMNI-ECOM-001. Lỗi luôn có dạng {code, message, details?, correlationId}."))
                .servers(List.of(new Server().url(publicUrl).description("API Gateway")))
                .components(new Components().addSecuritySchemes("bearer",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearer"));
    }

    /** Mọi API đều nhận X-Correlation-Id (tùy chọn) để truy vết (COM-05). */
    @Bean
    public OperationCustomizer correlationIdHeader() {
        return (operation, handlerMethod) -> operation.addParametersItem(new HeaderParameter()
                .name("X-Correlation-Id").required(false).description("Mã truy vết; không gửi thì hệ thống tự cấp"));
    }
}
