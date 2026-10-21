package com.social.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String USER_ID_HEADER = "X-User-Id";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TrueHunt Social & Opportunities API")
                        .version("1.0.0")
                        .description("Backend REST API implementation for Module 5: Opportunities and Module 6: Network and Connections")
                        .contact(new Contact().name("TrueHunt Engineering").email("dev@truehunt.in")))
                .components(new Components()
                        .addSecuritySchemes(USER_ID_HEADER, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name(USER_ID_HEADER)
                                .description("User session identifier passed in X-User-Id header")))
                .addSecurityItem(new SecurityRequirement().addList(USER_ID_HEADER));
    }
}
