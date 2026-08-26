package com.portfolio.onthisday.service;

import com.portfolio.onthisday.domain.Event;
import com.portfolio.onthisday.domain.FeedType;
import com.portfolio.onthisday.dto.OnThisDayEvent;
import com.portfolio.onthisday.dto.WikiPage;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

/**
 * Maps a Wikipedia {@link OnThisDayEvent} DTO onto a persistable {@link Event}.
 *
 * <p>Produces a "bare" entity — calendar position, text, and the denormalized primary
 * page — plus a stable {@code externalKey} for dedupe. Curation (score and tags) is applied
 * separately by the fetch pipeline.
 */
@Component
public class EventMapper {

    /**
     * Build an {@link Event} for the given feed type and calendar day. Returns {@code null}
     * when the DTO has no usable text (nothing worth storing).
     */
    public Event toEntity(OnThisDayEvent dto, FeedType feedType, int month, int day) {
        if (dto == null || dto.text() == null || dto.text().isBlank()) {
            return null;
        }

        Event event = new Event();
        event.setMonth(month);
        event.setDay(day);
        event.setYear(dto.year());
        event.setText(truncate(dto.text(), 2000));
        event.setFeedType(feedType);

        WikiPage page = primaryPage(dto);
        if (page != null) {
            event.setPageTitle(truncate(displayTitle(page), 512));
            event.setExtract(truncate(page.extract(), 4000));
            event.setThumbnailUrl(imageSource(page.thumbnail()));
            event.setImageUrl(imageSource(page.originalimage()));
            event.setWikipediaUrl(desktopUrl(page));
        }

        event.setExternalKey(computeExternalKey(feedType, month, day, dto.year(), dto.text()));
        return event;
    }

    /** The first linked page is typically the most relevant article for the entry. */
    private WikiPage primaryPage(OnThisDayEvent dto) {
        List<WikiPage> pages = dto.pages();
        return (pages == null || pages.isEmpty()) ? null : pages.get(0);
    }

    private String displayTitle(WikiPage page) {
        if (page.title() == null) {
            return null;
        }
        return page.title().replace('_', ' ');
    }

    private String imageSource(WikiPage.WikiImage image) {
        return image == null ? null : image.source();
    }

    private String desktopUrl(WikiPage page) {
        if (page.content_urls() == null || page.content_urls().desktop() == null) {
            return null;
        }
        return page.content_urls().desktop().page();
    }

    /**
     * A collision-resistant key over the source content so the same entry maps to the same
     * row across re-fetches. SHA-256 hex is 64 chars, matching the column width.
     */
    String computeExternalKey(FeedType feedType, int month, int day, Integer year, String text) {
        String raw = feedType.name() + "|" + month + "|" + day + "|" + year + "|" + text;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed present on every JVM; treat absence as fatal.
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
