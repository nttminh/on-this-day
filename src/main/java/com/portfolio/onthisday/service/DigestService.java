package com.portfolio.onthisday.service;

import com.portfolio.onthisday.api.EventNotFoundException;
import com.portfolio.onthisday.api.dto.EventDetail;
import com.portfolio.onthisday.api.dto.EventSummary;
import com.portfolio.onthisday.api.dto.PageResponse;
import com.portfolio.onthisday.api.dto.RelatedResponse;
import com.portfolio.onthisday.domain.Event;
import com.portfolio.onthisday.domain.EventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Read-side application service backing the REST API. Reads come from the local store;
 * a browse for a day that isn't stored yet triggers a one-off fetch (fetch-on-miss), so
 * the very first visit to a rare date still returns real data.
 *
 * <p>Results are cached (see cache names in {@code application.yml}); empty results are not
 * cached so a fetch that failed while offline can succeed on a later request.
 */
@Service
public class DigestService {

    private static final Logger log = LoggerFactory.getLogger(DigestService.class);

    private final EventRepository repository;
    private final FetchService fetchService;
    private final int topSize;

    public DigestService(EventRepository repository,
                         FetchService fetchService,
                         @Value("${onthisday.digest.top-size:30}") int topSize) {
        this.repository = repository;
        this.fetchService = fetchService;
        this.topSize = topSize;
    }

    /** Curated top picks for today. */
    @Cacheable(cacheNames = "digestToday", key = "T(java.time.LocalDate).now().toString()",
            unless = "#result.isEmpty()")
    public List<EventSummary> today() {
        LocalDate today = LocalDate.now();
        return topPicks(today.getMonthValue(), today.getDayOfMonth());
    }

    /** Curated top picks for an arbitrary day (used by {@link #today()} and the UI). */
    public List<EventSummary> topPicks(int month, int day) {
        ensureData(month, day);
        return repository.findTopForDay(month, day, PageRequest.of(0, topSize))
                .stream()
                .map(EventSummary::from)
                .toList();
    }

    /** Paginated browse of a day, optionally filtered to a single tag (infinite scroll). */
    @Cacheable(cacheNames = "digestByDay",
            key = "#month + '-' + #day + '-' + (#tag == null ? '' : #tag) + '-' + #page + '-' + #size",
            unless = "#result.content.isEmpty()")
    public PageResponse<EventSummary> browse(int month, int day, String tag, int page, int size) {
        ensureData(month, day);
        Pageable pageable = PageRequest.of(page, size);
        Page<Event> events = (tag == null || tag.isBlank())
                ? repository.findByMonthAndDayOrderByScoreDesc(month, day, pageable)
                : repository.findByMonthAndDayAndTag(month, day, tag, pageable);
        return PageResponse.of(events, EventSummary::from);
    }

    /** Distinct tags present for a day, for the chip row. */
    @Cacheable(cacheNames = "dayTags", key = "#month + '-' + #day", unless = "#result.isEmpty()")
    public List<String> tagsForDay(int month, int day) {
        ensureData(month, day);
        return repository.findDistinctTagsForDay(month, day);
    }

    @Cacheable(cacheNames = "eventById", key = "#id")
    public EventDetail getEvent(Long id) {
        return EventDetail.from(loadEvent(id));
    }

    @Cacheable(cacheNames = "relatedEvents", key = "#id")
    public RelatedResponse getRelated(Long id) {
        Event anchor = loadEvent(id);
        Pageable limit = PageRequest.of(0, 12);

        List<EventSummary> fromSameYear = anchor.getYear() == null
                ? List.of()
                : repository.findRelatedByYear(anchor.getYear(), anchor.getId(), limit)
                        .stream().map(EventSummary::from).toList();

        Set<String> tags = anchor.getTags();
        List<EventSummary> byTheme = tags.isEmpty()
                ? List.of()
                : repository.findRelatedByTags(tags, anchor.getId(), limit)
                        .stream().map(EventSummary::from).toList();

        return new RelatedResponse(fromSameYear, byTheme);
    }

    private Event loadEvent(Long id) {
        return repository.findById(id).orElseThrow(() -> new EventNotFoundException(id));
    }

    /** Fetch-on-miss: populate a day the first time anyone asks for it. */
    private void ensureData(int month, int day) {
        try {
            int stored = fetchService.ensureDay(month, day);
            if (stored > 0) {
                log.info("Fetch-on-miss stored {} events for {}/{}", stored, month, day);
            }
        } catch (Exception e) {
            // Serve whatever is already stored rather than failing the request.
            log.warn("Fetch-on-miss for {}/{} failed: {}", month, day, e.toString());
        }
    }
}
