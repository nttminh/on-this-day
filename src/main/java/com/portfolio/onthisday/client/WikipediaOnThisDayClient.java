package com.portfolio.onthisday.client;

import com.portfolio.onthisday.domain.FeedType;
import com.portfolio.onthisday.dto.OnThisDayResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.LocalDate;

/**
 * Thin client over the Wikipedia "On This Day" feed.
 *
 * <p>Wraps the reactive {@link WebClient} and exposes intent-revealing methods for the
 * different feed types. The base URL and headers are supplied by
 * {@link com.portfolio.onthisday.config.WikipediaClientConfig}, so this class only knows
 * about the path shape: {@code /feed/v1/wikipedia/en/onthisday/{type}/{mm}/{dd}}.
 */
@Component
public class WikipediaOnThisDayClient {

    private static final Logger log = LoggerFactory.getLogger(WikipediaOnThisDayClient.class);

    private final WebClient wikipediaWebClient;

    /**
     * URI template for the feed, with {@code {type}}, {@code {month}}, {@code {day}}
     * placeholders. Configurable so the app can target either Wikipedia feed endpoint
     * (see {@code wikipedia.api.path-template} in {@code application.yml}).
     */
    private final String pathTemplate;

    public WikipediaOnThisDayClient(
            WebClient wikipediaWebClient,
            @Value("${wikipedia.api.path-template}") String pathTemplate) {
        this.wikipediaWebClient = wikipediaWebClient;
        this.pathTemplate = pathTemplate;
    }

    /** Fetch the "events" feed for the month/day of the given date. */
    public OnThisDayResponse fetchEvents(LocalDate date) {
        return fetch(FeedType.EVENTS, date.getMonthValue(), date.getDayOfMonth()).block();
    }

    /**
     * Fetch a feed for a given month/day.
     *
     * @param type  which feed to request (events, births, ...)
     * @param month month of year, 1-12
     * @param day   day of month, 1-31
     * @return a {@link Mono} emitting the parsed response
     */
    public Mono<OnThisDayResponse> fetch(FeedType type, int month, int day) {
        log.info("Fetching Wikipedia '{}' feed for {}/{}", type.path(), month, day);
        return wikipediaWebClient.get()
                .uri(pathTemplate, type.path(), month, day)
                .retrieve()
                .bodyToMono(OnThisDayResponse.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .filter(this::isRetryable))
                .doOnError(e -> log.error("Failed to fetch '{}' feed for {}/{}: {}",
                        type.path(), month, day, e.toString()));
    }

    /** Retry on transient network errors and 5xx responses, but not on 4xx. */
    private boolean isRetryable(Throwable throwable) {
        if (throwable instanceof WebClientResponseException wcre) {
            return wcre.getStatusCode().is5xxServerError();
        }
        return true;
    }
}
