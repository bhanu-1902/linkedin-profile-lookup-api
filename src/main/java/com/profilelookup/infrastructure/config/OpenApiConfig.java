package com.profilelookup.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI profileLookupOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Profile Lookup API")
                        .version("0.1.0")
                        .description("Returns structured profile JSON for a submitted URL. No credential is "
                                + "required to call this deployment; requests are rate-limited per caller "
                                + "address instead -- see README, 'Public access & rate limiting.' Only "
                                + "fixture-backed profiles (derived from a legitimate LinkedIn data export) "
                                + "are served -- see README, 'Known limitations & legal considerations.'")
                        .license(new License().name("Unlicensed / demo project")));
    }
}
