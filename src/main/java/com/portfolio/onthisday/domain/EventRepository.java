package com.portfolio.onthisday.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Data access for {@link Event}. Read queries drive the digest and detail pages;
 * {@link #existsByExternalKey} backs dedupe during fetching.
 */
public interface EventRepository extends JpaRepository<Event, Long> {

    boolean existsByExternalKey(String externalKey);

    Optional<Event> findByExternalKey(String externalKey);

    /** All events for a calendar day, best-scored first — the browse/infinite-scroll query. */
    Page<Event> findByMonthAndDayOrderByScoreDesc(int month, int day, Pageable pageable);

    long countByMonthAndDay(int month, int day);

    /** Top-scored events for a calendar day, optionally filtered to one tag. */
    @Query("""
            SELECT e FROM Event e
            WHERE e.month = :month AND e.day = :day
            ORDER BY e.score DESC
            """)
    List<Event> findTopForDay(@Param("month") int month, @Param("day") int day, Pageable pageable);

    /** Browse a day filtered to a single tag (tag chips on the home page). */
    @Query("""
            SELECT e FROM Event e JOIN e.tags t
            WHERE e.month = :month AND e.day = :day AND t = :tag
            ORDER BY e.score DESC
            """)
    Page<Event> findByMonthAndDayAndTag(@Param("month") int month,
                                        @Param("day") int day,
                                        @Param("tag") String tag,
                                        Pageable pageable);

    /** "More from this year": same year, excluding the anchor event. */
    @Query("""
            SELECT e FROM Event e
            WHERE e.year = :year AND e.id <> :excludeId
            ORDER BY e.score DESC
            """)
    List<Event> findRelatedByYear(@Param("year") Integer year,
                                  @Param("excludeId") Long excludeId,
                                  Pageable pageable);

    /** "Related events": share at least one tag with the anchor, excluding it. */
    @Query("""
            SELECT DISTINCT e FROM Event e JOIN e.tags t
            WHERE t IN :tags AND e.id <> :excludeId
            ORDER BY e.score DESC
            """)
    List<Event> findRelatedByTags(@Param("tags") Collection<String> tags,
                                  @Param("excludeId") Long excludeId,
                                  Pageable pageable);

    /** Distinct tags present for a calendar day, to render the chip row. */
    @Query("""
            SELECT DISTINCT t FROM Event e JOIN e.tags t
            WHERE e.month = :month AND e.day = :day
            ORDER BY t ASC
            """)
    List<String> findDistinctTagsForDay(@Param("month") int month, @Param("day") int day);
}
