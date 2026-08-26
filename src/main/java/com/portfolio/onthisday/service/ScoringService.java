package com.portfolio.onthisday.service;

import com.portfolio.onthisday.domain.Event;
import org.springframework.stereotype.Service;

/**
 * Assigns a curation score to an {@link Event}. Higher scores surface first in digests,
 * so the "top picks" for a day are simply the highest-scored rows.
 *
 * <p>The heuristics favour entries that make good visual tiles: those with an image, a
 * readable summary, and provenance from Wikipedia's own curated "selected" feed, with a
 * gentle nudge for round-number ("notable") years.
 */
@Service
public class ScoringService {

    /**
     * Compute a score. Expects the event's page fields, feed type, and year to be set.
     */
    public double score(Event event) {
        double score = 1.0;

        // A tile with a picture is far more engaging in the grid.
        if (event.hasImage()) {
            score += 3.0;
        }
        if (event.getImageUrl() != null && !event.getImageUrl().isBlank()) {
            score += 0.5; // full-resolution original available for the detail hero
        }

        // A usable summary gives the detail page something to show.
        if (event.getExtract() != null && !event.getExtract().isBlank()) {
            score += 1.5;
        }

        // Prefer text that reads as a crisp headline, not a fragment or a wall of prose.
        if (event.getText() != null) {
            int len = event.getText().length();
            if (len >= 40 && len <= 300) {
                score += 0.5;
            }
        }

        // Provenance: Wikipedia's "selected" feed is its own editorial top picks.
        switch (event.getFeedType()) {
            case SELECTED -> score += 2.0;
            case EVENTS -> score += 1.0;
            case BIRTHS, DEATHS -> score += 0.5;
            case HOLIDAYS -> score += 0.75;
        }

        // "Notable" years: centuries and quarter-centuries feel like milestones.
        Integer year = event.getYear();
        if (year != null) {
            if (year % 100 == 0) {
                score += 0.5;
            } else if (year % 25 == 0) {
                score += 0.25;
            }
        }

        return score;
    }
}
