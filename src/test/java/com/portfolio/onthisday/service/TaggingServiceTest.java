package com.portfolio.onthisday.service;

import com.portfolio.onthisday.domain.Event;
import com.portfolio.onthisday.domain.FeedType;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TaggingServiceTest {

    private final TaggingService tagging = new TaggingService();

    @Test
    void tagsScienceSpaceAndDecadeForMoonLanding() {
        Set<String> tags = tagging.tagsFor(event(1969, FeedType.EVENTS,
                "Apollo 11 astronauts land on the Moon.",
                "The first crewed Moon landing."));
        assertThat(tags).contains("space", "1960s", "firsts");
    }

    @Test
    void warAndPoliticsFromBattleAndTreaty() {
        Set<String> tags = tagging.tagsFor(event(1071, FeedType.EVENTS,
                "The Seljuk Turks win the Battle of Manzikert; a treaty follows.", null));
        assertThat(tags).contains("war", "politics", "1070s");
    }

    @Test
    void revolutionDoesNotFalselyMatchScience() {
        // "revolution" must not match the "evolution" science stem (substring pitfall).
        Set<String> tags = tagging.tagsFor(event(1789, FeedType.EVENTS,
                "During the revolution the assembly adopts a new constitution.", null));
        assertThat(tags).doesNotContain("science");
        assertThat(tags).contains("politics");
    }

    @Test
    void feedTypeTagsApplied() {
        assertThat(tagging.tagsFor(event(1743, FeedType.BIRTHS, "A chemist is born.", null)))
                .contains("births", "science");
        assertThat(tagging.tagsFor(event(1980, FeedType.DEATHS, "An animator dies.", null)))
                .contains("deaths");
        assertThat(tagging.tagsFor(event(null, FeedType.HOLIDAYS, "A public holiday.", null)))
                .contains("holidays");
    }

    private Event event(Integer year, FeedType feedType, String text, String extract) {
        Event e = new Event();
        e.setYear(year);
        e.setFeedType(feedType);
        e.setText(text);
        e.setExtract(extract);
        return e;
    }
}
