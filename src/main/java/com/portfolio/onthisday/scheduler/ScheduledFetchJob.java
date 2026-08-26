package com.portfolio.onthisday.scheduler;

import com.portfolio.onthisday.service.FetchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Keeps the store fresh by fetching today's events from Wikipedia on a daily schedule,
 * and (optionally) once at startup so a freshly launched instance has data to show.
 *
 * <p>Disabled under the {@code test} profile so the suite never makes live calls.
 */
@Component
@Profile("!test")
public class ScheduledFetchJob implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ScheduledFetchJob.class);

    private final FetchService fetchService;
    private final boolean fetchOnStartup;

    public ScheduledFetchJob(FetchService fetchService,
                             @Value("${onthisday.fetch.on-startup:true}") boolean fetchOnStartup) {
        this.fetchService = fetchService;
        this.fetchOnStartup = fetchOnStartup;
    }

    /** Daily refresh; cron is configurable via {@code onthisday.fetch.cron}. */
    @Scheduled(cron = "${onthisday.fetch.cron}")
    public void fetchToday() {
        LocalDate today = LocalDate.now();
        log.info("Scheduled fetch for {}", today);
        safeFetch(today);
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!fetchOnStartup) {
            return;
        }
        LocalDate today = LocalDate.now();
        log.info("Startup fetch check for {}", today);
        try {
            int stored = fetchService.ensureDay(today.getMonthValue(), today.getDayOfMonth());
            log.info("Startup fetch stored {} new events for {}", stored, today);
        } catch (Exception e) {
            // Startup must succeed even if Wikipedia is unreachable (e.g. offline/dev).
            log.warn("Startup fetch failed (continuing without today's data): {}", e.toString());
        }
    }

    private void safeFetch(LocalDate date) {
        try {
            fetchService.fetchAndStore(date.getMonthValue(), date.getDayOfMonth());
        } catch (Exception e) {
            log.error("Scheduled fetch for {} failed: {}", date, e.toString());
        }
    }
}
