package com.portfolio.onthisday.api.dto;

import java.util.List;

/**
 * Related events for the detail page, split into the two sections the UI renders:
 * "more from this year" and "related events" (by shared theme/tag).
 */
public record RelatedResponse(
        List<EventSummary> fromSameYear,
        List<EventSummary> byTheme
) {
}
