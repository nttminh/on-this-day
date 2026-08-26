package com.portfolio.onthisday.api.dto;

import com.portfolio.onthisday.domain.Event;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Compact view of an event for grid tiles and related-event lists.
 */
public record EventSummary(
        Long id,
        Integer year,
        String title,
        String text,
        String thumbnailUrl,
        String wikipediaUrl,
        String feedType,
        double score,
        List<String> tags
) {

    public static EventSummary from(Event e) {
        Set<String> sorted = new TreeSet<>(e.getTags());
        return new EventSummary(
                e.getId(),
                e.getYear(),
                e.getPageTitle(),
                e.getText(),
                e.getThumbnailUrl(),
                e.getWikipediaUrl(),
                e.getFeedType().name(),
                e.getScore(),
                List.copyOf(sorted)
        );
    }
}
