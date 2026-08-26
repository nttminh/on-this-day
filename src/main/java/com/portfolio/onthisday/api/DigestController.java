package com.portfolio.onthisday.api;

import com.portfolio.onthisday.api.dto.EventSummary;
import com.portfolio.onthisday.api.dto.PageResponse;
import com.portfolio.onthisday.service.DigestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Digest endpoints: curated picks for today and paginated browsing of any calendar day.
 */
@RestController
@RequestMapping("/api/digest")
@Validated
@Tag(name = "Digest", description = "Curated and browsable 'On This Day' events")
public class DigestController {

    /** Upper bound on page size so a client can't request unbounded pages. */
    private static final int MAX_PAGE_SIZE = 60;

    private final DigestService digestService;

    public DigestController(DigestService digestService) {
        this.digestService = digestService;
    }

    @GetMapping("/today")
    @Operation(summary = "Curated top picks for today")
    public List<EventSummary> today() {
        return digestService.today();
    }

    @GetMapping("/{month}/{day}")
    @Operation(summary = "Browse any date, paginated for infinite scroll")
    public PageResponse<EventSummary> browse(
            @PathVariable @Min(1) @Max(12) int month,
            @PathVariable @Min(1) @Max(31) int day,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "24") @Min(1) @Max(MAX_PAGE_SIZE) int size) {
        return digestService.browse(month, day, tag, page, size);
    }

    @GetMapping("/{month}/{day}/tags")
    @Operation(summary = "Distinct tags available for a date (for the chip row)")
    public List<String> tags(
            @PathVariable @Min(1) @Max(12) int month,
            @PathVariable @Min(1) @Max(31) int day) {
        return digestService.tagsForDay(month, day);
    }
}
