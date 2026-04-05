package com.financeapp.config;

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

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Finance Data Processing & Access Control API")
                        .description("""
                                Backend API for a finance dashboard system.
                                
                                **Roles:**
                                - `VIEWER`  — Read transactions only
                                - `ANALYST` — Read transactions + access dashboard analytics
                                - `ADMIN`   — Full access: manage users, create/update/delete transactions
                                
                                **Default test credentials:**
                                - Admin:   admin@finance.com / admin123
                                - Analyst: analyst@finance.com / analyst123
                                - Viewer:  viewer@finance.com / viewer123
                                
                                Login via `POST /api/auth/login`, copy the token, click **Authorize** above.
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Venkata Koushik Javvaji")
                                .email("javvajikoushik2004@gmail.com")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste the JWT token obtained from /api/auth/login")));
    }
}
