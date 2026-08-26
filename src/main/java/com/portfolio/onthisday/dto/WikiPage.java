package com.portfolio.onthisday.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A Wikipedia article summary attached to an "On This Day" entry.
 *
 * <p>Carries the pieces the UI cares about: a title, a short extract, an optional
 * thumbnail image, and the canonical URL to read the full article on Wikipedia.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WikiPage(
        String title,
        String extract,
        WikiImage thumbnail,
        WikiImage originalimage,
        ContentUrls content_urls
) {

    /** An image reference (thumbnail or full-size original). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record WikiImage(String source, Integer width, Integer height) {
    }

    /** Links to the article on the desktop and mobile Wikipedia sites. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ContentUrls(UrlSet desktop, UrlSet mobile) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record UrlSet(String page) {
        }
    }
}
