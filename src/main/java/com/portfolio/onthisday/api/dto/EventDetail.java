package com.portfolio.onthisday.api.dto;

import com.portfolio.onthisday.domain.Event;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Full view of an event for the detail page (adds the summary/extract, the full-resolution
 * image, and the calendar position).
 */
public record EventDetail(
        Long id,
        int month,
        int day,
        Integer year,
        String title,
        String text,
        String extract,
        String thumbnailUrl,
        String imageUrl,
        String wikipediaUrl,
        String feedType,
        double score,
        List<String> tags
) {

    public static EventDetail from(Event e) {
        Set<String> sorted = new TreeSet<>(e.getTags());
        return new EventDetail(
                e.getId(),
                e.getMonth(),
                e.getDay(),
                e.getYear(),
                e.getPageTitle(),
                e.getText(),
                e.getExtract(),
                e.getThumbnailUrl(),
                e.getImageUrl(),
                e.getWikipediaUrl(),
                e.getFeedType().name(),
                e.getScore(),
                List.copyOf(sorted)
        );
    }
}
