package com.portfolio.onthisday.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configures the {@link WebClient} used to talk to the Wikimedia REST API.
 *
 * <p>The Wikimedia API asks callers to send a descriptive {@code User-Agent} so they can
 * contact you if a client misbehaves. See
 * <a href="https://api.wikimedia.org/wiki/Documentation">api.wikimedia.org/wiki/Documentation</a>.
 */
@Configuration
public class WikipediaClientConfig {

    @Bean
    public WebClient wikipediaWebClient(
            @Value("${wikipedia.api.base-url}") String baseUrl,
            @Value("${wikipedia.api.user-agent}") String userAgent) {

        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.USER_AGENT, userAgent)
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                .build();
    }
}
