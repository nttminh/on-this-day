package com.portfolio.onthisday.service;

import com.portfolio.onthisday.client.WikipediaOnThisDayClient;
import com.portfolio.onthisday.domain.Event;
import com.portfolio.onthisday.domain.EventRepository;
import com.portfolio.onthisday.domain.FeedType;
import com.portfolio.onthisday.dto.OnThisDayEvent;
import com.portfolio.onthisday.dto.OnThisDayResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Fetches "On This Day" data from Wikipedia, curates it, and persists it.
 *
 * <p>For a calendar day it pulls every feed type, maps each entry to an {@link Event},
 * applies tagging and scoring, removes near-duplicates within the batch, then upserts by
 * {@code externalKey} so repeated fetches don't create duplicate rows. Failures for one
 * feed are logged and skipped so a single bad response doesn't lose the rest.
 */
@Service
public class FetchService {

    private static final Logger log = LoggerFactory.getLogger(FetchService.class);

    /** Feeds pulled for each day. SELECTED overlaps EVENTS; batch dedupe reconciles them. */
    private static final List<FeedType> FEEDS = List.of(
            FeedType.SELECTED, FeedType.EVENTS, FeedType.BIRTHS, FeedType.DEATHS, FeedType.HOLIDAYS);

    private final WikipediaOnThisDayClient client;
    private final EventRepository repository;
    private final EventMapper mapper;
    private final TaggingService taggingService;
    private final ScoringService scoringService;

    public FetchService(WikipediaOnThisDayClient client,
                        EventRepository repository,
                        EventMapper mapper,
                        TaggingService taggingService,
                        ScoringService scoringService) {
        this.client = client;
        this.repository = repository;
        this.mapper = mapper;
        this.taggingService = taggingService;
        this.scoringService = scoringService;
    }

    /**
     * Ensure a day's data is present, fetching only if the day is empty. Returns the number
     * of newly stored events (0 if the day was already populated). Used for fetch-on-miss.
     */
    @Transactional
    public int ensureDay(int month, int day) {
        if (repository.countByMonthAndDay(month, day) > 0) {
            return 0;
        }
        return fetchAndStore(month, day);
    }

    /**
     * Fetch every feed for the given day, curate, and persist new entries.
     *
     * @return the number of newly stored events
     */
    @Transactional
    public int fetchAndStore(int month, int day) {
        log.info("Fetching all feeds for {}/{}", month, day);

        List<Event> candidates = new ArrayList<>();
        for (FeedType feed : FEEDS) {
            try {
                OnThisDayResponse response = client.fetch(feed, month, day).block();
                List<OnThisDayEvent> entries = entriesFor(response, feed);
                for (OnThisDayEvent dto : entries) {
                    Event event = mapper.toEntity(dto, feed, month, day);
                    if (event == null) {
                        continue;
                    }
                    event.setTags(taggingService.tagsFor(event));
                    event.setScore(scoringService.score(event));
                    candidates.add(event);
                }
            } catch (Exception e) {
                log.warn("Skipping feed '{}' for {}/{}: {}", feed.path(), month, day, e.toString());
            }
        }

        List<Event> deduped = dedupe(candidates);
        int stored = persistNew(deduped);
        log.info("Stored {} new events for {}/{} ({} candidates, {} after dedupe)",
                stored, month, day, candidates.size(), deduped.size());
        return stored;
    }

    /** Pick the list matching the feed type out of the multi-field response. */
    private List<OnThisDayEvent> entriesFor(OnThisDayResponse response, FeedType feed) {
        if (response == null) {
            return List.of();
        }
        List<OnThisDayEvent> list = switch (feed) {
            case EVENTS -> response.events();
            case SELECTED -> response.selected();
            case BIRTHS -> response.births();
            case DEATHS -> response.deaths();
            case HOLIDAYS -> response.holidays();
        };
        return list == null ? List.of() : list;
    }

    /**
     * Collapse near-duplicate entries (the same happening appearing in both the SELECTED and
     * EVENTS feeds) by a normalized text key, keeping the highest-scored variant.
     */
    private List<Event> dedupe(List<Event> candidates) {
        Map<String, Event> bestBySimilarity = new LinkedHashMap<>();
        for (Event event : candidates) {
            String key = similarityKey(event);
            Event existing = bestBySimilarity.get(key);
            if (existing == null || event.getScore() > existing.getScore()) {
                bestBySimilarity.put(key, event);
            }
        }
        return new ArrayList<>(bestBySimilarity.values());
    }

    private String similarityKey(Event event) {
        String text = event.getText() == null ? "" : event.getText();
        String normalized = text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        if (normalized.length() > 120) {
            normalized = normalized.substring(0, 120);
        }
        // Keep births/deaths/holidays distinct from events even if text is similar.
        String scope = switch (event.getFeedType()) {
            case BIRTHS -> "b:";
            case DEATHS -> "d:";
            case HOLIDAYS -> "h:";
            default -> "e:";
        };
        return scope + event.getYear() + ":" + normalized;
    }

    /** Save only entries whose externalKey isn't already stored. */
    private int persistNew(List<Event> events) {
        int stored = 0;
        for (Event event : events) {
            if (repository.existsByExternalKey(event.getExternalKey())) {
                continue;
            }
            repository.save(event);
            stored++;
        }
        return stored;
    }
}
