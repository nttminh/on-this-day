package com.portfolio.onthisday.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the custom JPQL in {@link EventRepository} against a real (embedded) database:
 * paging, tag filtering, related-by-year, related-by-tags, and distinct tags.
 */
@DataJpaTest
class EventRepositoryTest {

    @Autowired
    private EventRepository repository;

    private Event moon;   // 1969, science/space
    private Event woodstock; // 1969, arts

    @BeforeEach
    void seed() {
        moon = save("apollo-11", 7, 20, 1969, "Apollo 11 lands on the Moon.",
                FeedType.EVENTS, 9.0, Set.of("science", "space", "1960s"));
        woodstock = save("woodstock", 7, 20, 1969, "Woodstock festival is announced.",
                FeedType.SELECTED, 7.0, Set.of("arts", "1960s"));
        save("treaty", 7, 20, 1969, "A minor treaty is signed.",
                FeedType.EVENTS, 3.0, Set.of("politics", "1960s"));
        // A different day, to prove day-scoping works.
        save("other-day", 1, 1, 2000, "New year celebration.",
                FeedType.HOLIDAYS, 5.0, Set.of("holidays", "2000s"));
    }

    @Test
    void existsByExternalKeyDetectsDuplicates() {
        assertThat(repository.existsByExternalKey("apollo-11")).isTrue();
        assertThat(repository.existsByExternalKey("nope")).isFalse();
    }

    @Test
    void browseByDayIsScoredDescAndDayScoped() {
        var page = repository.findByMonthAndDayOrderByScoreDesc(7, 20, PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent().get(0).getText()).contains("Apollo 11");
        assertThat(repository.countByMonthAndDay(7, 20)).isEqualTo(3);
        assertThat(repository.countByMonthAndDay(2, 2)).isZero();
    }

    @Test
    void browseByTagFiltersToThatTag() {
        var page = repository.findByMonthAndDayAndTag(7, 20, "arts", PageRequest.of(0, 10));
        assertThat(page.getContent()).extracting(Event::getExternalKey).containsExactly("woodstock");
    }

    @Test
    void relatedByYearExcludesAnchor() {
        List<Event> related = repository.findRelatedByYear(1969, moon.getId(), PageRequest.of(0, 10));
        assertThat(related).extracting(Event::getExternalKey)
                .contains("woodstock", "treaty")
                .doesNotContain("apollo-11");
    }

    @Test
    void relatedByTagsMatchesSharedThemes() {
        List<Event> related = repository.findRelatedByTags(moon.getTags(), moon.getId(), PageRequest.of(0, 10));
        // moon shares "1960s" with woodstock and treaty; anchor itself excluded.
        assertThat(related).extracting(Event::getExternalKey)
                .contains("woodstock", "treaty")
                .doesNotContain("apollo-11");
    }

    @Test
    void distinctTagsForDayAreSortedAndDeduped() {
        List<String> tags = repository.findDistinctTagsForDay(7, 20);
        assertThat(tags).containsExactly("1960s", "arts", "politics", "science", "space");
    }

    private Event save(String key, int month, int day, Integer year, String text,
                       FeedType feedType, double score, Set<String> tags) {
        Event e = new Event();
        e.setExternalKey(key);
        e.setMonth(month);
        e.setDay(day);
        e.setYear(year);
        e.setText(text);
        e.setFeedType(feedType);
        e.setScore(score);
        e.setTags(new java.util.HashSet<>(tags));
        return repository.save(e);
    }
}
