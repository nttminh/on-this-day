package com.portfolio.onthisday.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Metadata shown on the Swagger UI / OpenAPI document. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI onThisDayOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("On This Day API")
                .description("Browse real historical events sourced from the Wikipedia "
                        + "\"On This Day\" API, curated and tagged for a Giphy-style UI.")
                .version("v1")
                .license(new License().name("MIT")));
    }
}
