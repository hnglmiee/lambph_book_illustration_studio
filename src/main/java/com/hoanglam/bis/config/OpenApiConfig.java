package com.hoanglam.bis.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
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
                        .title("Book Illustration Studio API Specification")
                        .version("v1.0.0")
                        .description(
                                "API Documentation for Book Illustration Studio " +
                                        "(BIS) project powered by Spring Boot & Gemini API."
                        )
                        .contact(new Contact()
                                .name("Hoang Lam")
                                .email("admin@bookstudio.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://springdoc.org")))

                // JWT Bearer authentication
                .components(new Components()
                        .addSecuritySchemes(
                                "bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                        ))

                // Apply JWT authentication globally
                .addSecurityItem(
                        new SecurityRequirement()
                                .addList("bearerAuth")
                );
    }
}