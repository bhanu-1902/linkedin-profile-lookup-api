package com.profilelookup.infrastructure.config;

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
    public OpenAPI profileLookupOpenApi() {
        final String apiKeyScheme = "ApiKeyAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("Profile Lookup API")
                        .version("0.1.0")
                        .description("Returns structured profile JSON for a submitted URL. "
                                + "Only fixture-backed profiles (derived from a legitimate "
                                + "LinkedIn data export) are served -- see README, "
                                + "'Known limitations & legal considerations.'")
                        .license(new License().name("Unlicensed / demo project")))
                .addSecurityItem(new SecurityRequirement().addList(apiKeyScheme))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes(apiKeyScheme, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-API-Key")));
    }
}
