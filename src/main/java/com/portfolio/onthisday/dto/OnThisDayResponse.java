package com.portfolio.onthisday.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Top-level response from the Wikipedia "On This Day" feed.
 *
 * <p>The {@code /onthisday/events/{mm}/{dd}} endpoint returns a single {@code events}
 * array. The sibling endpoints ({@code /births}, {@code /deaths}, {@code /holidays},
 * {@code /selected}) each return their own array, so all are declared here as nullable
 * fields; only the one matching the requested endpoint is populated.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OnThisDayResponse(
        List<OnThisDayEvent> events,
        List<OnThisDayEvent> births,
        List<OnThisDayEvent> deaths,
        List<OnThisDayEvent> holidays,
        List<OnThisDayEvent> selected
) {
}
