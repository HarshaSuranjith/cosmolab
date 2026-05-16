package com.arcticsurge.cosmolab.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI cosmoLabOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("CosmoLab Clinical API")
                        .description("""
                                REST API for CosmoLab — a minimal clinical information system. Covers patient demographics, EHR records (openEHR \
                                containment hierarchy: EHR → Composition → Entry), vital signs observations, \
                                and problem list evaluations. The ward overview endpoint is the primary \
                                load test target (JMeter / Gatling / k6).
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Harsha Amarasiri")
                                .email("harshasuranjith@gmail.com")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Dev — JBoss EAP 8.0")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Mock JWT — any non-empty value accepted. " +
                                        "Spring Security is permit-all; auth boundary is scaffolded for future IDP integration.")));
    }
}
