package com.portfolio.onthisday.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.onthisday.domain.Event;
import com.portfolio.onthisday.domain.EventRepository;
import com.portfolio.onthisday.domain.FeedType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

/**
 * Loads a small bundled fixture of <em>real</em> historical events so the UI has content to
 * show when the live Wikipedia API is unreachable (offline dev, restricted networks, demos).
 *
 * <p>Enabled only when {@code onthisday.seed.enabled=true}. The seed is run through the same
 * tagging and scoring pipeline as live data and deduped by {@code externalKey}, so enabling
 * it alongside a working API simply tops up a couple of dates without creating duplicates.
 */
@Component
@ConditionalOnProperty(name = "onthisday.seed.enabled", havingValue = "true")
public class DevDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);
    private static final String SEED_LOCATION = "seed/events.json";

    private final EventRepository repository;
    private final EventMapper mapper;
    private final TaggingService taggingService;
    private final ScoringService scoringService;
    private final ObjectMapper objectMapper;

    public DevDataSeeder(EventRepository repository,
                         EventMapper mapper,
                         TaggingService taggingService,
                         ScoringService scoringService,
                         ObjectMapper objectMapper) {
        this.repository = repository;
        this.mapper = mapper;
        this.taggingService = taggingService;
        this.scoringService = scoringService;
        this.objectMapper = objectMapper;
    }

    /** One seed record; mirrors the fields the app stores per event. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record SeedEvent(int month, int day, Integer year, FeedType feedType, String text,
                     String title, String extract, String thumbnailUrl, String imageUrl,
                     String wikipediaUrl) {
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            List<SeedEvent> seeds = readSeed();
            int stored = 0;
            for (SeedEvent seed : seeds) {
                if (persist(seed)) {
                    stored++;
                }
            }
            log.info("Dev seed loaded: {} new events from {} records", stored, seeds.size());
        } catch (Exception e) {
            log.warn("Dev seed failed to load: {}", e.toString());
        }
    }

    private List<SeedEvent> readSeed() throws Exception {
        Resource resource = new ClassPathResource(SEED_LOCATION);
        try (InputStream in = resource.getInputStream()) {
            return objectMapper.readValue(in, objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, SeedEvent.class));
        }
    }

    private boolean persist(SeedEvent seed) {
        String externalKey = mapper.computeExternalKey(
                seed.feedType(), seed.month(), seed.day(), seed.year(), seed.text());
        if (repository.existsByExternalKey(externalKey)) {
            return false;
        }

        Event event = new Event();
        event.setExternalKey(externalKey);
        event.setMonth(seed.month());
        event.setDay(seed.day());
        event.setYear(seed.year());
        event.setText(seed.text());
        event.setFeedType(seed.feedType());
        event.setPageTitle(seed.title());
        event.setExtract(seed.extract());
        event.setThumbnailUrl(seed.thumbnailUrl());
        event.setImageUrl(seed.imageUrl());
        event.setWikipediaUrl(seed.wikipediaUrl());

        event.setTags(taggingService.tagsFor(event));
        event.setScore(scoringService.score(event));

        repository.save(event);
        return true;
    }
}
