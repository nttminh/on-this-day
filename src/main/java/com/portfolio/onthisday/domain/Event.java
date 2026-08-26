package com.portfolio.onthisday.domain;

import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * A historical "On This Day" entry, persisted so it isn't re-fetched from Wikipedia
 * on every request.
 *
 * <p>Events recur annually, so the calendar position is stored as {@code month}/{@code day}
 * (1-based) rather than a full date; {@code year} is the year the event happened and may be
 * {@code null} (e.g. holidays). The most relevant Wikipedia article is denormalized onto the
 * row (title/extract/image/url) to keep read queries single-table and fast.
 */
@Entity
@Table(
        name = "events",
        indexes = {
                @Index(name = "idx_events_month_day", columnList = "cal_month,cal_day"),
                @Index(name = "idx_events_year", columnList = "cal_year"),
                @Index(name = "idx_events_external_key", columnList = "externalKey", unique = true)
        }
)
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Stable hash of the source content (feed type + month/day/year + text). Used to
     * dedupe so re-fetching the same date does not create duplicate rows.
     */
    @Column(nullable = false, unique = true, length = 64)
    private String externalKey;

    @Column(name = "cal_month", nullable = false)
    private int month;

    @Column(name = "cal_day", nullable = false)
    private int day;

    /** Year the event occurred; may be negative (BCE) or null (e.g. holidays). */
    @Column(name = "cal_year")
    private Integer year;

    @Column(nullable = false, length = 2000)
    private String text;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private FeedType feedType;

    // --- Denormalized primary Wikipedia page ---

    @Column(length = 512)
    private String pageTitle;

    @Column(length = 4000)
    private String extract;

    @Column(length = 1024)
    private String thumbnailUrl;

    @Column(length = 1024)
    private String imageUrl;

    @Column(length = 1024)
    private String wikipediaUrl;

    // --- Curation ---

    /** Curation score; higher ranks first in digests. */
    @Column(nullable = false)
    private double score;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "event_tags",
            joinColumns = @JoinColumn(name = "event_id"),
            indexes = @Index(name = "idx_event_tags_tag", columnList = "tag")
    )
    @Column(name = "tag", length = 64)
    private Set<String> tags = new HashSet<>();

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public Event() {
        // Required by JPA and used by the mapper.
    }

    // --- Getters / setters ---

    public Long getId() {
        return id;
    }

    public String getExternalKey() {
        return externalKey;
    }

    public void setExternalKey(String externalKey) {
        this.externalKey = externalKey;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public FeedType getFeedType() {
        return feedType;
    }

    public void setFeedType(FeedType feedType) {
        this.feedType = feedType;
    }

    public String getPageTitle() {
        return pageTitle;
    }

    public void setPageTitle(String pageTitle) {
        this.pageTitle = pageTitle;
    }

    public String getExtract() {
        return extract;
    }

    public void setExtract(String extract) {
        this.extract = extract;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getWikipediaUrl() {
        return wikipediaUrl;
    }

    public void setWikipediaUrl(String wikipediaUrl) {
        this.wikipediaUrl = wikipediaUrl;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    /** True when this entry has an image, used by curation and the UI. */
    public boolean hasImage() {
        return thumbnailUrl != null && !thumbnailUrl.isBlank();
    }
}
