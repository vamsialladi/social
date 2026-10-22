package com.social.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USER_ID_HEADER_SCHEME = "UserIdHeader";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Social & Artist Platform API")
                        .version("1.0.0")
                        .description("Backend REST APIs for Profile Management, Media Upload, Posts & Articles, Opportunities, and Network Connections.")
                        .contact(new Contact().name("Backend Development Team")))
                .components(new Components()
                        .addSecuritySchemes(USER_ID_HEADER_SCHEME, new SecurityScheme()
                                .name(USER_ID_HEADER)
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .description("User session identifier passed in X-User-Id header (e.g. user-123)")))
                .addSecurityItem(new SecurityRequirement().addList(USER_ID_HEADER_SCHEME));
    }
}
