package com.portfolio.onthisday.api;

import com.portfolio.onthisday.api.dto.EventDetail;
import com.portfolio.onthisday.api.dto.RelatedResponse;
import com.portfolio.onthisday.service.DigestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Single-event endpoints: detail and related events.
 */
@RestController
@RequestMapping("/api/event")
@Tag(name = "Event", description = "Single event detail and related events")
public class EventController {

    private final DigestService digestService;

    public EventController(DigestService digestService) {
        this.digestService = digestService;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Single event detail")
    public EventDetail event(@PathVariable Long id) {
        return digestService.getEvent(id);
    }

    @GetMapping("/{id}/related")
    @Operation(summary = "Related events (same year, and shared theme/category)")
    public RelatedResponse related(@PathVariable Long id) {
        return digestService.getRelated(id);
    }
}
