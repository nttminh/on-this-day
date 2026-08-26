package com.portfolio.onthisday.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * A single "On This Day" entry as returned by Wikipedia.
 *
 * @param text  a short human-readable description of the event
 * @param year  the year the event occurred (may be negative for BCE); null for holidays
 * @param pages the Wikipedia articles linked to this entry (first is usually the most relevant)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OnThisDayEvent(
        String text,
        Integer year,
        List<WikiPage> pages
) {
}
