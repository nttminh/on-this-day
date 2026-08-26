package com.portfolio.onthisday.domain;

/**
 * The kinds of "On This Day" feeds Wikipedia exposes for a given month/day.
 *
 * <p>Each constant carries the URL path segment used by the Wikipedia REST API.
 */
public enum FeedType {

    EVENTS("events"),
    SELECTED("selected"),
    BIRTHS("births"),
    DEATHS("deaths"),
    HOLIDAYS("holidays");

    private final String path;

    FeedType(String path) {
        this.path = path;
    }

    /** The URL path segment for this feed (e.g. {@code "events"}). */
    public String path() {
        return path;
    }
}
