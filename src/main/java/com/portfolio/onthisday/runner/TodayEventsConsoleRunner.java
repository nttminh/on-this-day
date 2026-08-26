package com.portfolio.onthisday.runner;

import com.portfolio.onthisday.client.WikipediaOnThisDayClient;
import com.portfolio.onthisday.dto.OnThisDayEvent;
import com.portfolio.onthisday.dto.OnThisDayResponse;
import com.portfolio.onthisday.dto.WikiPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Step 1 smoke test: on application startup, call the Wikipedia "On This Day" API for
 * today's date and print the events to the console.
 *
 * <p>This proves the WebClient call and JSON mapping work end-to-end before persistence,
 * scheduling, and the REST layer are built out. It is disabled under the {@code test}
 * profile so it does not fire network calls during the test suite.
 */
@Component
@Profile("!test")
public class TodayEventsConsoleRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TodayEventsConsoleRunner.class);

    private final WikipediaOnThisDayClient client;

    public TodayEventsConsoleRunner(WikipediaOnThisDayClient client) {
        this.client = client;
    }

    @Override
    public void run(ApplicationArguments args) {
        LocalDate today = LocalDate.now();
        log.info("=== On This Day: {} ===", today);

        try {
            OnThisDayResponse response = client.fetchEvents(today);
            List<OnThisDayEvent> events = response == null ? null : response.events();

            if (events == null || events.isEmpty()) {
                log.warn("No events returned for {}", today);
                return;
            }

            log.info("Fetched {} events for {}. Showing them below:", events.size(), today);
            events.forEach(this::printEvent);
        } catch (Exception e) {
            // Never let a failed network call crash startup during development.
            log.error("Could not fetch today's events from Wikipedia: {}", e.getMessage());
        }
    }

    private void printEvent(OnThisDayEvent event) {
        String year = event.year() == null ? "----" : String.valueOf(event.year());
        System.out.printf("%n[%s] %s%n", year, event.text());

        WikiPage page = primaryPage(event);
        if (page != null) {
            if (page.content_urls() != null && page.content_urls().desktop() != null) {
                System.out.printf("        read: %s%n", page.content_urls().desktop().page());
            }
            if (page.thumbnail() != null) {
                System.out.printf("        image: %s%n", page.thumbnail().source());
            }
        }
    }

    /** The first linked page is typically the most relevant article for the entry. */
    private WikiPage primaryPage(OnThisDayEvent event) {
        List<WikiPage> pages = event.pages();
        return (pages == null || pages.isEmpty()) ? null : pages.get(0);
    }
}
