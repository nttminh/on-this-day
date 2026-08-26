package com.portfolio.onthisday.service;

import com.portfolio.onthisday.domain.Event;
import com.portfolio.onthisday.domain.FeedType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScoringServiceTest {

    private final ScoringService scoring = new ScoringService();

    @Test
    void imageAndExtractRaiseScore() {
        Event withImage = base();
        withImage.setThumbnailUrl("https://img");
        withImage.setExtract("A good summary.");

        Event bare = base();

        assertThat(scoring.score(withImage)).isGreaterThan(scoring.score(bare));
    }

    @Test
    void selectedFeedOutranksPlainEvents() {
        Event selected = base();
        selected.setFeedType(FeedType.SELECTED);

        Event events = base();
        events.setFeedType(FeedType.EVENTS);

        assertThat(scoring.score(selected)).isGreaterThan(scoring.score(events));
    }

    @Test
    void notableCenturyYearGetsBonus() {
        Event century = base();
        century.setYear(1900);

        Event ordinary = base();
        ordinary.setYear(1903);

        assertThat(scoring.score(century)).isGreaterThan(scoring.score(ordinary));
    }

    private Event base() {
        Event e = new Event();
        e.setFeedType(FeedType.EVENTS);
        e.setText("A concise, headline-length description of a historical event here.");
        e.setYear(1903);
        return e;
    }
}
