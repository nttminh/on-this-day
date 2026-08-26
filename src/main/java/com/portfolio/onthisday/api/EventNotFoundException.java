package com.portfolio.onthisday.api;

/** Thrown when an event id does not resolve to a stored event. */
public class EventNotFoundException extends RuntimeException {

    public EventNotFoundException(Long id) {
        super("No event found with id " + id);
    }
}
