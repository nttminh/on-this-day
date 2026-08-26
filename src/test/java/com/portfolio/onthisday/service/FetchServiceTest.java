package com.portfolio.onthisday.service;

import com.portfolio.onthisday.client.WikipediaOnThisDayClient;
import com.portfolio.onthisday.domain.Event;
import com.portfolio.onthisday.domain.EventRepository;
import com.portfolio.onthisday.domain.FeedType;
import com.portfolio.onthisday.dto.OnThisDayEvent;
import com.portfolio.onthisday.dto.OnThisDayResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Exercises the fetch pipeline with a mocked Wikipedia client and repository, using the
 * real mapper/tagging/scoring. Verifies cross-feed dedupe and externalKey-based upsert.
 */
@ExtendWith(MockitoExtension.class)
class FetchServiceTest {

    @Mock
    private WikipediaOnThisDayClient client;
    @Mock
    private EventRepository repository;

    private FetchService fetchService;

    @BeforeEach
    void setUp() {
        fetchService = new FetchService(client, repository,
                new EventMapper(), new TaggingService(), new ScoringService());
    }

    @Test
    void dedupesTheSameEntryAcrossSelectedAndEventsFeeds() {
        OnThisDayEvent moon = new OnThisDayEvent(
                "Apollo 11 lands the first humans on the Moon.", 1969, List.of());

        // Same happening appears in both SELECTED and EVENTS.
        when(client.fetch(eq(FeedType.SELECTED), anyInt(), anyInt()))
                .thenReturn(Mono.just(response(FeedType.SELECTED, moon)));
        when(client.fetch(eq(FeedType.EVENTS), anyInt(), anyInt()))
                .thenReturn(Mono.just(response(FeedType.EVENTS, moon)));
        when(client.fetch(eq(FeedType.BIRTHS), anyInt(), anyInt()))
                .thenReturn(Mono.just(new OnThisDayResponse(null, null, null, null, null)));
        when(client.fetch(eq(FeedType.DEATHS), anyInt(), anyInt()))
                .thenReturn(Mono.just(new OnThisDayResponse(null, null, null, null, null)));
        when(client.fetch(eq(FeedType.HOLIDAYS), anyInt(), anyInt()))
                .thenReturn(Mono.just(new OnThisDayResponse(null, null, null, null, null)));

        when(repository.existsByExternalKey(anyString())).thenReturn(false);

        int stored = fetchService.fetchAndStore(7, 20);

        // Two candidates collapse to one; only one row saved.
        assertThat(stored).isEqualTo(1);
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(repository, times(1)).save(captor.capture());

        Event saved = captor.getValue();
        // The higher-scored SELECTED variant should win the dedupe.
        assertThat(saved.getFeedType()).isEqualTo(FeedType.SELECTED);
        assertThat(saved.getTags()).contains("space", "1960s", "firsts");
    }

    @Test
    void skipsEntriesAlreadyStored() {
        OnThisDayEvent moon = new OnThisDayEvent("Apollo 11 lands on the Moon.", 1969, List.of());
        when(client.fetch(eq(FeedType.SELECTED), anyInt(), anyInt()))
                .thenReturn(Mono.just(response(FeedType.SELECTED, moon)));
        when(client.fetch(eq(FeedType.EVENTS), anyInt(), anyInt()))
                .thenReturn(Mono.just(new OnThisDayResponse(null, null, null, null, null)));
        when(client.fetch(eq(FeedType.BIRTHS), anyInt(), anyInt()))
                .thenReturn(Mono.just(new OnThisDayResponse(null, null, null, null, null)));
        when(client.fetch(eq(FeedType.DEATHS), anyInt(), anyInt()))
                .thenReturn(Mono.just(new OnThisDayResponse(null, null, null, null, null)));
        when(client.fetch(eq(FeedType.HOLIDAYS), anyInt(), anyInt()))
                .thenReturn(Mono.just(new OnThisDayResponse(null, null, null, null, null)));

        when(repository.existsByExternalKey(anyString())).thenReturn(true);

        int stored = fetchService.fetchAndStore(7, 20);

        assertThat(stored).isZero();
        verify(repository, times(0)).save(org.mockito.ArgumentMatchers.any());
    }

    private OnThisDayResponse response(FeedType type, OnThisDayEvent... events) {
        List<OnThisDayEvent> list = List.of(events);
        return switch (type) {
            case EVENTS -> new OnThisDayResponse(list, null, null, null, null);
            case SELECTED -> new OnThisDayResponse(null, null, null, null, list);
            case BIRTHS -> new OnThisDayResponse(null, list, null, null, null);
            case DEATHS -> new OnThisDayResponse(null, null, list, null, null);
            case HOLIDAYS -> new OnThisDayResponse(null, null, null, list, null);
        };
    }
}
